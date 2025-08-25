package com.telegram_notifier.service;

import com.telegram_notifier.config.AppProperties;
import com.telegram_notifier.model.TelegramMessage;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class TelegramService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TelegramService.class);

    private final AppProperties properties;
    private final ChannelTimestampService timestampService;
    private WebClient webClient;
    
    @Autowired
    public TelegramService(AppProperties properties, ChannelTimestampService timestampService) {
        this.properties = properties;
        this.timestampService = timestampService;
    }

    @PostConstruct
    public void initializeWebClient() {
        log.info("Initializing timestamp-based Telegram web scraping service...");
        
        this.webClient = WebClient.builder()
            .codecs(clientCodecConfigurer -> 
                clientCodecConfigurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // 2MB
            .build();
            
        log.info("Telegram web scraping service initialized successfully");
    }

    /**
     * Fetch only NEW messages (after last fetch time) from all channels
     */
    public List<TelegramMessage> fetchRecentMessages() {
        List<TelegramMessage> allNewMessages = new ArrayList<>();
        List<String> channels = getConfiguredChannels();
        
        log.info("Fetching NEW messages from {} channels using timestamp filtering", channels.size());
        
        LocalDateTime fetchStartTime = LocalDateTime.now();
        
        for (String channel : channels) {
            try {
                String channelName = channel.trim();
                if (channelName.startsWith("@")) {
                    channelName = channelName.substring(1);
                }
                
                List<TelegramMessage> channelMessages = fetchChannelMessages(channelName);
                
                // Filter to get only NEW messages (after last fetch time)
                List<TelegramMessage> newMessages = filterNewMessages(channelName, channelMessages);
                allNewMessages.addAll(newMessages);
                
                log.info("Channel @{}: Found {} total messages, {} are NEW", 
                        channelName, channelMessages.size(), newMessages.size());
                
                // Update last fetch time for this channel
                if (!newMessages.isEmpty()) {
                    // Find the latest timestamp among new messages
                    LocalDateTime latestTimestamp = newMessages.stream()
                        .map(TelegramMessage::getTimestamp)
                        .max(LocalDateTime::compareTo)
                        .orElse(fetchStartTime);
                    
                    timestampService.updateLastFetchTime(channelName, latestTimestamp);
                } else {
                    // No new messages, update to current fetch time
                    timestampService.updateLastFetchTime(channelName, fetchStartTime);
                }
                
                // Add delay to avoid rate limiting
                Thread.sleep(1000);
                
            } catch (Exception e) {
                log.error("Failed to fetch messages from channel: {}", channel, e);
            }
        }
        
        log.info("Total NEW messages fetched: {}", allNewMessages.size());
        return allNewMessages;
    }

    private List<TelegramMessage> fetchChannelMessages(String channelName) {
        List<TelegramMessage> messages = new ArrayList<>();
        
        try {
            String url = "https://t.me/s/" + channelName;
            log.debug("Scraping channel: {}", url);
            
            String html = webClient.get()
                    .uri(url)
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Connection", "keep-alive")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (html == null) {
                log.warn("No content received from channel: @{}", channelName);
                return messages;
            }
            
            // Check if channel exists and is accessible
            if (html.contains("tgme_page_description") && html.contains("channel doesn't exist")) {
                log.warn("Channel @{} doesn't exist or is not accessible", channelName);
                return messages;
            }
            
            messages = parseMessagesWithTimestamps(html, channelName);
            log.debug("Successfully parsed {} messages with timestamps from @{}", messages.size(), channelName);
            
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                log.warn("Channel @{} not found (404)", channelName);
            } else if (e.getStatusCode().value() == 403) {
                log.warn("Channel @{} access forbidden (403) - may be private", channelName);
            } else if (e.getStatusCode().value() == 429) {
                log.warn("Rate limited while accessing @{} (429) - will retry later", channelName);
            } else {
                log.error("HTTP error accessing channel @{}: {} {}", channelName, e.getStatusCode(), e.getMessage());
            }
        } catch (Exception e) {
            log.error("Unexpected error scraping channel @{}: {}", channelName, e.getMessage());
        }
        
        return messages;
    }

    private List<TelegramMessage> parseMessagesWithTimestamps(String html, String channelName) {
        List<TelegramMessage> messages = new ArrayList<>();
        
        try {
            // Pattern to match entire message blocks with timestamps
            Pattern messageBlockPattern = Pattern.compile(
                "<div class=\"tgme_widget_message[^\"]*\"[^>]*data-post=\"[^\"]+\"[^>]*>(.*?)</div>\\s*</div>", 
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
            );
            
            Matcher blockMatcher = messageBlockPattern.matcher(html);
            
            while (blockMatcher.find()) {
                String messageBlock = blockMatcher.group(1);
                
                // Extract message content
                String messageContent = extractMessageContent(messageBlock);
                if (messageContent == null || !isValidJobMessage(messageContent)) {
                    continue;
                }
                
                // Extract timestamp
                LocalDateTime messageTime = extractTimestamp(messageBlock);
                if (messageTime == null) {
                    // If no timestamp found, use current time minus a small offset
                    messageTime = LocalDateTime.now().minusMinutes(messages.size());
                }
                
                TelegramMessage message = new TelegramMessage(messageContent, messageTime, channelName);
                messages.add(message);
                
                // Limit to avoid memory issues
                if (messages.size() >= 20) {
                    break;
                }
            }
            
        } catch (Exception e) {
            log.error("Error parsing messages with timestamps from HTML", e);
        }
        
        return messages;
    }

    private String extractMessageContent(String messageBlock) {
        // Multiple patterns to catch different message formats
        List<Pattern> patterns = Arrays.asList(
            Pattern.compile(
                "<div class=\"tgme_widget_message_text[^\"]*\"[^>]*>(.*?)</div>", 
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                "<div class=\"tgme_widget_message_media_caption[^\"]*\"[^>]*>(.*?)</div>", 
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
            )
        );
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(messageBlock);
            if (matcher.find()) {
                String content = cleanHtmlContent(matcher.group(1));
                if (content != null && !content.trim().isEmpty()) {
                    return content;
                }
            }
        }
        
        return null;
    }

    private LocalDateTime extractTimestamp(String messageBlock) {
        try {
            // Look for timestamp in datetime attribute
            Pattern timestampPattern = Pattern.compile(
                "<time[^>]+datetime=\"([^\"]+)\"", 
                Pattern.CASE_INSENSITIVE
            );
            
            Matcher matcher = timestampPattern.matcher(messageBlock);
            if (matcher.find()) {
                String datetimeStr = matcher.group(1);
                
                // Parse ISO datetime format from Telegram
                // Example: "2024-08-25T04:15:51+00:00"
                return LocalDateTime.parse(datetimeStr.substring(0, 19), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            
            // Alternative: look for relative time and convert
            Pattern relativeTimePattern = Pattern.compile(
                "<span class=\"tgme_widget_message_date\"[^>]*title=\"([^\"]+)\"", 
                Pattern.CASE_INSENSITIVE
            );
            
            matcher = relativeTimePattern.matcher(messageBlock);
            if (matcher.find()) {
                String timeStr = matcher.group(1);
                return parseRelativeTime(timeStr);
            }
            
        } catch (DateTimeParseException e) {
            log.debug("Could not parse timestamp from message block", e);
        } catch (Exception e) {
            log.debug("Error extracting timestamp from message block", e);
        }
        
        return null;
    }

    private LocalDateTime parseRelativeTime(String timeStr) {
        try {
            // Handle formats like "Aug 25, 2024 at 09:45:51"
            if (timeStr.contains("at")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm:ss", Locale.ENGLISH);
                return LocalDateTime.parse(timeStr, formatter);
            }
            
            // Handle other formats as needed
            log.debug("Unhandled time format: {}", timeStr);
        } catch (DateTimeParseException e) {
            log.debug("Could not parse relative time: {}", timeStr, e);
        }
        
        return null;
    }

    private String cleanHtmlContent(String content) {
        if (content == null) return "";
        
        return content
                .replaceAll("<[^>]+>", "")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&mdash;", "—")
                .replaceAll("&ndash;", "–")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isValidJobMessage(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        if (content.length() < 50) {
            return false;
        }
        
        String lowerContent = content.toLowerCase();
        if (lowerContent.contains("subscribe to") || 
            lowerContent.contains("join our") ||
            lowerContent.contains("follow us") ||
            lowerContent.startsWith("👆") ||
            lowerContent.startsWith("⬆️")) {
            return false;
        }
        
        return lowerContent.contains("job") || 
               lowerContent.contains("hiring") || 
               lowerContent.contains("position") || 
               lowerContent.contains("vacancy") || 
               lowerContent.contains("developer") || 
               lowerContent.contains("engineer") || 
               lowerContent.contains("salary") ||
               lowerContent.contains("remote") ||
               lowerContent.contains("experience") ||
               lowerContent.contains("apply");
    }

    /**
     * Filter messages to only include those newer than last fetch time
     */
    private List<TelegramMessage> filterNewMessages(String channelName, List<TelegramMessage> messages) {
        List<TelegramMessage> newMessages = new ArrayList<>();
        
        for (TelegramMessage message : messages) {
            if (timestampService.isMessageNew(channelName, message.getTimestamp())) {
                newMessages.add(message);
            }
        }
        
        return newMessages;
    }

    private List<String> getConfiguredChannels() {
        String channelsConfig = properties.getTelegram().getChannels();
        if (channelsConfig == null || channelsConfig.trim().isEmpty()) {
            log.warn("No Telegram channels configured");
            return Collections.emptyList();
        }
        
        return Arrays.asList(channelsConfig.split(","));
    }

    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service_type", "Timestamp-based Web Scraping");
        status.put("channels_configured", getConfiguredChannels().size());
        status.put("channels", getConfiguredChannels());
        status.put("channel_last_fetch", timestampService.getChannelStatus());
        return status;
    }

    public void resetChannelTimestamps() {
        timestampService.resetAllChannels();
        log.info("Reset all channel timestamps");
    }
}
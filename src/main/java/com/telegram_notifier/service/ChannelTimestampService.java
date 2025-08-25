package com.telegram_notifier.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ChannelTimestampService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChannelTimestampService.class);
    
    // Store last fetch time per channel
    private final Map<String, LocalDateTime> lastFetchTimes = new HashMap<>();
    
    /**
     * Get the last fetch time for a channel. If never fetched, returns 24 hours ago.
     */
    public LocalDateTime getLastFetchTime(String channelName) {
        LocalDateTime lastFetch = lastFetchTimes.get(channelName);
        if (lastFetch == null) {
            // First time fetching - start from 24 hours ago to avoid processing old messages
            lastFetch = LocalDateTime.now().minusHours(24);
            log.info("First time fetching from @{}, starting from 24 hours ago: {}", 
                    channelName, lastFetch.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        return lastFetch;
    }
    
    /**
     * Update the last fetch time for a channel
     */
    public void updateLastFetchTime(String channelName, LocalDateTime fetchTime) {
        lastFetchTimes.put(channelName, fetchTime);
        log.debug("Updated last fetch time for @{}: {}", channelName, 
                fetchTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
    
    /**
     * Update last fetch time to current time
     */
    public void updateLastFetchTimeToNow(String channelName) {
        LocalDateTime now = LocalDateTime.now();
        updateLastFetchTime(channelName, now);
        log.info("Updated last fetch time for @{} to current time: {}", channelName,
                now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
    
    /**
     * Check if a message timestamp is newer than the last fetch time for the channel
     */
    public boolean isMessageNew(String channelName, LocalDateTime messageTimestamp) {
        LocalDateTime lastFetch = getLastFetchTime(channelName);
        boolean isNew = messageTimestamp.isAfter(lastFetch);
        
        if (isNew) {
            log.debug("Message from @{} at {} is NEW (after last fetch {})", 
                    channelName, 
                    messageTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    lastFetch.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        } else {
            log.debug("Message from @{} at {} is OLD (before/at last fetch {})", 
                    channelName, 
                    messageTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    lastFetch.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        
        return isNew;
    }
    
    /**
     * Get status information about all channels
     */
    public Map<String, String> getChannelStatus() {
        Map<String, String> status = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (Map.Entry<String, LocalDateTime> entry : lastFetchTimes.entrySet()) {
            status.put("@" + entry.getKey(), entry.getValue().format(formatter));
        }
        
        return status;
    }
    
    /**
     * Reset all channel timestamps (useful for testing)
     */
    public void resetAllChannels() {
        lastFetchTimes.clear();
        log.info("Reset all channel timestamps");
    }
}

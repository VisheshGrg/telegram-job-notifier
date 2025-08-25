package com.telegram_notifier.model;

import java.time.LocalDateTime;

public class TelegramMessage {
    private final String content;
    private final LocalDateTime timestamp;
    private final String channelName;
    
    public TelegramMessage(String content, LocalDateTime timestamp, String channelName) {
        this.content = content;
        this.timestamp = timestamp;
        this.channelName = channelName;
    }
    
    public String getContent() {
        return content;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getChannelName() {
        return channelName;
    }
    
    @Override
    public String toString() {
        return String.format("TelegramMessage{channel='%s', time=%s, content='%s'}", 
                           channelName, timestamp, content.substring(0, Math.min(content.length(), 50)) + "...");
    }
}

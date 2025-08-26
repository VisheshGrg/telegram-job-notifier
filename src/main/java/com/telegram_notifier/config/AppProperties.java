package com.telegram_notifier.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Telegram telegram = new Telegram();
    private AI ai = new AI();
    private Notion notion = new Notion();
    private Storage storage = new Storage();
    private Cloudinary cloudinary = new Cloudinary();
    private Resume resume = new Resume();

    // Getters
    public Telegram getTelegram() { return telegram; }
    public AI getAi() { return ai; }
    public Notion getNotion() { return notion; }
    public Storage getStorage() { return storage; }
    public Cloudinary getCloudinary() { return cloudinary; }
    public Resume getResume() { return resume; }

    // Setters
    public void setTelegram(Telegram telegram) { this.telegram = telegram; }
    public void setAi(AI ai) { this.ai = ai; }
    public void setNotion(Notion notion) { this.notion = notion; }
    public void setStorage(Storage storage) { this.storage = storage; }
    public void setCloudinary(Cloudinary cloudinary) { this.cloudinary = cloudinary; }
    public void setResume(Resume resume) { this.resume = resume; }

    public static class Telegram {
        private String apiId = "";
        private String apiHash = "";
        private String phoneNumber = "";
        @NotBlank private String channels = "";
        private int pollIntervalMinutes = 30;
        private String sessionFile = "telegram-session";

        // Getters
        public String getApiId() { return apiId; }
        public String getApiHash() { return apiHash; }
        public String getPhoneNumber() { return phoneNumber; }
        public String getChannels() { return channels; }
        public int getPollIntervalMinutes() { return pollIntervalMinutes; }
        public String getSessionFile() { return sessionFile; }

        // Setters
        public void setApiId(String apiId) { this.apiId = apiId; }
        public void setApiHash(String apiHash) { this.apiHash = apiHash; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public void setChannels(String channels) { this.channels = channels; }
        public void setPollIntervalMinutes(int pollIntervalMinutes) { this.pollIntervalMinutes = pollIntervalMinutes; }
        public void setSessionFile(String sessionFile) { this.sessionFile = sessionFile; }
    }

    public static class AI {
        private Gemini gemini = new Gemini();
        public Gemini getGemini() { return gemini; }
        public void setGemini(Gemini gemini) { this.gemini = gemini; }

        public static class Gemini {
            @NotBlank private String apiKey;
            @NotBlank String model = "gemini-1.5-flash";
            @NotBlank String relevancePrompt;
            private int rateLimitDelaySeconds = 10;

            public String getApiKey() { return apiKey; }
            public String getModel() { return model; }
            public String getRelevancePrompt() { return relevancePrompt; }
            public int getRateLimitDelaySeconds() { return rateLimitDelaySeconds; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
            public void setModel(String model) { this.model = model; }
            public void setRelevancePrompt(String relevancePrompt) { this.relevancePrompt = relevancePrompt; }
            public void setRateLimitDelaySeconds(int rateLimitDelaySeconds) { this.rateLimitDelaySeconds = rateLimitDelaySeconds; }
        }
    }

    public static class Notion {
        private String integrationToken = "";
        private String databaseId = "";
        private String version = "2022-06-28";

        public String getIntegrationToken() { return integrationToken; }
        public String getDatabaseId() { return databaseId; }
        public String getVersion() { return version; }
        public void setIntegrationToken(String integrationToken) { this.integrationToken = integrationToken; }
        public void setDatabaseId(String databaseId) { this.databaseId = databaseId; }
        public void setVersion(String version) { this.version = version; }
    }
    
    public static class Storage {
        private String type = "notion"; // Only notion storage is supported

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
    
    public static class Cloudinary {
        private String cloudName = "";
        private String apiKey = "";
        private String apiSecret = "";

        public String getCloudName() { return cloudName; }
        public String getApiKey() { return apiKey; }
        public String getApiSecret() { return apiSecret; }
        public void setCloudName(String cloudName) { this.cloudName = cloudName; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }
    }
    
    public static class Resume {
        private String templatePath = "resume-template.tex";
        private boolean generateEnabled = true;

        public String getTemplatePath() { return templatePath; }
        public boolean isGenerateEnabled() { return generateEnabled; }
        public void setTemplatePath(String templatePath) { this.templatePath = templatePath; }
        public void setGenerateEnabled(boolean generateEnabled) { this.generateEnabled = generateEnabled; }
    }
}
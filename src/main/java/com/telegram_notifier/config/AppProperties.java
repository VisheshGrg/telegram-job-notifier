package com.telegram_notifier.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Telegram telegram = new Telegram();
    private AI ai = new AI();
    private Sheets sheets = new Sheets();
    private Notion notion = new Notion();
    private Storage storage = new Storage();

    // Getters
    public Telegram getTelegram() { return telegram; }
    public AI getAi() { return ai; }
    public Sheets getSheets() { return sheets; }
    public Notion getNotion() { return notion; }
    public Storage getStorage() { return storage; }

    // Setters
    public void setTelegram(Telegram telegram) { this.telegram = telegram; }
    public void setAi(AI ai) { this.ai = ai; }
    public void setSheets(Sheets sheets) { this.sheets = sheets; }
    public void setNotion(Notion notion) { this.notion = notion; }
    public void setStorage(Storage storage) { this.storage = storage; }

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

    public static class Sheets {
        private String spreadsheetId = "";
        private String sheetRange = "Sheet1!A:H";
        private String credentialsPath = "credentials.json";

        public String getSpreadsheetId() { return spreadsheetId; }
        public String getSheetRange() { return sheetRange; }
        public String getCredentialsPath() { return credentialsPath; }
        public void setSpreadsheetId(String spreadsheetId) { this.spreadsheetId = spreadsheetId; }
        public void setSheetRange(String sheetRange) { this.sheetRange = sheetRange; }
        public void setCredentialsPath(String credentialsPath) { this.credentialsPath = credentialsPath; }
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
        private String type = "csv"; // csv, json, sqlite, google-sheets, notion
        private String filePath = "job_listings";

        public String getType() { return type; }
        public String getFilePath() { return filePath; }
        public void setType(String type) { this.type = type; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
    }
}
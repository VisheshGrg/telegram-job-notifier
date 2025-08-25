package com.telegram_notifier.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public class JobDetails {
    private String company;
    private String role;
    private String location;
    private String url;
    private String salary;
    private String sourceChannel;
    private String rawSnippet;
    private OffsetDateTime postedAt;

    public JobDetails() {}

    public JobDetails(String company, String role, String location, String url, String salary, String sourceChannel, String rawSnippet, OffsetDateTime postedAt) {
        this.company = company;
        this.role = role;
        this.location = location;
        this.url = url;
        this.salary = salary;
        this.sourceChannel = sourceChannel;
        this.rawSnippet = rawSnippet;
        this.postedAt = postedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getCompany() { return company; }
    public String getRole() { return role; }
    public String getLocation() { return location; }
    public String getUrl() { return url; }
    public String getSalary() { return salary; }
    public String getSourceChannel() { return sourceChannel; }
    public String getRawSnippet() { return rawSnippet; }
    public OffsetDateTime getPostedAt() { return postedAt; }

    // Setters
    public void setCompany(String company) { this.company = company; }
    public void setRole(String role) { this.role = role; }
    public void setLocation(String location) { this.location = location; }
    public void setUrl(String url) { this.url = url; }
    public void setSalary(String salary) { this.salary = salary; }
    public void setSourceChannel(String sourceChannel) { this.sourceChannel = sourceChannel; }
    public void setRawSnippet(String rawSnippet) { this.rawSnippet = rawSnippet; }
    public void setPostedAt(OffsetDateTime postedAt) { this.postedAt = postedAt; }
    
    // Convenience setter for string date
    public void setPostedDate(String dateString) {
        try {
            LocalDate localDate = LocalDate.parse(dateString);
            this.postedAt = localDate.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        } catch (Exception e) {
            // If parsing fails, use current time
            this.postedAt = OffsetDateTime.now();
        }
    }

    public static class Builder {
        private JobDetails jobDetails = new JobDetails();

        public Builder company(String company) { jobDetails.company = company; return this; }
        public Builder role(String role) { jobDetails.role = role; return this; }
        public Builder location(String location) { jobDetails.location = location; return this; }
        public Builder url(String url) { jobDetails.url = url; return this; }
        public Builder salary(String salary) { jobDetails.salary = salary; return this; }
        public Builder sourceChannel(String sourceChannel) { jobDetails.sourceChannel = sourceChannel; return this; }
        public Builder rawSnippet(String rawSnippet) { jobDetails.rawSnippet = rawSnippet; return this; }
        public Builder postedAt(OffsetDateTime postedAt) { jobDetails.postedAt = postedAt; return this; }

        public JobDetails build() { return jobDetails; }
    }
}
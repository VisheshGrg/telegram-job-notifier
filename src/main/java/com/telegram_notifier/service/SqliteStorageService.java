package com.telegram_notifier.service;

import com.telegram_notifier.model.JobDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SqliteStorageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SqliteStorageService.class);

    private static final String DB_FILE = "jobs.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    @PostConstruct
    public void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            createTableIfNotExists(conn);
            log.info("SQLite database initialized: {}", DB_FILE);
        } catch (SQLException e) {
            log.error("Failed to initialize SQLite database", e);
        }
    }

    private void createTableIfNotExists(Connection conn) throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS jobs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                posted_at TEXT,
                company TEXT,
                role TEXT,
                location TEXT,
                salary TEXT,
                url TEXT,
                raw_snippet TEXT,
                source_channel TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }

    public void appendJob(JobDetails job) {
        String insertSQL = """
            INSERT INTO jobs (posted_at, company, role, location, salary, url, raw_snippet, source_channel)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            String postedAt = job.getPostedAt() == null ? null :
                    job.getPostedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            pstmt.setString(1, postedAt);
            pstmt.setString(2, job.getCompany());
            pstmt.setString(3, job.getRole());
            pstmt.setString(4, job.getLocation());
            pstmt.setString(5, job.getSalary());
            pstmt.setString(6, job.getUrl());
            pstmt.setString(7, job.getRawSnippet());
            pstmt.setString(8, job.getSourceChannel());

            pstmt.executeUpdate();
            log.info("Saved job to SQLite: {} - {}", job.getCompany(), job.getRole());

        } catch (SQLException e) {
            log.error("Failed to save job to SQLite database", e);
        }
    }

    public List<JobDetails> getAllJobs() {
        List<JobDetails> jobs = new ArrayList<>();
        String selectSQL = "SELECT * FROM jobs ORDER BY created_at DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {

            while (rs.next()) {
                JobDetails job = JobDetails.builder()
                        .company(rs.getString("company"))
                        .role(rs.getString("role"))
                        .location(rs.getString("location"))
                        .salary(rs.getString("salary"))
                        .url(rs.getString("url"))
                        .rawSnippet(rs.getString("raw_snippet"))
                        .sourceChannel(rs.getString("source_channel"))
                        .build();
                jobs.add(job);
            }

        } catch (SQLException e) {
            log.error("Failed to retrieve jobs from SQLite database", e);
        }

        return jobs;
    }

    public int getJobCount() {
        String countSQL = "SELECT COUNT(*) FROM jobs";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSQL)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Failed to count jobs in SQLite database", e);
        }
        return 0;
    }
}

# Storage Alternatives - No Google Account Required!

Since Google account setup can be complex, I've created **4 different storage options** for your job data. Choose the one that works best for you:

## Option 1: CSV Files (Easiest - Recommended)

**✅ No account setup required**
**✅ Works with Excel, Google Sheets, etc.**
**✅ Human readable**

### Configuration:
```properties
app.storage.type=csv
```

### Output:
Creates `job_listings.csv` in your project folder with columns:
- Posted At, Company, Role, Location, Salary, URL, Raw Snippet, Source Channel

### How to use:
- Open `job_listings.csv` with Excel, Numbers, or any spreadsheet app
- Sort, filter, and analyze your job data

---

## Option 2: JSON Files (Developer Friendly)

**✅ No account setup required**
**✅ Easy to import into other applications**
**✅ Structured data format**

### Configuration:
```properties
app.storage.type=json
```

### Output:
Creates `job_listings.json` with structured job data:
```json
[
  {
    "company": "Google",
    "role": "Software Engineer",
    "location": "Remote",
    "salary": "$120k-160k",
    "url": "https://careers.google.com/jobs/123",
    "rawSnippet": "Looking for a talented software engineer...",
    "sourceChannel": "@remotejobs",
    "postedAt": "2024-01-15T10:30:00Z"
  }
]
```

---

## Option 3: SQLite Database (Most Powerful)

**✅ No account setup required**
**✅ Query with SQL**
**✅ Handle large amounts of data efficiently**

### Configuration:
```properties
app.storage.type=sqlite
```

### Output:
Creates `jobs.db` SQLite database file

### How to use:
- Use DB Browser for SQLite (free tool) to view data
- Query with SQL: `SELECT * FROM jobs WHERE company LIKE '%Google%'`
- Export to CSV/JSON when needed

---

## Option 4: Google Sheets (Cloud Based)

**⚠️ Requires Google account setup**
**✅ Cloud accessible from anywhere**
**✅ Real-time collaboration**

### Configuration:
```properties
app.storage.type=google-sheets
app.sheets.spreadsheet-id=your_spreadsheet_id
app.sheets.credentials-path=credentials.json
```

---

## Quick Start Configuration

### For Immediate Use (CSV):
Update your `application.properties`:
```properties
spring.application.name=telegram-notifier

# Telegram Client Configuration
app.telegram.api-id=27654662
app.telegram.api-hash=91e7448bb2fb8a55f80511d46b2cb4b2
app.telegram.phone-number=+919588568012

# AI Configuration
app.ai.gemini.api-key=AIzaSyCO7TdwTsJ4N14RvmD2H5MGkoMxWNzIJGo
app.ai.gemini.model=gemini-1.5-flash
app.ai.gemini.relevance-prompt=You are a strict filter for job posts relevant to a Software Engineer / Backend / Java / Spring Boot developer (0-3 years). Return EXACTLY "YES" if relevant, otherwise "NO".

# Storage Configuration - CSV (NO GOOGLE ACCOUNT NEEDED!)
app.storage.type=csv

# Channels to monitor
app.telegram.channels=@remotework,@backendjobs,@javajobs
app.telegram.poll-interval-minutes=30
```

### That's it! Run the app:
```bash
mvn spring-boot:run
```

## Testing Your Setup

### 1. Check health:
```bash
curl http://localhost:8080/api/jobs/health
```

### 2. Test with a manual job post:
```bash
curl -X POST http://localhost:8080/api/jobs/process-manual \
  -H "Content-Type: application/json" \
  -d '{"message": "Software Engineer position at Google. Remote work available. $120k salary. Apply at careers.google.com"}'
```

### 3. Check your storage:
```bash
curl http://localhost:8080/api/jobs/storage-info
```

### 4. View the results:
- **CSV**: Open `job_listings.csv` 
- **JSON**: Open `job_listings.json`
- **SQLite**: Use DB Browser for SQLite to open `jobs.db`

## Switching Storage Types

You can change storage type anytime by updating `app.storage.type` and restarting:

```properties
# Change from CSV to SQLite:
app.storage.type=sqlite

# Or change to JSON:
app.storage.type=json
```

## Popular Job Channels to Monitor

```properties
app.telegram.channels=@remotework,@backendjobs,@javajobs,@startupjobs,@techjobs,@pythonworkremote,@reactjobs
```

## File Outputs

After running, you'll see these files in your project:
```
demo/
├── job_listings.csv        (if using CSV)
├── job_listings.json       (if using JSON)  
├── jobs.db                 (if using SQLite)
├── telegram-session        (Telegram auth session)
└── logs/                   (application logs)
```

**Start with CSV - it's the easiest and works everywhere!**

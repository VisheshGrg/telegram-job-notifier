# Telegram Job Notifier

An automated system that monitors Telegram channels for job postings, uses AI to filter relevant positions, and automatically saves qualified job details to Google Sheets.

## Features

- **Automated Telegram Monitoring**: Continuously monitors configured Telegram channels
- **AI-Powered Filtering**: Uses Google Gemini AI to identify relevant job posts
- **Job Detail Extraction**: Automatically extracts company, role, location, salary, and other details
- **Google Sheets Integration**: Saves qualified jobs to a configured Google Spreadsheet
- **REST API**: Manual processing and management endpoints
- **Scheduled Processing**: Configurable polling intervals

## Prerequisites

1. **Java 17+**
2. **Maven 3.6+**
3. **Telegram API Credentials**: Get API ID and API Hash from [my.telegram.org](https://my.telegram.org)
4. **Google Gemini API Key**: Get from [Google AI Studio](https://aistudio.google.com/)
5. **Google Sheets API Credentials**: Set up service account in Google Cloud Console

## Setup Instructions

### 1. Telegram API Setup
1. Go to [my.telegram.org](https://my.telegram.org) and log in with your phone number
2. Go to "API Development Tools"
3. Create a new application:
   - App title: "Telegram Job Notifier" (or any name)
   - Short name: "job-notifier" (or any short name)
   - Platform: Choose "Desktop"
4. Save the `api_id` and `api_hash` - you'll need these for configuration
5. **Important**: You'll need to authenticate with your phone number when running the app for the first time

### 2. Google Sheets Setup
1. Create a Google Spreadsheet
2. Copy the spreadsheet ID from the URL
3. Set up Google Sheets API:
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create/select a project
   - Enable Google Sheets API
   - Create service account credentials
   - Download the JSON credentials file as `credentials.json`

### 3. Google Gemini API Setup
1. Go to [Google AI Studio](https://aistudio.google.com/)
2. Create an API key
3. Save the API key for configuration

### 4. Application Configuration
Edit `src/main/resources/application.properties`:

```properties
# Telegram Client Configuration  
app.telegram.api-id=your_api_id
app.telegram.api-hash=your_api_hash
app.telegram.phone-number=+1234567890
app.telegram.channels=@jobschannel1,@jobschannel2
app.telegram.poll-interval-minutes=30
app.telegram.session-file=telegram-session

# AI Configuration
app.ai.gemini.api-key=your_gemini_api_key
app.ai.gemini.model=gemini-1.5-flash
app.ai.gemini.relevance-prompt=You are a strict filter for job posts relevant to a Software Engineer / Backend / Java / Spring Boot developer (0-3 years). Return EXACTLY "YES" if relevant, otherwise "NO".

# Google Sheets Configuration
app.sheets.spreadsheet-id=your_spreadsheet_id
app.sheets.sheet-range=Sheet1!A:H
app.sheets.credentials-path=credentials.json
```

### 5. File Setup
1. Place `credentials.json` in the project root directory
2. Ensure all configuration values are filled

### 6. First Time Authentication
When you run the application for the first time:
1. The app will send an SMS code to your phone number
2. Enter the verification code in the console when prompted
3. The app will create a session file to remember your authentication
4. Subsequent runs won't require re-authentication unless you delete the session file

## Running the Application

### Development
```bash
mvn spring-boot:run
```

### Production
```bash
mvn clean package
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

## API Endpoints

### Health Check
```bash
GET /api/jobs/health
```

### Process Manual Message
```bash
POST /api/jobs/process-manual
Content-Type: application/json

{
    "message": "Your job posting text here..."
}
```

### Trigger Immediate Processing
```bash
POST /api/jobs/process-now
```

### Initialize Google Sheets
```bash
POST /api/jobs/init-sheets
```

## Google Sheets Output

The application creates a spreadsheet with the following columns:
- **Posted At**: Timestamp when the job was processed
- **Company**: Company name
- **Role**: Job title/role
- **Location**: Job location
- **Salary**: Salary information (if available)
- **URL**: Application or company URL
- **Raw Snippet**: Short snippet from the original post
- **Source Channel**: Telegram channel where the job was found

## Configuration Options

### Telegram Settings
- `app.telegram.api-id`: API ID from my.telegram.org
- `app.telegram.api-hash`: API Hash from my.telegram.org  
- `app.telegram.phone-number`: Your phone number (with country code)
- `app.telegram.channels`: Comma-separated list of channel usernames (e.g., @jobs,@hiring)
- `app.telegram.poll-interval-minutes`: How often to check for new messages

### AI Settings
- `app.ai.gemini.api-key`: Your Gemini API key
- `app.ai.gemini.model`: AI model to use (default: gemini-1.5-flash)
- `app.ai.gemini.relevance-prompt`: Prompt to determine job relevance

### Google Sheets Settings
- `app.sheets.spreadsheet-id`: Google Spreadsheet ID
- `app.sheets.sheet-range`: Range to append data (e.g., Sheet1!A:H)
- `app.sheets.credentials-path`: Path to service account credentials JSON

## Troubleshooting

### Common Issues

1. **Authentication issues**
   - Make sure you have access to the phone number for SMS verification
   - Verify API ID and API Hash are correct from my.telegram.org
   - On first run, you'll need to enter the SMS verification code

2. **Cannot read channel messages**
   - Ensure the channels are public or you have joined them with your account
   - Check that channel usernames are correct (with @ prefix in config)

2. **Google Sheets errors**
   - Check that the service account has access to the spreadsheet
   - Verify credentials.json is in the correct location
   - Ensure Google Sheets API is enabled

3. **AI API errors**
   - Verify Gemini API key is correct
   - Check API quotas and limits

### Logs
The application provides detailed logging. Check the console output for error messages and processing status.

## License

This project is open source and available under the [MIT License](LICENSE).

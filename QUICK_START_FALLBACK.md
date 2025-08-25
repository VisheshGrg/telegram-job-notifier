# Quick Start - Fallback Method (No API Credentials Required)

If you're having trouble getting Telegram API credentials from my.telegram.org, you can use the **web scraping fallback method** to get started immediately.

## What You Need (Minimal Setup)

1. **Google Gemini API Key** - [Get from here](https://aistudio.google.com/)
2. **Google Sheets setup** - [Follow Google Sheets guide](README.md#2-google-sheets-setup)

## Quick Configuration

1. **Copy the example config:**
   ```bash
   cp application.properties.example src/main/resources/application.properties
   ```

2. **Edit `src/main/resources/application.properties`:**
   ```properties
   spring.application.name=telegram-notifier

   # Telegram - Use web scraping mode (NO API credentials needed)
   app.telegram.scraping-mode=true
   app.telegram.channels=@remotework,@backendjobs,@javajobs
   app.telegram.poll-interval-minutes=30

   # AI Configuration (REQUIRED - get from https://aistudio.google.com/)
   app.ai.gemini.api-key=YOUR_GEMINI_API_KEY_HERE
   app.ai.gemini.model=gemini-1.5-flash
   app.ai.gemini.relevance-prompt=You are a strict filter for job posts relevant to a Software Engineer / Backend / Java / Spring Boot developer (0-3 years). Return EXACTLY "YES" if relevant, otherwise "NO".

   # Google Sheets Configuration (REQUIRED)
   app.sheets.spreadsheet-id=YOUR_SPREADSHEET_ID_HERE
   app.sheets.sheet-range=Sheet1!A:H
   app.sheets.credentials-path=credentials.json
   ```

3. **Place your Google Sheets `credentials.json` file in the project root**

4. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

## How Web Scraping Mode Works

- ✅ **No Telegram authentication required**
- ✅ **Works with public channels only**
- ✅ **No API rate limits**
- ⚠️ **Slightly slower than API mode**
- ⚠️ **May be less reliable than API mode**

## Supported Channels (Public channels that work well)

```properties
# Popular job channels you can monitor immediately:
app.telegram.channels=@remotework,@backendjobs,@javajobs,@startupjobs,@techjobs
```

## Testing

1. **Test the health endpoint:**
   ```bash
   curl http://localhost:8080/api/jobs/health
   ```

2. **Process messages manually:**
   ```bash
   curl -X POST http://localhost:8080/api/jobs/process-now
   ```

3. **Test with a manual job post:**
   ```bash
   curl -X POST http://localhost:8080/api/jobs/process-manual \
     -H "Content-Type: application/json" \
     -d '{"message": "Software Engineer position at Google. Remote work available. Apply at jobs.google.com"}'
   ```

## Switching to API Mode Later

Once you get Telegram API credentials:

1. Set `app.telegram.scraping-mode=false`
2. Add your API credentials:
   ```properties
   app.telegram.api-id=your_api_id
   app.telegram.api-hash=your_api_hash
   app.telegram.phone-number=+1234567890
   ```
3. Restart the application

## Troubleshooting Web Scraping Mode

**Issue: No messages found**
- Check that channels are public
- Verify channel names are correct (use @channelname format)

**Issue: Rate limiting**
- Increase `app.telegram.poll-interval-minutes` to 60 or higher

**Issue: Parsing errors**
- Some channels may have different HTML structure
- Check logs for specific parsing errors

## Popular Working Channels

These channels are known to work well with web scraping:
- `@remotework` - Remote job opportunities
- `@backendjobs` - Backend development jobs
- `@javajobs` - Java-specific positions
- `@startupjobs` - Startup opportunities
- `@techjobs` - General tech positions

Start with these and add more as needed!

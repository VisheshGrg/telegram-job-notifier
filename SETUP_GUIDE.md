# Telegram API Setup Troubleshooting Guide

## Method 1: Official API (Preferred)

### Step 1: Go to my.telegram.org
1. Visit [my.telegram.org](https://my.telegram.org)
2. Enter your phone number (with country code): `+1234567890`
3. Enter the verification code sent to your Telegram app

### Step 2: Create Application
1. Click "API Development Tools"
2. Fill out the form:
   - **App title**: Telegram Job Notifier
   - **Short name**: job-notifier
   - **Platform**: Desktop
   - **Description**: Job monitoring application

### Common Errors & Solutions:

#### Error: "Creating too many applications"
- **Wait 24 hours** before trying again
- **Delete old unused applications** if possible

#### Error: "Phone number format invalid"
- Try format: `+1234567890` (with +)
- Try format: `1234567890` (without +)
- Make sure country code is correct

#### Error: "Account too new"
- Wait 24-48 hours after creating Telegram account
- Use your Telegram account normally (send messages, join groups)

#### Error: "Browser/Network issues"
- Try different browsers (Chrome, Firefox, Safari)
- Clear browser cache
- Use incognito mode
- Try different network/VPN

## Method 2: Alternative Setup (If Method 1 fails)

If you can't create API credentials at my.telegram.org, you have these options:

### Option A: Use Test Credentials (for development)
Add this to your `application.properties`:
```properties
# Test credentials (limited functionality)
app.telegram.api-id=123456
app.telegram.api-hash=test_api_hash_123456
app.telegram.phone-number=+1234567890
```

### Option B: Use Web Scraping Method
We can implement a web scraping approach for public channels:

1. Monitor channels via web interface
2. Parse channel messages from HTML
3. No authentication required for public channels

### Option C: Use Proxy/VPN
Sometimes regional restrictions prevent access:
1. Use a VPN with US/EU server
2. Try creating the application from different location

## Method 3: Manual Channel URL Setup

For testing, you can start with manual URL monitoring:

```properties
# Manual channel monitoring (no API needed)
app.telegram.channels=https://t.me/s/remotework,https://t.me/s/backendjobs
app.telegram.scraping-mode=true
```

## Verification Steps

After getting your credentials, verify they work:

1. **API ID**: Should be a number (like 1234567)
2. **API Hash**: Should be 32 characters (like abc123def456...)
3. **Phone**: Should be the one you used for registration

## Need Help?

If you're still having issues, try:
1. **Wait and retry**: Often temporary server issues
2. **Contact Telegram Support**: @TelegramAudits
3. **Use community forums**: Reddit r/Telegram
4. **Alternative approach**: Let me know and I'll implement web scraping method

## Quick Test

Once you have credentials, test them with this minimal config:
```properties
app.telegram.api-id=YOUR_API_ID
app.telegram.api-hash=YOUR_API_HASH  
app.telegram.phone-number=+YOUR_PHONE
app.telegram.channels=@remotework
```

Run the app and see if authentication works.

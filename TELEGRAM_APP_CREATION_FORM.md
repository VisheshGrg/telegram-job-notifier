# Telegram Application Creation Form - Exact Values

When you fill out the form at [my.telegram.org](https://my.telegram.org), use these **exact values**:

## Form Fields:

### 1. App title:
```
Telegram Job Notifier
```

### 2. Short name:
```
jobnotifier123
```
*(Must be unique, try adding your initials if this is taken)*

### 3. URL:
```
https://localhost:8080
```
*(This is just a placeholder - it doesn't need to be a real website)*

### 4. Platform:
```
Desktop
```
*(Select from the dropdown)*

### 5. Description:
```
Automated job monitoring system that fetches job postings from Telegram channels and processes them using AI.
```

## Screenshot Guide:
```
┌─────────────────────────────────────┐
│ App title: Telegram Job Notifier   │
├─────────────────────────────────────┤
│ Short name: jobnotifier123          │
├─────────────────────────────────────┤
│ URL: https://localhost:8080         │
├─────────────────────────────────────┤
│ Platform: [Desktop ▼]              │
├─────────────────────────────────────┤
│ Description: Automated job...       │
├─────────────────────────────────────┤
│           [Create]                  │
└─────────────────────────────────────┘
```

## After Successful Creation:

You'll get:
- **api_id**: A number like `1234567`
- **api_hash**: A string like `abcd1234ef5678gh9012ij3456kl7890`

## Common URL Field Issues:

❌ **Don't use these:**
- Empty field
- Just "localhost" 
- Invalid URLs like "test.com" (missing https://)

✅ **These work fine:**
- `https://localhost:8080`
- `https://example.com`  
- `https://yourname.github.io`
- `https://google.com` (any valid URL)

## Still Not Working?

**Try the NO-API fallback method:**

1. **Change this in your `application.properties`:**
   ```properties
   app.telegram.scraping-mode=true
   ```

2. **You can skip the Telegram API setup completely!**

3. **Just provide:**
   - Google Gemini API key
   - Google Sheets credentials
   - Channel names (like @remotework, @backendjobs)

4. **Run the app and it will work without any Telegram authentication!**

# Deploy Telegram Notifier to Render 🚀

## Prerequisites ✅
- [x] GitHub account
- [x] Render account (free at render.com)
- [x] Your secrets ready (Telegram API, Gemini API, Notion tokens)

## Step 1: Push to GitHub 📤

```bash
# Add all files (make sure local.env is in .gitignore!)
git add .
git commit -m "Add Docker configuration for Render deployment"
git push origin main
```

## Step 2: Create Web Service on Render 🌐

1. **Go to [render.com](https://render.com)** → Sign in → **"New +"** → **"Web Service"**

2. **Connect GitHub Repository:**
   - Connect your GitHub account
   - Select your `telegram-notifier` repository
   - Branch: `main`

3. **Configure Service:**
   - **Name:** `telegram-notifier-app` (or any name you like)
   - **Environment:** `Docker`
   - **Plan:** `Free` (0$/month)
   - **Dockerfile Path:** `./Dockerfile` (should auto-detect)

## Step 3: Set Environment Variables 🔐

In Render dashboard → **Environment** tab, add these secrets:

### Required Secrets (Add as **Secret** not **Static**):
```bash
APP_TELEGRAM_API_ID=your_telegram_api_id_here
APP_TELEGRAM_API_HASH=your_telegram_api_hash_here
APP_TELEGRAM_PHONE_NUMBER=+1234567890
APP_AI_GEMINI_API_KEY=your_gemini_api_key_here
APP_NOTION_INTEGRATION_TOKEN=your_notion_integration_token_here
APP_NOTION_DATABASE_ID=your_notion_database_id_here
```

**📝 Note:** Get your actual values from your `local.env` file and paste them in Render dashboard.

### Static Variables (can be **Static**):
```bash
APP_AI_GEMINI_MODEL=gemini-2.0-flash
APP_AI_GEMINI_RATE_LIMIT_DELAY_SECONDS=10
APP_NOTION_VERSION=2022-06-28
APP_STORAGE_TYPE=notion
APP_TELEGRAM_CHANNELS=@ravi_kumar_gupta,@jobs_and_internships_updates,@jobb_test_posts
APP_TELEGRAM_POLL_INTERVAL_MINUTES=1
SPRING_PROFILES_ACTIVE=production
SERVER_PORT=8080
```

## Step 4: Deploy! 🎯

1. **Click "Create Web Service"**
2. **Wait for build** (5-10 minutes first time)
3. **Your app will be live at:** `https://your-app-name.onrender.com`

## Step 5: Test Your Deployment 🧪

Visit these URLs:
- **Health Check:** `https://your-app-name.onrender.com/api/health`
- **Manual Process:** `https://your-app-name.onrender.com/api/process-now`
- **View Status:** `https://your-app-name.onrender.com/api/processing-status`

## 🎉 Success! Your app will now:
- ✅ Run 24/7 on Render (free tier has some sleep limitations)
- ✅ Automatically fetch Telegram messages every minute
- ✅ Process through AI and store in Notion
- ✅ Handle environment variables securely

## Troubleshooting 🔧

**Build fails?**
- Check Render logs in dashboard
- Ensure all environment variables are set
- Verify Dockerfile syntax

**App sleeping on free tier?**
- Render free tier sleeps after 15 minutes of inactivity
- Consider upgrading to paid plan for 24/7 uptime
- Or set up external monitoring to ping your app

**Telegram not fetching?**
- Check if channels are public and accessible
- Verify API credentials in environment variables
- Check app logs in Render dashboard

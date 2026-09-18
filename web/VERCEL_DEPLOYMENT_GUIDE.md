# Deploying AttendMark Web Edition to Vercel (100% Free)

You can deploy the AttendMark Web Edition to **Vercel** in under 60 seconds with zero server cost and free automatic SSL.

---

### Option 1: Deploy with Vercel CLI (Super Fast)

1. Open your terminal in the `web` folder:
   ```bash
   cd web
   ```
2. Install the free Vercel CLI (if not already installed):
   ```bash
   npm install -g vercel
   ```
3. Run:
   ```bash
   vercel
   ```
4. Follow the 3 prompts:
   - *Set up and deploy?* $\rightarrow$ **Y**
   - *Which scope?* $\rightarrow$ Select your free account
   - *Link to existing project?* $\rightarrow$ **N**
   - *What's your project's name?* $\rightarrow$ **attendmark** (or any name you like)
   - *In which directory is your code located?* $\rightarrow$ **./**
5. Done! Vercel will give you a live production link like:  
   `https://attendmark.vercel.app`

---

### Option 2: Deploy via GitHub + Vercel Dashboard (No Command Line)

1. **Export / Push to GitHub**:
   - Push your project repository to GitHub (or create a new GitHub repo containing the `web` directory).
2. **Go to Vercel**:
   - Log into [vercel.com](https://vercel.com) (free signup with GitHub or Google).
3. **Import Project**:
   - Click **"Add New..."** $\rightarrow$ **"Project"**.
   - Select your GitHub repository.
4. **Configure Root Directory**:
   - Click **Edit** next to **Root Directory** and choose **`web`** (or `public`).
   - Leave Framework Preset as **Other** (it's pure HTML5 + Tailwind CSS + Vanilla JS).
5. **Deploy**:
   - Click **"Deploy"**.
   - In 10 seconds, your site is live with a permanent URL: `https://your-project.vercel.app`.

---

### How the Cloud Sync Works on Vercel

The cloud sync engine connects directly to the sync endpoint over HTTPS:
- No server code or backend needed on Vercel.
- Whether you access AttendMark from `https://attendmark.vercel.app` or Google Cloud, entering your phone's 6-character sync code will immediately connect to your phone's data!

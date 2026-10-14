# Campus Lost & Found — Simple Version

## IMPORTANT — delete the old database once

This version adds a new item status (`CANCELLED`). H2 remembers the old
list of allowed statuses, so **delete the `data` folder inside
`simple-app` once before running**. It is recreated automatically
(demo accounts included).

## What's new in this update

- **Smarter matching page.** Staff no longer use dropdowns. Each open lost
  item is a card showing its photo, description, location, reporter and
  time. Beside it, every open found item is listed as a clickable card
  with its own details and a "% similar" bar, best match first.
- **Algorithm does the first pass.** `service/MatchingService.java` ranks
  found items using **TF-IDF + cosine similarity** (plain Java, no extra
  library). Staff still make the final call.
- **"I didn't get my item".** After a match, students get a second
  button. It cancels that lost report, tells the student to submit a new
  one, notifies staff, and leaves the found item open for re-matching.
- **Saintgits background.** The three campus photos are in
  `static/images/background1-3.jpg`.

---

## Earlier changes

- **Image uploads** — both "Report Lost" and "Report Found" now have an
  optional photo field. Photos are saved to a `uploads/` folder that
  gets created automatically, and shown on the Dashboard and Matching
  Portal pages.
- **Photo carousel background on every page** — drop 3 campus photos at
  `src/main/resources/static/images/background1.jpg`,
  `background2.jpg`, and `background3.jpg` (exact names), restart the
  app, and every page will slowly crossfade between them behind the
  content. Until you add photos, it just shows a plain blue gradient —
  nothing breaks either way.
- **Matching portal improvements**: a lost item drops out of the staff
  dropdown the moment it's matched; a found item stays available so it
  can be offered to more than one possible lost report; and students
  now have an "I got my item" button to confirm pickup, which is what
  finally closes out both the lost and found report.
- **Ready for multiple laptops at once** — see below.

---

## Running this for your presentation (multiple laptops)

You do **not** need to install this on every laptop. Run it on **one**
laptop (yours), and everyone else just opens a web page.

### Step 1 — Find your laptop's IP address
On the laptop that will run the app, open Command Prompt and type:
```
ipconfig
```
Look for **IPv4 Address** under your WiFi adapter — something like
`192.168.1.23`.

### Step 2 — Run the app as usual
Run `LostFoundApplication.java` from IntelliJ/VS Code like always.

### Step 3 — Allow it through Windows Firewall (one-time)
The first time you run it, Windows will likely pop up asking to allow
Java through the firewall — click **Allow access** (both Private and
Public networks, to be safe for a classroom WiFi).

If you don't get that popup: Windows Security → Firewall & network
protection → Allow an app through firewall → find Java → check both
boxes.

### Step 4 — Everyone connects to YOUR laptop
On every other laptop, make sure they're on the **same WiFi network**
as your laptop, then open a browser to:
```
http://192.168.1.23:8080/login
```
(using the actual IP number from Step 1, not this example).

Everyone is now using the same running app and the same database — if
one person reports a lost item, everyone else sees it immediately on
their own laptop.

**Important:** your laptop (the one running the app) must stay open
and running the whole time. If you close it, everyone loses the
connection.

---

## Saving this to GitHub — why it's worth doing

- **Proof of real, incremental work.** Every commit is timestamped —
  if a professor ever asks "did you actually write this," your commit
  history shows the project being built step by step over time, not
  copy-pasted in one go.
- **Free backup.** If your laptop dies or you delete a file by mistake,
  your code is safe on GitHub's servers.
- **Working with Lijohn.** You can both push changes and pull each
  other's work instead of emailing zip files back and forth.
- **Undo button for your whole project.** If a change breaks something,
  you can look at exactly what changed and revert it.
- **Portfolio value.** A public GitHub repo is something you can link
  on a resume or show in an interview later.

### Quick setup (if you've never used Git before)
1. Install Git from [git-scm.com](https://git-scm.com) if you don't have it.
2. Create a new repository on [github.com](https://github.com) (don't
   add a README there — you already have one).
3. In a terminal, inside your `simple-app` folder:
```
git init
git add .
git commit -m "Initial working version"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/YOUR-REPO-NAME.git
git push -u origin main
```
4. From then on, after making changes:
```
git add .
git commit -m "describe what you changed"
git push
```

One thing to add before pushing: create a file named `.gitignore` in
`simple-app` with this content, so you don't upload build files or your
local database:
```
target/
data/
uploads/
```

---

## Demo logins (created automatically on first run)

| Role | Email | Password |
|---|---|---|
| Admin | admin@saintgits.org | admin123 |
| Staff | staff@saintgits.org | staff123 |
| Student | johan@saintgits.org | student123 |

## How to read this codebase (suggested order)

1. `LostFoundApplication.java` — the starting point
2. `model/User.java`, `model/Item.java` — what data looks like
3. `AuthController.java` — login/logout, the full session-based auth flow
4. `DashboardController.java` — shows the "check session, then do the
   work" pattern every other controller reuses
5. `ItemController.java` — reporting lost/found items, with the role
   restriction logic written out explicitly, plus the photo upload code
6. `MatchController.java` — staff pairing items and notifying the student

# My Reminder (Phase 1)

Status: source code written, NOT yet compiled or run. Nothing here has been tested on a device.

## What Phase 1 contains
- Create, edit, delete reminders (title, notes, date, time, repeat, priority, alert style)
- Repeat: once, daily, weekly, weekdays, weekends
- Three alert styles: standard, important (sound + vibration), full-screen (urgent alarm sound)
- Complete / Snooze 10m / Snooze 1h buttons right on the notification
- Full-screen alert screen with Complete, Snooze 5/10/15/30/60 min, custom minutes, Dismiss
- Optional "remind again every 15 min, max 3 times"
- Alarms restored after reboot, app update, clock change, timezone change
- Banners if notifications, exact alarms or full-screen permission are missing

## Build the APK for free (no Android Studio)
1. Make a free GitHub account and create a new PRIVATE repository.
2. Upload everything in this folder (including the hidden .github folder).
3. Open the repo, go to Actions, pick "Build APK", press Run workflow.
4. After about 5 to 10 minutes, open the finished run and download the artifact "MyReminder-debug-apk". Unzip it to get app-debug.apk.
If the build fails, copy the red error text and send it to Claude.

## Install on your phone
1. Send app-debug.apk to your phone (Telegram, Drive, USB).
2. Tap it. If asked, allow "Install unknown apps" for that file manager.
3. Open My Reminder, allow notifications.
4. Phone settings, Apps, My Reminder, Battery: set to Unrestricted. Also allow Autostart if your phone has it.

## First test checklist
1. Add a reminder 2 minutes ahead, close the app, wait. Did it ring?
2. Tap Snooze 10m on it. Does it come back?
3. Add one with style Full-screen, lock the phone, wait. Does the alert screen show?
4. Add a Daily reminder, restart the phone. Does it still exist and fire next day?

# WA Companion Tools (Android starter)

This is an Android companion utility that works alongside the official WhatsApp app. It does **not** modify WhatsApp, read private chats, scrape statuses, hide typing indicators, or send messages automatically.

## Included
- Save reusable quick replies locally.
- Copy a reply or share it to the official WhatsApp app.
- Schedule a local reminder; when it fires, the user chooses whether to share the message to WhatsApp.
- Shortcuts to WhatsApp and Android app settings.
- Save personal status-related notes locally.

## Build
This repository includes a GitHub Actions workflow. Open the **Actions** tab and select **Build Android APK**. The workflow runs automatically after changes are pushed to `main`. When it succeeds, download the `wa-companion-debug-apk` artifact from the run.

The project uses Android Gradle Plugin 8.7.3, Kotlin 2.0.21, compile SDK 35 and minimum Android 6.0 (API 23).

## Important
This is starter source code, not a signed production release. The APK must be installed and tested on a real device before wider distribution. Exact alarms and notifications depend on Android permissions.

## Third-party visual asset credits
- Meme War challenge photos: Unsplash contributors, credited beside each template in the app; images are used under the Unsplash License.
- Meme War sticker artwork: Twemoji by Twitter and contributors, graphics licensed CC BY 4.0. https://github.com/twitter/twemoji

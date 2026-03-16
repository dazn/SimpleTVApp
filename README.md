# SimpleTVApp

A minimal Android TV media browser and player built with Jetpack Compose and AndroidX TV Material, designed to browse and stream media served by [SimpleFileServer](https://github.com/dazn/SimpleFileServer).

## Features

- Browse a remote media server's directory tree
- Stream video and audio via ExoPlayer/media3
- Full TV remote control support with seek-based trick play (FF/RW)
- Focus-aware list navigation with remembered scroll position
- Correct display aspect ratio for anamorphic/non-square-pixel content (SAR ≠ 1:1)
- Enriched playback error overlay showing codec info when a file fails to play

## Remote Control

| Button | Action |
|--------|--------|
| D-pad Up/Down | Show/hide player controls |
| D-pad Left/Right | Seek ±10 seconds |
| Center / OK | Play/pause (or resume from trick play) |
| Fast Forward | Cycle trick play: 15× → 45× → 130× → normal |
| Rewind | Cycle trick play: −15× → −45× → −130× → −15× (wraps) |
| Play/Pause | Toggle playback |
| Play | Resume playback |
| Pause | Pause playback |
| Stop | Stop and return to browse |
| Back | Hide controls overlay / exit player |

Trick play works by pausing the player and seeking by a fixed number of seconds per interval (1 s), producing visually distinct speeds in both directions without using ExoPlayer's `setPlaybackSpeed()`. When playback reaches the end naturally, the screen returns to browse automatically after a short delay.

## Setup

Copy `local.properties.example` to `local.properties` and fill in your server details:

```
BASE_URL=http://YOUR_SERVER_IP:PORT
API_KEY=your-api-key-here
```

These are injected at build time via `BuildConfig`.

## Build

```bash
./gradlew app:assembleDebug          # Build debug APK
./gradlew app:installDebug           # Install to connected device/emulator
./gradlew app:testDebugUnitTest      # Run unit tests
./gradlew app:lintDebug              # Run lint checks
```

## Architecture

- **Single Activity** — `MainActivity.kt` hosts all Compose navigation
- **Screens**: `BrowseScreen` (directory listing) → `PlayerScreen` (video/audio playback)
- **ViewModels**: `BrowseViewModel` (directory state, focus memory), `PlayerViewModel` (player lifecycle, trick play, aspect ratio, error state)
- **Network**: plain `HttpURLConnection` for directory listing; `DefaultHttpDataSource` for streaming
- **ffprobe metadata**: server-supplied `ffprobe_response` on each file item is used to derive display aspect ratio and codec info — no transcoding, all handling is client-side
- **AndroidX TV Material** — `androidx.tv:tv-material` components for TV-optimised focus and navigation behaviour; not standard Material 3
- Min SDK 30 (Android 11) / Target SDK 36

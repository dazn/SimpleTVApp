# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew app:assembleDebug          # Build debug APK
./gradlew app:assembleRelease        # Build release APK
./gradlew app:testDebugUnitTest      # Run unit tests (debug)
./gradlew app:testDebugUnitTest --tests "org.dazn.simpletvapp.presentation.browse.BrowseViewModelTest"  # Run a single test class
./gradlew app:connectedDebugAndroidTest  # Run instrumentation tests on device
./gradlew app:lintDebug              # Run lint checks
./gradlew app:installDebug           # Install debug APK to connected device
```

## Configuration

`BASE_URL` and `API_KEY` are read from `local.properties` at build time and injected as `BuildConfig.BASE_URL` / `BuildConfig.API_KEY`. Copy `local.properties.example` to get started. Never hardcode these values.

## Architecture

Single-module Android TV app. Navigation graph lives in `App.kt` (two destinations: `browse` and `player`). `MainActivity` just sets the theme and calls `App()`.

### Data flow

```
MediaApiClient (HttpURLConnection) → MediaRepository (open class, sortable) → BrowseViewModel
BuildConfig.BASE_URL/API_KEY       → DefaultHttpDataSource (ExoPlayer)      → PlayerViewModel
```

- **`MediaRepository`** is an `open class` (not interface) so unit tests subclass it directly with `FakeMediaRepository` — no mocking library needed.
- **`BrowseViewModel`** accepts a `MediaRepository` constructor parameter (default instance provided), making it testable without DI framework.
- **`PlayerViewModel`** extends `AndroidViewModel` (needs `Application` for `ExoPlayer.Builder`). It owns the `ExoPlayer` instance for its entire lifecycle — `onCleared()` releases it. It is constructed via `PlayerViewModelFactory` (defined in the same file) which passes ffprobe-derived args (`displayAspectRatio`, `videoCodec`, `audioCodec`) through the nav back-stack.

### ffprobe metadata

The server may include a `ffprobe_response` field on `type: "file"` directory entries (full `ffprobe -show_format -show_streams` JSON). `MediaItem` deserialises this into `FfprobeResponse` → `FfprobeStream` / `FfprobeFormat` (all in `data/model/MediaItem.kt`). `kotlinx.serialization` is configured with `ignoreUnknownKeys = true` so unknown ffprobe fields are silently dropped.

`BrowseScreen` extracts `display_aspect_ratio`, video codec+profile, and audio codec from the first matching streams and passes them as nullable nav args to the player route. `PlayerViewModel` uses these to:
- Expose `val aspectRatio: Float?` (parsed from `"W:H"` string) — `PlayerScreen` applies `Modifier.aspectRatio()` when non-null, otherwise `fillMaxSize`.
- Populate `_playbackError: StateFlow<String?>` via `onPlayerError` — `PlayerScreen` shows a full-screen overlay with codec info and a "Go back" button.

### Trick-play (FF/RW)

`PlayerViewModel` implements seek-based trick play instead of `setPlaybackSpeed()`. On FF/RW: player is paused, a coroutine (`trickPlayJob`) seeks by `deltaSeconds * 333ms` every 333 ms. `_playbackSpeed` stores the delta as a float (e.g. `2f`, `-5f`) — the sign encodes direction. `resetSpeed()` cancels the job and calls `player.play()`.

### Directory navigation

`BrowseViewModel` owns all directory traversal — there is no Compose nav back-stack entry per directory. `loadPath(path)` replaces `currentPath` and reloads; `navigateUp()` derives the parent by string-splitting on `"/"`. Focus memory is an in-memory `Map<String, String>` (`lastFocusedByPath`) keyed by directory path — it survives intra-session navigation but is lost on process death.

### TV-specific notes

- Use `androidx.tv.*` components (`androidx.tv:tv-material`) instead of standard Material 3 — required for TV focus/navigation behaviour.
- The manifest declares `LEANBACK_LAUNCHER` (required for TV home screen) and marks touchscreen as optional.
- Min SDK 30 (Android 11); target SDK 36.
- All dependencies are version-catalogued in `gradle/libs.versions.toml`.

## TV Sample Apps

`tv-samples/` contains read-only Google reference projects. `JetStreamCompose` is the most relevant for modern TV UI patterns. Do not modify anything under `tv-samples/`.

# SnapTube Clone

An open, clean-room Android client recreating the core features and user experience of **SnapTube** (v7.63.0.76360410).

Designed with a modular, reliable architecture, this project serves as a complete functional blueprint for a media downloading and playback application on Android.

---

## Architecture & Modules

The project is structured into clean separation-of-concerns Gradle modules:

- **`:app`** — Main application entry point, Jetpack Compose UI (`HomeScreen`, `SearchScreen`, `ChooseFormatScreen`, `LibraryScreen`, `SettingsScreen`), Navigation graph, and ViewModels.
- **`:core:common`** — Dispatchers, result wrappers, and formatting utilities.
- **`:core:model`** — Domain data classes (`DownloadTask`, `MediaInfo`, `DownloadFormat`, `LocalMedia`, `Playlist`, `QualityPreference`).
- **`:core:database`** — Room database (`SnapTubeDatabase`), entities (mirroring the observed live `app_database.db` tables: `history`, `delete_record`, `download_task`, `search_history`, `bookmark`, `playlist`), and DAOs.
- **`:core:extractor`** — Media extraction registry, YouTube extractor (InnerTube client, signature decipherer, n-parameter transform), social media extractors (Instagram, TikTok, Facebook, Twitter), and generic HTML/OpenGraph extractor.
- **`:core:download`** — Resumable HTTP downloader (`ResumableDownloader`), storage manager (`StorageManager`), download engine (`DownloadEngine`), foreground download service (`DownloadService`), and Media3 Transformer muxer (`MediaMuxer`).
- **`:core:player`** — Standalone ExoPlayer playback controller (`PlaybackController`) and `.pls` playlist parser (`PlsPlaylistParser`).

---

## Key Features Recreated

1. **Multi-Source Extraction**: Dedicated extractors for YouTube, Instagram, TikTok, Facebook, Twitter, and any public HTML page with OpenGraph or HTML5 video tags.
2. **Format & Quality Selection**: Detailed quality variants (MP4 resolutions from 144p to 4K/8K, bitrates, WEBM, M4A, MP3 audio conversion).
3. **Resumable Download Manager**: Chunked downloading with `.part` files, Range header resume support, pause/resume/cancel/retry flows, and persistent queues that survive process death.
4. **Background Downloads**: Foreground `dataSync` service keeping downloads alive while the app is backgrounded.
5. **On-Device Muxing**: Combines video-only and audio-only streams into a single playable MP4 container using Media3 Transformer.
6. **Media Library & Player**: Full local video/audio scanner (MediaStore), completed downloads view, search history, recycle bin, and built-in player with playback speed presets (0.25x–2.0x) and landscape/fullscreen toggle.
7. **Settings & Customization**: DataStore-backed preferences for custom download directories (SAF), Wi-Fi only mode, concurrency limits, theme modes (System/Light/Dark), and language selection.
8. **Intents & Sharing**: Handles `ACTION_SEND` (sharing a link from another browser/app) and `ACTION_VIEW` deep links (`snaptube://`, `snaptubeapp.com`).

---

## Building the APK

> **Environment Note**: Building an APK/AAB is not possible directly inside mobile terminal environments (no local JDK/Android SDK). Build the project on a workstation (Linux, macOS, or Windows) or via GitHub Actions.

### Prerequisites
- JDK 17
- Android SDK (API 35)
- Gradle 8.9+

### Build Commands (Workstation)
```bash
# Clone or open project directory
cd SnapTubeClone

# Build debug APK
./gradlew assembleDebug

# Build release APK (with R8 minification & shrinking)
./gradlew assembleRelease
```

The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk` or `app/build/outputs/apk/release/app-release-unsigned.apk`.

---

## Legal & Compliance

- **Clean-Room Implementation**: Built completely from scratch without copying binary or obfuscated code from the original application.
- **No DRM Bypass**: Respects site terms of service; excludes DRM-protected content and geo-restricted paywalls.
- **Open Standards**: Uses public APIs and standard open-source patterns (similar to NewPipe and yt-dlp).

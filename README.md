# Free Whiteboard Animator

A **100% free, offline, open-source** Android app for creating whiteboard animation videos. Built with Kotlin 2.0 and Jetpack Compose.

![Android](https://img.shields.io/badge/Android-SDK%2026%2B-green) ![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue) ![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-purple)

## ✨ Features

- **Script to Video**: Enter text → auto-split into scenes → generate video
- **Hand-drawing animations**: Classic whiteboard reveal effect
- **Built-in assets**: Hands, backgrounds, doodle icons included
- **Text-to-Speech**: Offline TTS using Android system voices
- **Auto-synced subtitles**: Animated text overlays
- **Timeline editor**: Edit scenes, timing, images
- **Templates**: Pre-made templates for education, business, marketing
- **Export to MP4**: Up to 1080p, social media presets (YouTube, TikTok, Instagram)
- **Offline-first**: No internet required after install
- **No watermarks**: 100% free forever

## 📱 Requirements

- Android 8.0 (API 26) or higher
- 4GB+ RAM recommended
- ~50MB storage for app + projects

## 🚀 Quick Start

### Option 1: Build from Source

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/FreeWhiteboardAnimator.git
   cd FreeWhiteboardAnimator
   ```

2. **Open in Android Studio**
   - Android Studio Hedgehog or newer
   - JDK 17+ required

3. **Build the APK**
   ```bash
   ./gradlew assembleDebug
   ```
   
4. **Find the APK** at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

5. **Install on device**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Option 2: Download APK

Download the latest release from the Releases page and install directly.

## 📖 How to Use

1. **Create a new project** or use a template
2. **Enter your script** - each paragraph becomes a scene
3. **Customize scenes** - edit text, add images, adjust timing
4. **Preview** your animation
5. **Export** to MP4 video

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Database | Room |
| Video Export | MediaMuxer + MediaCodec |
| Background Tasks | WorkManager |
| TTS | Android TextToSpeech |
| Navigation | Navigation Compose |

## 📁 Project Structure

```
app/src/main/
├── java/com/freewhiteboard/animator/
│   ├── MainActivity.kt
│   ├── WhiteboardAnimatorApp.kt
│   ├── data/
│   │   ├── database/    # Room DB, DAOs
│   │   ├── model/       # Entities, enums
│   │   └── repository/  # Data access
│   ├── engine/
│   │   ├── AnimationEngine.kt   # Canvas rendering
│   │   ├── AssetManager.kt      # Asset loading
│   │   ├── TTSManager.kt        # Text-to-speech
│   │   └── VideoExporter.kt     # MP4 encoding
│   ├── ui/
│   │   ├── navigation/  # Nav graph
│   │   ├── screens/     # Compose screens
│   │   └── theme/       # Material 3 theme
│   └── work/
│       └── ExportWorker.kt  # Background export
└── res/
    ├── drawable/        # Vector assets
    ├── values/          # Strings, themes
    └── mipmap/          # App icons
```

## 🔒 Privacy

- **No internet required** - works 100% offline
- **No analytics** - no tracking whatsoever
- **No accounts** - no login needed
- **No ads** - completely ad-free
- **Local storage only** - your data stays on your device

## 📄 License

MIT License - Free for personal and commercial use.

## 🤝 Contributing

Contributions welcome! Please open an issue or pull request.

---

**Made with ❤️ for creators who need free tools**

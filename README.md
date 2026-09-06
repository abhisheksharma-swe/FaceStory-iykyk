# FaceStory

An Android application that transforms group videos into 9:16 Instagram-ready story collages. FaceStory runs entirely on-device to detect individuals, track continuous appearance segments across the video, pick their sharpest portrait, and render a high-resolution portrait grid.


## Features

- **On-Device Face Detection**: Uses Google ML Kit to detect faces across extracted video frames.
- **Bipartite Appearance Tracking**: Groups consecutive frame detections into continuous appearance tracks using Hungarian bipartite matching and spatial/temporal gating.
- **Identity Clustering (Constrained HAC)**: Generates 128-d MobileFaceNet embeddings and merges non-overlapping appearance segments using Hierarchical Agglomerative Clustering with strict temporal mutual exclusivity constraints.
- **Intelligent Portrait Selection**: Evaluates face crops based on Laplacian variance (sharpness), head pose frontality, eye-open probabilities, scale, and boundary margins to select the best frame for each person.
- **Dynamic 9:16 Story Collage**: Renders a vertical Instagram Story collage with adaptive grid layouts (1 to 20+ people), high-res bilinear portrait crops, and glassmorphism appearance badges.
- **Gallery Export & Native Sharing**: Save directly to `Pictures/FaceStory` via Android MediaStore or share immediately via Android Sharesheet.


## Tech Stack

- **UI**: Jetpack Compose, Material 3, Material Icons Extended
- **Language**: Kotlin & Coroutines / Flow
- **Architecture**: MVVM + Clean Architecture (UseCases, Domain Models, Repositories)
- **ML & Vision**:
  - Google ML Kit Face Detection (`com.google.mlkit:face-detection:16.1.7`)
  - TensorFlow Lite (`org.tensorflow:tensorflow-lite:2.14.0`) with MobileFaceNet model
- **Media**: Android MediaMetadataRetriever, MediaStore API, FileProvider

## Project Structure

```
app/src/main/java/com/iykyk/facestory/
├── MainActivity.kt
├── data/
│   ├── export/         # CollageRenderer, CollageExporter, ShareManager
│   ├── ml/             # MLKitFaceDetector, TFLiteFaceEmbedder
│   └── video/          # VideoFrameExtractor
├── domain/
│   ├── model/          # AppearanceSegment, DetectedFace, PersonCluster, ProcessingState
│   └── usecase/        # ProcessVideoUseCase, ClusterFacesUseCase, TrackAppearancesUseCase, ...
├── ml/                 # ImageQualityEvaluator
├── ui/
│   ├── screen/         # HomeScreen, ProcessingScreen, ResultScreen, NoFacesScreen, ErrorScreen
│   ├── theme/          # Color, Theme, Type
│   └── viewmodel/      # CollageViewModel
└── util/               # BitmapUtils, Constants, MathUtils, UniversalLogger
```


## Requirements

- Android Studio Ladybug (2024.2+) or newer
- JDK 17
- Android SDK 37 (Compile/Target)
- Minimum SDK: Android 7.0 (API level 24)


## Building and Testing

### Run Unit Tests
```bash
./gradlew test
```

### Build Debug APK
```bash
./gradlew assembleDebug
```
The resulting APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

## License

This project is licensed under the MIT License.

# Source and build status

Date: 2026-10-30

## Current source snapshot: 0.6.1 (`versionCode` 12)

This ZIP contains the updated Android source. It is not a prebuilt APK. The source version is intentionally set to
0.6.1 so an APK rebuilt from this tree can be distinguished from the earlier 0.6.0 / versionCode 11 package.

### Requested UI and behavior changes present in this source

- Auto Settings exposes Fastest, Balanced, Most accurate, and a Custom Settings editor for the persisted engine settings.
- Appearance exposes Solid colors, Gradient, and Liquid Glass treatments above the palette selector. Appearance starts collapsed.
- The old live-statistics strip is not rendered in the chat screen. A wide-screen toolbar control expands the chat by hiding the history pane and restores split view when tapped again.
- The chat composer is centered and constrained to a maximum width.
- Settings automatically expands Model & loading when the loaded model's session configuration differs from the stored settings.
- Temperature, Top-P, Top-K and Seed are supplied as per-request sampling overrides; session/load-time settings continue to require a reload when they differ.
- Native UI Components are opt-in and rendered from bounded JSON data using predefined Compose components. Model-provided HTML/JavaScript is not executed by this renderer.
- Chat messages submitted while a model is loading are queued and processed in FIFO order after the engine becomes ready.
- Image search has its own enable switch; automatic image lookup after web search is separately controlled and off by default.

### Checks completed in this editing session

| Check | Result |
|---|---|
| `bash scripts/verify-requested-features.sh` | 27 static integration checks passed |
| ZIP/source content audit | Full project tree retained; only the intentional `LivePerformanceMonitor.kt` removal and added `NativeUiComponents.kt` differ from the supplied auto-settings baseline, alongside the associated feature edits |
| Chat toolbar change | Expand/restore action connected to the existing wide-screen history-pane state |
| Version marker | 0.6.1 / versionCode 12 |

### Build and device verification boundary

A Gradle compile was attempted with `./gradlew :app:compileDebugKotlin --no-daemon --console=plain`, but the wrapper could not download Gradle 8.9 because this environment cannot resolve `services.gradle.org` (`UnknownHostException`). There is no configured Android SDK/NDK in this environment. Therefore:

- No successful Android/Kotlin Gradle build is claimed.
- No new APK is included in this ZIP.
- Compose rendering, Android runtime behavior, model loading, image search, and inference speed still need to be checked on a device after building this exact source tree.

The static integration script checks that the requested features are connected across their source files; it is not a compiler or a substitute for device tests.

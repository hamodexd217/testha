<p align="center">
  <img src="docs/assets/logo.png" alt="BigMoeOnEdge+" width="220">
</p>

<h1 align="center">BigMoeOnEdge+</h1>

<p align="center">A tablet-friendly Android app for running local GGUF language models with a native chat UI and a customized BigMoeOnEdge inference engine.</p>

<p align="center">
  <a href="https://github.com/hamodexd217/BigMoeOnEdge-plus/releases/latest">Latest APK</a> ·
  <a href="https://github.com/hamodexd217/BigMoeOnEdge-plus/issues">Issues</a> ·
  <a href="docs/STATUS.md">Testing status</a>
</p>

---

## Overview

BigMoeOnEdge+ runs local `.gguf` models on Android. It supports dense and Mixture-of-Experts models, persistent conversations, streaming generation, reasoning display, model management, attachments, web search, tools, vision through compatible `mmproj` files, generated files, and an optional local API server.

The Android app version in this source tree is **0.6.1** (`versionCode` 12). The native engine is based on the project's customized BigMoeOnEdge 0.28.1-compatible source. This is a source snapshot; it is not an APK, and an APK built from an older checkout will not contain these changes.

## Current features

### Local models and chat

- Load dense and MoE GGUF models; import projector (`mmproj`) files for compatible vision models.
- Persistent Room-backed conversations and per-chat drafts.
- Streaming text and reasoning, message editing/copying, source links, and per-message generation statistics.
- Queue messages while a model is loading. Queued messages are sent in order once the model is ready.
- A centered chat composer with a maximum width, designed for tablet layouts.
- On wide screens, use the dedicated expand/restore button in the chat toolbar, or the pane handle, to hide chat history and use the full conversation width.

### Settings and appearance

- **Auto Settings:** choose Fastest, Balanced, Most Accurate, or configure a Custom Settings profile.
- The **Model & loading** section opens automatically when a change requires reloading the currently loaded model.
- Sampling settings such as Temperature, Top-P, Top-K, and Seed are applied per request and do not by themselves require model reload. Load-time settings such as context size, CPU thread counts, batch width, cache allocation, I/O and routing features still require a reload.
- Three visual treatments for the app: **Solid colors**, **Gradient**, and **Liquid Glass**. Liquid Glass uses translucent, frosted-looking surfaces and subtle borders; it intentionally avoids costly real-time blur.
- Built-in and custom color palettes.

### Native UI and artifacts

- **Native UI Components** are optional and off by default. When enabled, the model can return a compact `bmoe-ui` JSON block for the app to render as safe Jetpack Compose cards, metrics, bars, selectable cards, and action buttons. The block is data, not executable code; HTML/JavaScript is not required.
- Native UI Components are separate from **Artifacts**. Explicit HTML, SVG, and Mermaid artifacts use the existing preview flow; supported source-code blocks remain copyable/highlighted instead of being run as arbitrary application code.

### Search, tools, and attachments

- Web search with automatic or always-on search behavior.
- Optional image-search tool and an independent setting to search for relevant images after a web search. Automatic image follow-up is off by default.
- Optional file, code, and utility tools. Destructive or file-changing actions use confirmation where applicable.
- Text, source files, archives, documents, images, and video attachments, subject to model and processing limits.

### Optional local API

The app can expose its loaded model over a local HTTP API for compatible tools on the device. It supports OpenAI-style chat completions and Anthropic Messages endpoints. The server is off by default and uses an API key; network binding is configurable. See [`docs/API_SERVER.md`](docs/API_SERVER.md) before enabling it.

## Download

Use the [latest published APK](https://github.com/hamodexd217/BigMoeOnEdge-plus/releases/latest). The APK available there may not include changes from this source snapshot unless a release has been built and published after those changes. The source version is bumped to 0.6.1 / versionCode 12 so a freshly built APK can be distinguished from the earlier 0.6.0 / versionCode 11 build.

### Where to find the updated UI

- **Settings → Auto settings:** choose Fastest, Balanced, Most accurate, or open **Custom settings** to edit the engine profile directly.
- **Settings → Appearance:** the **Surface style** selector appears above the palette list. Choose Solid colors, Gradient, or Liquid glass.
- **Chat on a wide screen:** the expand icon in the top toolbar hides the history pane; its restore icon brings the split view back. The old live-statistics strip is not part of the chat layout.
- **Settings → Tools:** Native UI Components is optional and off by default.
- **Settings → Web search:** image search has its own enable switch, separate from the optional automatic image lookup after a web search. Automatic follow-up is off by default.

## Build from source

Requirements:

- Android Studio with the project's compatible Android Gradle Plugin and Kotlin toolchain.
- Android SDK 35 and a matching NDK/CMake setup for the vendored native engine.
- A network connection the first time Gradle downloads its wrapper and dependencies, unless these are already cached.

Build a debug APK from the repository root:

```bash
./gradlew :app:assembleDebug
```

The APK is normally written under `app/build/outputs/apk/debug/`. For GitHub Actions, make sure artifact storage has available quota and configure stable signing secrets for signed release builds; do not commit keystores or passwords.

The default build targets `arm64-v8a`. The optional Hexagon/NPU path requires its separate compatible toolchain and packaged runtime libraries; a Snapdragon chipset alone does not mean the NPU backend is available in a given APK. See [`docs/npu-android-build.md`](docs/npu-android-build.md).

## Project layout

```text
app/src/main/java/com/bigmoe/onedge/
  core/       engine coordination, generation, model loading, agent/tool routing
  data/       Room entities and DataStore settings
  parser/     Markdown, code fences, and artifact metadata
  server/     optional local HTTP API
  tools/      web, file, code, and utility tools
  ui/         Jetpack Compose screens and native UI renderer
  workspace/  generated-file workspace and diff support
app/src/main/cpp/
  core/       customized native inference engine and local patches
  third_party/ vendored llama.cpp source
patches/      reproducible project-specific native patches
docs/         architecture, model loading, API, and validation notes
scripts/      offline checks and diagnostics helpers
.github/workflows/  CI workflows
```

## Validation status and limitations

The current source changes still need a fresh Android Gradle build and device testing. During this editing session, the Gradle wrapper could not download Gradle because the environment could not resolve `services.gradle.org`; therefore, a successful Kotlin/Android compile is **not** claimed. Static source checks are useful for catching obvious issues but are not a substitute for Gradle compilation or testing on an Android device.

Expected behavior can vary by model architecture, quantization, context length, engine build options, Android version, and thermal state. Do not treat estimated memory or performance figures as device measurements. Image and video handling depends on the selected model and compatible projector files. The optional local API and NPU path require additional real-device testing.

## Credits and license

BigMoeOnEdge+ builds on [BigMoeOnEdge](https://github.com/Helldez/BigMoeOnEdge), [llama.cpp](https://github.com/ggml-org/llama.cpp), Jetpack Compose, Room, DataStore, and the Android ecosystem. Vendored dependencies retain their own notices and licenses. BigMoeOnEdge+ is licensed under Apache License 2.0; see [`LICENSE`](LICENSE).

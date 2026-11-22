# Parallax Zero-DEX Native V2

A separate native-only Android variant for ParallaxDPT.

## Invariants

- No Java or Kotlin application classes.
- No shell/bootstrap `classes.dex`.
- `android:hasCode="false"`.
- Startup uses framework `android.app.NativeActivity`.
- Native entry point is `ANativeActivity_onCreate` in `libParallaxN.so`.
- CI hard-fails if any `classes.dex`, `classes2.dex`, or other `classes*.dex` entry is present in the final APK.

## Scope

This is a genuine native-only package path. It is suitable for NDK-native applications, native SDK experiments, and native-first application designs.

It does not transparently convert an existing Java/Kotlin APK into a zero-DEX APK. Java/Kotlin activities, services, providers, and application logic need ART bytecode unless they are redesigned or ported to native code.

## Build artifact

Run the `Parallax Zero-DEX Native V2` GitHub Actions workflow. The artifact contains:

- `parallax-zero-dex-native-v2.apk`
- `apk-contents.txt`
- `SHA256SUMS`

The workflow builds `armeabi-v7a`, `arm64-v8a`, `x86`, and `x86_64` copies of `libParallaxN.so` and verifies the signed APK contains exactly zero DEX files.

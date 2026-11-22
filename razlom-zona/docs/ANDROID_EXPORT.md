# Android packaging preparation

**Engine:** Godot 4.4 standard, renderer GL Compatibility for a lower-end Android baseline. Source code is not an APK.

To build on a machine with Godot 4.4 installed:

1. Install matching **Godot Android export templates** from the Editor menu.
2. Install a supported JDK and Android SDK including platform-tools, build-tools and an Android platform (follow the official Godot documentation matching your chosen engine version).
3. Configure Editor > Editor Settings > Export > Android with paths to Java SDK, Android SDK, and your debug keystore.
4. Project > Export > Add > Android. Choose a unique Android package identifier such as `com.razlom.zona`, and configure landscape orientation, architecture and application icons.
5. Export a **debug APK** to a local `build/` directory (excluded from source control). Install on the device and test touch, saving and FPS.

**No secrets or signing keys belong in the repo.** A release-signing key must remain private.

Once an export preset is validated on the real engine/device, Android CI can be added without inventing build success. Current checks are source checks only.

#include <android/log.h>
#include <android/native_activity.h>
#include <android/native_window.h>

#include <cstddef>
#include <cstdint>

namespace {

constexpr const char *kTag = "ParallaxZeroDexV2";
constexpr uint32_t kBackground = 0xff141414u;

void logEvent(const char *event) {
    __android_log_print(ANDROID_LOG_INFO, kTag, "%s", event);
}

void drawBackground(ANativeWindow *window) {
    if (window == nullptr) {
        return;
    }

    if (ANativeWindow_setBuffersGeometry(window, 0, 0, WINDOW_FORMAT_RGBA_8888) != 0) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "window geometry setup failed");
        return;
    }

    ANativeWindow_Buffer buffer{};
    if (ANativeWindow_lock(window, &buffer, nullptr) != 0 || buffer.bits == nullptr) {
        __android_log_print(ANDROID_LOG_WARN, kTag, "window lock failed");
        return;
    }

    auto *pixels = static_cast<uint32_t *>(buffer.bits);
    for (int y = 0; y < buffer.height; ++y) {
        uint32_t *row = pixels + (y * buffer.stride);
        for (int x = 0; x < buffer.width; ++x) {
            row[x] = kBackground;
        }
    }

    ANativeWindow_unlockAndPost(window);
}

void onStart(ANativeActivity *) { logEvent("onStart"); }
void onResume(ANativeActivity *) { logEvent("onResume"); }
void onPause(ANativeActivity *) { logEvent("onPause"); }
void onStop(ANativeActivity *) { logEvent("onStop"); }
void onDestroy(ANativeActivity *) { logEvent("onDestroy"); }

void onWindowCreated(ANativeActivity *, ANativeWindow *window) {
    logEvent("onNativeWindowCreated");
    drawBackground(window);
}

void onWindowDestroyed(ANativeActivity *, ANativeWindow *) {
    logEvent("onNativeWindowDestroyed");
}

}  // namespace

extern "C" __attribute__((visibility("default"), used))
void ANativeActivity_onCreate(ANativeActivity *activity,
                              void *savedState,
                              size_t savedStateSize) {
    (void)savedState;
    (void)savedStateSize;

    if (activity == nullptr || activity->callbacks == nullptr) {
        return;
    }

    activity->callbacks->onStart = onStart;
    activity->callbacks->onResume = onResume;
    activity->callbacks->onPause = onPause;
    activity->callbacks->onStop = onStop;
    activity->callbacks->onDestroy = onDestroy;
    activity->callbacks->onNativeWindowCreated = onWindowCreated;
    activity->callbacks->onNativeWindowDestroyed = onWindowDestroyed;

    __android_log_print(
            ANDROID_LOG_INFO,
            kTag,
            "Parallax Zero-DEX Native V2 started with no application DEX bootstrap");
}

// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("plugin");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("plugin")
//      }
//    }
#include <cstring>
#include <dlfcn.h>
#include <jni.h>
#include "bpf_hook.h"
#include "logging.h"
#include "native_api.h"

static HookFunType hook_func = nullptr;

void on_library_loaded(const char *name, void *handle) {
    if (std::string(name).ends_with("libfuse_jni.so")) {
        bpf_hook::Hook(handle, hook_func);
    }
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
jint JNI_OnLoad(JavaVM *jvm, void *v __unused) {
    JNIEnv *env;
    if (jvm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}

extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    hook_func = entries->hook_func;
    return on_library_loaded;
}

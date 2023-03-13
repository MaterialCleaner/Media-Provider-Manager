/*
 * Copyright 2023 Green Mushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <dlfcn.h>
#include <regex>
#include <string>
#include <sys/system_properties.h>
#include "bpf_hook.h"

#define LOG_TAG "FuseDaemon"

#include "logging.h"

// Regex copied from FileUtils.java in MediaProvider, but without media directory.
const std::regex PATTERN_OWNED_PATH(
        "^/storage/[^/]+/(?:[0-9]+/)?Android/(?:data|obb)/([^/]+)(/?.*)?",
        std::regex_constants::icase);
static constexpr char PRIMARY_VOLUME_PREFIX[] = "/storage/emulated";

bool isPackageOwnedPath(const std::string &path) {
    return path.length() >= 33 && std::regex_match(path, PATTERN_OWNED_PATH);
}

bool (*old_StartsWith)(std::string_view s, std::string_view prefix);

bool new_StartsWith(std::string_view s, std::string_view prefix) {
    if (prefix == PRIMARY_VOLUME_PREFIX) {
        auto path = std::string(s);
        if (isPackageOwnedPath(path)) {
            return false;
        }
    }
    return old_StartsWith(s, prefix);
}

bool (*old_containsMount)(const std::string &path);

bool new_containsMount(const std::string &path) {
    if (isPackageOwnedPath(path)) {
        return true;
    }
    return old_containsMount(path);
}

bool (*old_containsMount_30)(const std::string &path, const std::string &userid);

bool new_containsMount_30(const std::string &path, const std::string &userid) {
    if (isPackageOwnedPath(path)) {
        return true;
    }
    return old_containsMount_30(path, userid);
}

namespace bpf_hook {

    int GetApiLevel() {
        char buf[PROP_VALUE_MAX] = {0};
        __system_property_get("ro.build.version.sdk", buf);
        return atoi(buf);
    }

    bool IsFuse() {
        char prop[PROP_VALUE_MAX] = {0};
        __system_property_get("persist.sys.fuse", prop);
        return !strcmp(prop, "true");
    }

    void Hook(void *handle, HookFunType hook_func) {
        if (!IsFuse()) {
            return;
        }
        // bypass restriction
        if (GetApiLevel() >= 31) {
            auto startsWith = dlsym(handle,
                                    "_ZN7android4base10StartsWithENSt6__ndk117basic_string_viewIcNS1_11char_traitsIcEEEES5_");
            if (startsWith != nullptr) {
                hook_func((void *) startsWith, (void *) new_StartsWith, (void **) &old_StartsWith);
            } else {
                LOGE("failed to find StartsWith");
            }
        }
        // protect mount point
        auto containsMount = dlsym(handle,
                                   "_ZN13mediaprovider4fuse13containsMountERKNSt6__ndk112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEE");
        if (containsMount != nullptr) {
            hook_func((void *) containsMount, (void *) new_containsMount,
                      (void **) &old_containsMount);
        } else {
            auto containsMount_30 = dlsym(handle,
                                          "_ZN13mediaprovider4fuse13containsMountERKNSt6__ndk112basic_stringIcNS1_11char_traitsIcEENS1_9allocatorIcEEEES9_");
            if (containsMount_30 != nullptr) {
                hook_func((void *) containsMount_30, (void *) new_containsMount_30,
                          (void **) &old_containsMount_30);
            } else {
                LOGE("failed to find containsMount");
            }
        }
    }
}  // namespace bpf_hook

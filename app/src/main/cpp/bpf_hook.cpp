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

bool (*old_StartsWith)(std::string_view s, std::string_view prefix);

bool new_StartsWith(std::string_view s, std::string_view prefix) {
    if (prefix == PRIMARY_VOLUME_PREFIX) {
        auto path = std::string(s);
        std::smatch match;
        if (std::regex_match(path, match, PATTERN_OWNED_PATH)) {
            return false;
        }
    }
    return old_StartsWith(s, prefix);
}

namespace bpf_hook {
    static bool isFuse() {
        char prop[PROP_VALUE_MAX] = {0};
        __system_property_get("persist.sys.fuse", prop);
        return !strcmp(prop, "true");
    }

    void Hook(void *handle, HookFunType hook_func) {
        if (!isFuse()) {
            return;
        }
        auto startsWith = dlsym(handle,
                                "_ZN7android4base10StartsWithENSt6__ndk117basic_string_viewIcNS1_11char_traitsIcEEEES5_");
        if (startsWith != nullptr) {
            hook_func((void *) startsWith, (void *) new_StartsWith, (void **) &old_StartsWith);
        }
    }
}  // namespace bpf_hook

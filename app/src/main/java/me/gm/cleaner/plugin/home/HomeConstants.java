/*
 * Copyright 2021 Green Mushroom
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

package me.gm.cleaner.plugin.home;

public class HomeConstants {
    public static String TRASHES;
    public static String PACKAGE_INFO;
    public static String PATH;
    public static String LENGTH;
    public static String IS_CHECKED;

    public static String DIR;
    public static String TITLE;
    public static String ICON;
    public static String SERVICE;

    static {
        TRASHES = "trashes";
        PACKAGE_INFO = "appInfo";
        PATH = "path";
        LENGTH = "length";
        IS_CHECKED = "isChecked";

        DIR = "dir";
        TITLE = "title";
        ICON = "icon";
        SERVICE = "service";
    }
}

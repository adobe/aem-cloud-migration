/*
 Copyright 2019 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.skyline.migration.util;

import java.util.regex.Pattern;

/**
 * Utilities that contain rules specific to the JCR.  Note that this tool only reads from and writes to disk, so while
 * this utility class contains JCR rules, it does not contain actual JCR access methods.
 */
public class JcrUtil {

    public static boolean isJcrSafePath(String path) {
        Pattern unsafePath = Pattern.compile(".*[\\:\\[\\]\\|\\* \\.\\\"\\'].*");
        return !unsafePath.matcher(path).matches();
    }

    // Replaces JCR-Illegal characters with underscores as specified in
    // https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/reference-materials/javadoc/com/day/cq/commons/jcr/JcrUtil.html#STANDARD_LABEL_CHAR_MAPPING
    // The characters are '.', '/', ':', '[', ']', '*', ''', '"', '|', ' '
    public static String getJcrSafeNodeName(String name) {
        return name.replaceAll("[\\/\\:\\[\\]\\|\\* \\.\\\"\\']", "_").toLowerCase();
    }
}

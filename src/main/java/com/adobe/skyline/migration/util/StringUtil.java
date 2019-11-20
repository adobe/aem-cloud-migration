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

import java.util.*;

/**
 * Utilities for String manipulation
 */
public class StringUtil {

    public static List<String> getListFromString(String in) {
        List<String> retVal = new ArrayList<>();

        if (in.length() > 0) {
            if (in.startsWith("[")) {
                in = removeBrackets(in);
            }

            String[] items = in.split(",");

            for (String item : items) {
                retVal.add(item.trim());
            }
        }

        return retVal;
    }

    public static String concatenateCollectionToCsv(Collection<String> in) {
        StringBuilder builder = new StringBuilder();

        Iterator<String> iter = in.iterator();

        while (iter.hasNext()) {
            builder.append(iter.next());
            if (iter.hasNext()) {
                builder.append(",");
            }
        }

        return builder.toString();
    }

    public static String removeBrackets(String in) {
        if (in.length() > 0) {
            in = in.substring(1); //Remove opening bracket
            in = in.substring(0, in.length() - 1); //Remove closing bracket
        }

        return in;
    }
}

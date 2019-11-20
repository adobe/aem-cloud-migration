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

package com.adobe.skyline.migration.util.file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.adobe.skyline.migration.util.file.matchers.FileNameMatcher;
import com.adobe.skyline.migration.util.file.matchers.XmlContentMatcher;

/**
 * Service with methods to search through files based on name or content
 */
public class FileQueryService {

    public List<String> findFileByName(String fileName, File pathToSearch) {
        FileMatcher nameMatcher = new FileNameMatcher(fileName);
        return findFilesInDirectoryRecursive(nameMatcher, pathToSearch);
    }

    public List<String> findFilesByNodeProperty(String propertyName, String propertyValue, File pathToSearch) {
        XmlContentMatcher xmlMatcher = new XmlContentMatcher(propertyName, propertyValue);
        return findFilesInDirectoryRecursive(xmlMatcher, pathToSearch);
    }

    private List<String> findFilesInDirectoryRecursive(FileMatcher matcher, File pathToSearch) {
        List<String> matchedFilePaths = new ArrayList<>();

        if (pathToSearch.exists()) {
            if (matcher.matches(pathToSearch)) {
                matchedFilePaths.add(pathToSearch.getAbsoluteFile().toString());
            }
            if (pathToSearch.isDirectory()) {
                for (File child : pathToSearch.listFiles()) { //Recurse into child directories
                    List<String> foundInSubDirectory = findFilesInDirectoryRecursive(matcher, child);
                    matchedFilePaths.addAll(foundInSubDirectory);
                }
            }
        }

        return matchedFilePaths;
    }
}
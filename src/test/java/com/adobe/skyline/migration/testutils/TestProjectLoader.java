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

package com.adobe.skyline.migration.testutils;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.rules.TemporaryFolder;

import com.adobe.skyline.migration.util.file.FileUtil;

public class TestProjectLoader {

    public File copyMigratedProjectToTemp(TemporaryFolder temp) {
        return copyResourceDirectoryToTemp("archetype23", temp);
    }

    public File copyConfProjectToTemp(TemporaryFolder temp) {
        return copyResourceDirectoryToTemp("archetype17", temp);
    }

    public File copyEtcProjectToTemp(TemporaryFolder temp) {
        return copyResourceDirectoryToTemp("archetype11", temp);
    }

    public File copyMissingWorkflowProjectToTemp(TemporaryFolder temp) {
        return copyResourceDirectoryToTemp("testMissingWFProject", temp);
    }

    public File copyNoModelProjectToTemp(TemporaryFolder temp) {
        return copyResourceDirectoryToTemp("testProjectNoModel", temp);
    }

    public File copyNoLauncherProjectToTemp(TemporaryFolder temp) {
        return copyResourceDirectoryToTemp("testProjectNoLauncher", temp);
    }

    private File copyResourceDirectoryToTemp(String resourceName, TemporaryFolder temp) {
        File tempFolder = null;

        try {
            tempFolder = temp.newFolder();
            File customerProjectRoot = new File(getClass().getClassLoader().getResource(resourceName).getPath());
            FileUtil.copyDirectoryRecursively(customerProjectRoot, tempFolder);
        } catch (Exception e) {
            fail("Exception occurred when creating test project: " + e.getMessage());
        }

        return tempFolder;
    }
}

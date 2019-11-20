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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.adobe.skyline.migration.SkylineMigrationBaseTest;

public class FileQueryServiceTest extends SkylineMigrationBaseTest {

    private static final String PATH_TO_CONF_WORKFLOW = "/ui.conf.wf/src/main/content/jcr_root/conf/global/settings/workflow";
    private static final String PATH_TO_VAR_WORKFLOW = "/ui.conf.wf/src/main/content/jcr_root/var/workflow";


    @Test
    public void testRecursiveNameSearch() {
        String projectPath = projectLoader.copyConfProjectToTemp(temp).getPath();
        String subDirectoryName = "workflow";

        List<String> result = queryService.findFileByName(subDirectoryName, new File(projectPath));
        assertEquals("File not found.", 2, result.size());

        boolean confFound = false;
        boolean varFound = false;

        for (String path : result) {
            if(path.equals(projectPath + PATH_TO_CONF_WORKFLOW)) {
                confFound = true;
            } else if (path.equals(projectPath + PATH_TO_VAR_WORKFLOW)) {
                varFound = true;
            }
        }

        assertTrue("Expected file not found", confFound && varFound);
    }

    @Test
    public void testRecursiveNameSearchMatchesInitialInput() {
        String projectPath = projectLoader.copyConfProjectToTemp(temp).getPath();
        String subDirectoryName = "workflow";

        List<String> result = queryService.findFileByName(subDirectoryName, new File(projectPath + PATH_TO_CONF_WORKFLOW));
        assertEquals("File not found.", 1, result.size());
    }

    @Test
    public void testXmlPropertySearch() {
        String projectPath = projectLoader.copyConfProjectToTemp(temp).getPath();
        File projectFile = new File(projectPath);

        List<String> result = queryService.findFilesByNodeProperty("jcr:primaryType", "cq:WorkflowLauncher", projectFile);
        assertEquals("Not all files found.", 13, result.size());
    }
}
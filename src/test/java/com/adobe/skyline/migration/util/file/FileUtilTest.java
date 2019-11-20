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

import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.Assert.*;

public class FileUtilTest extends SkylineMigrationBaseTest {

    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testEmptyLineRemoval() throws IOException {
        String targetPath = temp.newFolder().toPath().toString() + "/testEmptyLines.xml";
        File testFile = new File(getClass().getClassLoader().getResource("testEmptyLines.xml").getPath());
        File copiedFile = Files.copy(testFile.toPath(), FileSystems.getDefault().getPath(targetPath)).toFile();

        FileUtil.removeEmptyLinesFromFile(copiedFile);

        Scanner scanner = new Scanner(copiedFile);

        while (scanner.hasNext()) {
            if (scanner.nextLine().equals("")) {
                fail("Empty line found.");
            }
        }
    }
}
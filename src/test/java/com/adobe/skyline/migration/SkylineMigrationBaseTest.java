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

package com.adobe.skyline.migration;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.skyline.migration.main.WorkflowStepConfiguration;
import com.adobe.skyline.migration.testutils.TestProjectLoader;
import com.adobe.skyline.migration.util.file.FileQueryService;

@RunWith(MockitoJUnitRunner.class)
public abstract class SkylineMigrationBaseTest {

    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    protected TestProjectLoader projectLoader;

    protected WorkflowStepConfiguration config;
    protected FileQueryService queryService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        projectLoader = new TestProjectLoader();

        config = new WorkflowStepConfiguration();
        queryService = new FileQueryService();
    }

}

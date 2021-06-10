/*
 Copyright 2020 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.skyline.migration.transformer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.dao.FilterFileDAO;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.model.workflow.WorkflowProject;

import static com.adobe.skyline.migration.MigrationConstants.*;
import static com.adobe.skyline.migration.testutils.TestConstants.CONF_WORKFLOW_PROJECT_NAME;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.endsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VarNodeCleanerTest extends SkylineMigrationBaseTest {

    @Mock FilterFileDAO filterFileDAO;

    @Mock ChangeTrackingService changeTrackingService;

    @Mock WorkflowProject workflowProject;

    private File tempProjectRoot;

    private VarNodeCleaner cleaner;

    @Before
    public void setUp() {
        super.setUp();
        this.tempProjectRoot = projectLoader.copyConfProjectToTemp(temp);
        when(workflowProject.getPath()).thenReturn(tempProjectRoot.getPath() + "/ui.content");
        this.cleaner = new VarNodeCleaner(filterFileDAO, changeTrackingService);
    }

    @Test
    public void testNoVarPathDoesntChangeAnything() {
        when(filterFileDAO.findPathsWith(any())).thenReturn(new ArrayList<>());

        cleaner.cleanNodes(workflowProject);

        verify(filterFileDAO, never()).removePath(any());
        verify(changeTrackingService, never()).trackVarPathDeleted(any());
    }

    @Test
    public void testVarFilesDeleted() {
        File varWorkflowDir = new File(tempProjectRoot, "ui.content/src/main/content/jcr_root/var/workflow");
        assertTrue(varWorkflowDir.exists());
        when(filterFileDAO.findPathsWith(any())).thenReturn(Arrays.asList("/var/workflow"));

        cleaner.cleanNodes(workflowProject);

        assertFalse(varWorkflowDir.exists());
        verify(filterFileDAO).removePath("/var/workflow");
        verify(changeTrackingService).trackVarPathDeleted(endsWith(CONF_WORKFLOW_PROJECT_NAME + File.separator + SRC + File.separator + MAIN + File.separator + CONTENT + File.separator + JCR_ROOT_ON_DISK + File.separator + VAR + File.separator + WORKFLOW));
    }
}
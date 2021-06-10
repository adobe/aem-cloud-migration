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

package com.adobe.skyline.migration.transformer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.adobe.skyline.migration.exception.CustomerDataException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.dao.WorkflowLauncherDAO;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.model.workflow.Workflow;
import com.adobe.skyline.migration.model.workflow.WorkflowLauncher;
import com.adobe.skyline.migration.model.workflow.WorkflowProject;
import com.adobe.skyline.migration.testutils.TestConstants;

public class LauncherDisablerTest extends SkylineMigrationBaseTest {

    private File tempProjectRoot;

    @Mock
    private WorkflowLauncherDAO launcherDAO;

    private LauncherDisabler disabler;
    private ChangeTrackingService changeTracker;

    @Before
    public void setUp() {
        super.setUp();
        this.tempProjectRoot = projectLoader.copyConfProjectToTemp(temp);
        this.changeTracker = new ChangeTrackingService();
        this.disabler = new LauncherDisabler(launcherDAO, changeTracker);
    }

    @Test
    public void testLaunchersDisabled() {
        projectLoader.copyConfProjectToTemp(temp);

        try {
            WorkflowLauncher launcherA = createLauncher(
                    "/content/dam(/.*/)(marketing/seasonal)(/.*/)renditions/original",
                    "update-asset-marketing-launcher");
            Workflow workflowA = new Workflow();
            workflowA.addLauncher(launcherA);

            WorkflowLauncher launcherB = createLauncher(
                    "/content/dam/projects/marketing/seasonal(/.*)/04_digital/delivery_site/delivery_scene7(?!/jcr:content/folderThumbnail)(/.*)",
                    "scene7_digitalfolder");
            WorkflowLauncher launcherC = createLauncher(
                    "/content/dam/projects/marketing/seasonal(/.*)/04_digital/delivery_shop_app/delivery_scene7(?!/jcr:content/folderThumbnail)(/.*)",
                    "scene7_digitalfolder_delivery_shop");
            Workflow workflowB = new Workflow();
            workflowB.addLauncher(launcherB);
            workflowB.addLauncher(launcherC);

            List<Workflow> workflows = new ArrayList<>();
            workflows.add(workflowA);
            workflows.add(workflowB);

            WorkflowProject wfProject = new WorkflowProject();
            wfProject.setWorkflows(workflows);

            disabler.disableLaunchers(wfProject);

            verify(launcherDAO).disableLauncher(launcherA);
            verify(launcherDAO).disableLauncher(launcherB);
            verify(launcherDAO).disableLauncher(launcherC);

            List<String> disabledLaunchers = changeTracker.getDisabledLaunchers();
            assertTrue(disabledLaunchers.contains("update-asset-marketing-launcher"));
            assertTrue(disabledLaunchers.contains("scene7_digitalfolder"));
            assertTrue(disabledLaunchers.contains("scene7_digitalfolder_delivery_shop"));

        } catch(Exception e) {
            fail("Unexpected exception caught: " + e.getMessage());
        }
    }

    @Test
    public void testSyntheticLaunchersSkipped() throws CustomerDataException {
        WorkflowLauncher launcherA = createLauncher(
                "/content/dam(/.*/)(marketing/seasonal)(/.*/)renditions/original",
                "update-asset-marketing-launcher");
        launcherA.setSynthetic(true);
        Workflow workflowA = new Workflow();
        workflowA.addLauncher(launcherA);

        List<Workflow> workflows = new ArrayList<>();
        workflows.add(workflowA);

        WorkflowProject wfProject = new WorkflowProject();
        wfProject.setWorkflows(workflows);

        disabler.disableLaunchers(wfProject);

        verify(launcherDAO, never()).disableLauncher(launcherA);
    }

    private WorkflowLauncher createLauncher(String glob, String name) {
        WorkflowLauncher launcher = new WorkflowLauncher();

        launcher.setGlob(glob);
        launcher.setName(name);
        launcher.setRelativePath(TestConstants.CONF_LAUNCHER_PATH + name + "/"
            + MigrationConstants.CONTENT_XML);
        launcher.setLauncherFile(new File(getAbsolutePathForLauncher(name)));

        return launcher;
    }

    private String getAbsolutePathForLauncher(String launcherName) {
        return Path.of(tempProjectRoot.getPath(), TestConstants.CONF_WORKFLOW_PROJECT_NAME,
                TestConstants.CONF_LAUNCHER_PATH, launcherName, MigrationConstants.CONTENT_XML).toString();
    }
}
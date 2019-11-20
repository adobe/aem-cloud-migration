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

package com.adobe.skyline.migration.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.exception.CustomerDataException;
import com.adobe.skyline.migration.model.workflow.WorkflowLauncher;
import com.adobe.skyline.migration.testutils.TestConstants;
import com.adobe.skyline.migration.util.XmlUtil;

public class WorkflowLauncherDAOTest extends SkylineMigrationBaseTest {

    private File tempProjectRoot;
    private WorkflowLauncherDAO dao;

    @Before
    public void setUp() {
        super.setUp();
        this.tempProjectRoot = projectLoader.copyConfProjectToTemp(temp);
        this.dao = new WorkflowLauncherDAO();
    }

    @Test
    public void testLaunchersDisabled() throws CustomerDataException {
        projectLoader.copyConfProjectToTemp(temp);

        WorkflowLauncher launcherA = createLauncher(
                "/content/dam(/.*/)(marketing/seasonal)(/.*/)renditions/original",
                "update-asset-marketing-launcher");
        dao.disableLauncher(launcherA);

        WorkflowLauncher launcherB = createLauncher(
                "/content/dam/projects/marketing/seasonal(/.*)/04_digital/delivery_site/delivery_scene7(?!/jcr:content/folderThumbnail)(/.*)",
                "scene7_digitalfolder");
        dao.disableLauncher(launcherB);

        WorkflowLauncher launcherC = createLauncher(
                "/content/dam/projects/marketing/seasonal(/.*)/04_digital/delivery_shop_app/delivery_scene7(?!/jcr:content/folderThumbnail)(/.*)",
                "scene7_digitalfolder_delivery_shop");
        dao.disableLauncher(launcherC);

        assertDisabled("update-asset-marketing-launcher");
        assertDisabled("scene7_digitalfolder");
        assertDisabled("scene7_digitalfolder_delivery_shop");
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

    private void assertDisabled(String launcherName) {
        try {
            File targetLauncher = new File(getAbsolutePathForLauncher(launcherName));
            Document targetLauncherXml = XmlUtil.loadXml(targetLauncher);
            String enabled = targetLauncherXml.getFirstChild().getAttributes().getNamedItem(
                    MigrationConstants.ENABLED_PROP).getTextContent();
            assertEquals(MigrationConstants.FALSE_VALUE, enabled);
        } catch (Exception e) {
            fail(
                    "Exception occurred while evaluating target launcher configuration: "
                            + e.getMessage());
        }
    }

    private String getAbsolutePathForLauncher(String launcherName) {
        return tempProjectRoot.getPath() + File.separator + TestConstants.WORKFLOW_PROJECT_NAME + File.separator +
                TestConstants.CONF_LAUNCHER_PATH + launcherName + File.separator + MigrationConstants.CONTENT_XML;
    }
}
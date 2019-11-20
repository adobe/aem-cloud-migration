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

package com.adobe.skyline.migration.parser;

import static org.junit.Assert.*;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.dao.WorkflowLauncherDAO;
import com.adobe.skyline.migration.dao.WorkflowModelDAO;
import com.adobe.skyline.migration.model.workflow.Workflow;
import com.adobe.skyline.migration.model.workflow.WorkflowLauncher;
import com.adobe.skyline.migration.model.workflow.WorkflowModel;
import com.adobe.skyline.migration.model.workflow.WorkflowProject;
import org.junit.Before;
import org.junit.Test;

import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.exception.CustomerDataException;

import java.util.List;

public class CustomerProjectLoaderTest extends SkylineMigrationBaseTest {

    private CustomerProjectLoader loader;

    @Before
    public void setUp() {
        super.setUp();

        WorkflowLauncherDAO launcherDAO = new WorkflowLauncherDAO();
        WorkflowModelDAO modelDAO = new WorkflowModelDAO();

        this.loader = new CustomerProjectLoader(queryService, launcherDAO, modelDAO);
    }

    @Test
    public void testMissingWFProjectThrowsException() {
        try {
            String missingWfProjectPath = projectLoader.copyMissingWorkflowProjectToTemp(temp).getPath();
            loader.getWorkflowProjects(missingWfProjectPath);
            fail("Exception expected, but not thrown.");
        } catch (CustomerDataException cde) {
            //success
        }
    }

    @Test
    public void testModelExtractionFromConf() throws CustomerDataException {
        String projectPath = projectLoader.copyConfProjectToTemp(temp).getPath();

        WorkflowProject workflowProject = loader.getWorkflowProjects(projectPath).get(0);
        List<Workflow> workflows = workflowProject.getWorkflows();

        boolean s7found = false;
        boolean updateAssetFound = false;

        for (Workflow workflow : workflows) {
            WorkflowModel model = workflow.getWorkflowModel();
            String modelName = model.getName();

            if (modelName.equals("scene7-digitalfolder")) {
                s7found = true;
                assertEquals("/conf/global/settings/workflow/models/scene7-digitalfolder", model.getConfigurationPage());
                assertEquals("/var/workflow/models/scene7-digitalfolder", model.getRuntimeComponent());
                assertEquals(1, model.getWorkflowSteps().size());
            } else if(modelName.equals("update_asset")) {
                updateAssetFound = true;
                assertEquals("/conf/global/settings/workflow/models/dam/update_asset", model.getConfigurationPage());
                assertEquals("/var/workflow/models/dam/update_asset", model.getRuntimeComponent());
                assertEquals(10, model.getWorkflowSteps().size());
            }
        }

        assertTrue(s7found);
        assertTrue(updateAssetFound);
    }

    @Test
    public void testModelExtractionFromEtc() throws CustomerDataException {
        String projectPath = projectLoader.copyEtcProjectToTemp(temp).getPath();

        WorkflowProject workflowProject = loader.getWorkflowProjects(projectPath).get(0);
        List<Workflow> workflows = workflowProject.getWorkflows();

        boolean xmpFound = false;
        boolean updateAssetFound = false;

        for (Workflow workflow : workflows) {
            WorkflowModel model = workflow.getWorkflowModel();
            String modelName = model.getName();

            if(modelName.equals("update_asset")) {
                updateAssetFound = true;
                assertEquals("/etc/workflow/models/dam/update_asset", model.getConfigurationPage());
                assertEquals("/etc/workflow/models/dam/update_asset/jcr:content/model", model.getRuntimeComponent());
                assertEquals(14, model.getWorkflowSteps().size());
            }
        }

        assertTrue(updateAssetFound);
    }

    @Test
    public void testConfWorkflowProjectPathReturned() throws CustomerDataException {
        String projectPath = projectLoader.copyConfProjectToTemp(temp).getPath();
        List<WorkflowProject> projects = loader.getWorkflowProjects(projectPath);
        validateLauncherPaths(projects, MigrationConstants.PATH_TO_CONF_GLOBAL_LAUNCHER_CONFIG);
    }

    @Test
    public void testLaunchersFoundForLegacyWorkflowLocation() throws CustomerDataException {
        String projectPath = projectLoader.copyEtcProjectToTemp(temp).getPath();
        List<WorkflowProject> projects = loader.getWorkflowProjects(projectPath);
        validateLauncherPaths(projects, MigrationConstants.PATH_TO_ETC_LAUNCHER_CONFIG);
    }

    @Test
    public void testCollectionLauncherExcluded() throws CustomerDataException {
        String projectPath = projectLoader.copyConfProjectToTemp(temp).getPath();
        List<WorkflowProject> projects = loader.getWorkflowProjects(projectPath);

        for (WorkflowProject wfProj : projects) {
            for (Workflow wf : wfProj.getWorkflows()) {
                for (WorkflowLauncher launcher : wf.getLaunchers()) {
                    if (launcher.getGlob().contains("collections")) {
                        fail("Collection-focused launchers should not be included.");
                    }
                }
            }
        }
    }

    @Test
    public void testMetadataLauncherExcluded() throws CustomerDataException {
        String projectPath = projectLoader.copyConfProjectToTemp(temp).getPath();
        List<WorkflowProject> projects = loader.getWorkflowProjects(projectPath);

        for (WorkflowProject wfProj : projects) {
            for (Workflow wf : wfProj.getWorkflows()) {
                for (WorkflowLauncher launcher : wf.getLaunchers()) {
                    if (launcher.getGlob().contains("metadata")) {
                        fail("Collection-focused launchers should not be included.");
                    }
                }
            }
        }
    }

    @Test
    public void testModelLoadedWithoutLauncher() throws CustomerDataException {
        String projectPath = projectLoader.copyNoLauncherProjectToTemp(temp).getPath();
        List<WorkflowProject> projects = loader.getWorkflowProjects(projectPath);
        assertEquals("update_asset", projects.get(0).getWorkflows().get(0).getWorkflowModel().getName());
    }
    
    private void validateLauncherPaths(List<WorkflowProject> projects, String launcherRoot) {
        for (WorkflowProject wfProj : projects) {
            for (Workflow wf : wfProj.getWorkflows()) {
                for (WorkflowLauncher launcher : wf.getLaunchers()) {
                    String relPath = launcher.getRelativePath();
                    assertTrue(relPath.contains(MigrationConstants.WORKFLOW));
                    assertTrue(relPath.contains(MigrationConstants.LAUNCHER));
                    assertTrue(relPath.contains(MigrationConstants.CONFIG));
                    assertTrue(relPath.contains(MigrationConstants.CONTENT_XML));

                    String launcherFolder = relPath.substring(0, relPath.lastIndexOf("/"));
                    String launcherConfigRoot = launcherFolder.substring(0, launcherFolder.lastIndexOf("/"));
                    assertEquals(launcherRoot, launcherConfigRoot);
                }
            }
        }
    }
}
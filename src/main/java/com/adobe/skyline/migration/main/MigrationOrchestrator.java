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

package com.adobe.skyline.migration.main;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.dao.ContainerProjectDAO;
import com.adobe.skyline.migration.dao.FilterFileDAO;
import com.adobe.skyline.migration.dao.MavenProjectDAO;
import com.adobe.skyline.migration.dao.ProcessingProfileDAO;
import com.adobe.skyline.migration.dao.WorkflowLauncherDAO;
import com.adobe.skyline.migration.dao.WorkflowModelDAO;
import com.adobe.skyline.migration.dao.WorkflowRunnerConfigDAO;
import com.adobe.skyline.migration.exception.CustomerDataException;
import com.adobe.skyline.migration.exception.ProjectCreationException;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.model.workflow.Workflow;
import com.adobe.skyline.migration.model.workflow.WorkflowProject;
import com.adobe.skyline.migration.parser.CustomerProjectLoader;
import com.adobe.skyline.migration.transformer.LauncherDisabler;
import com.adobe.skyline.migration.transformer.MigrationReportWriter;
import com.adobe.skyline.migration.transformer.ModelTransformer;
import com.adobe.skyline.migration.transformer.VarNodeCleaner;
import com.adobe.skyline.migration.transformer.WorkflowRunnerConfigCreator;
import com.adobe.skyline.migration.transformer.processingprofile.ProcessingProfileCreator;
import com.adobe.skyline.migration.transformer.processingprofile.ProfileMapperFactory;
import com.adobe.skyline.migration.transformer.processingprofile.ProfileMapperFactoryImpl;
import com.adobe.skyline.migration.util.Logger;
import com.adobe.skyline.migration.util.file.FileQueryService;

/**
 * The MigrationOrchestrator manages creating each of the various services, DAOs, parsers, and transformers and then calls
 * each of the parsers and transformers to actually perform the work of the migration.  The unit test for this class serves
 * as an integration test for the end-to-end migration.
 */
class MigrationOrchestrator {
    private String customerProjectPath;
    private WorkflowStepConfiguration config;
    private String reportOutputDirectory;

    MigrationOrchestrator(String customerProjectPath, WorkflowStepConfiguration config, String reportOutputDirectory) {
        this.customerProjectPath = customerProjectPath;
        this.config = config;
        this.reportOutputDirectory = reportOutputDirectory;
    }

    void exec() throws CustomerDataException, ProjectCreationException {
        //Initialize General Services
        FileQueryService queryService = new FileQueryService();
        ProfileMapperFactory mapperFactory = new ProfileMapperFactoryImpl();
        ChangeTrackingService changeTracker = new ChangeTrackingService();

        //Initialize Data Access Objects
        WorkflowLauncherDAO launcherDAO = new WorkflowLauncherDAO();
        WorkflowModelDAO modelDAO = new WorkflowModelDAO();
        FilterFileDAO appsFilterDAO = new FilterFileDAO(Paths.get(customerProjectPath, MigrationConstants.MIGRATION_PROJECT_APPS).toString());
        FilterFileDAO contentFilterDAO = new FilterFileDAO(Paths.get(customerProjectPath, MigrationConstants.MIGRATION_PROJECT_CONTENT).toString());
        ProcessingProfileDAO ppDAO = new ProcessingProfileDAO(Paths.get(customerProjectPath, MigrationConstants.MIGRATION_PROJECT_CONTENT).toString());
        WorkflowRunnerConfigDAO runnerConfigDAO = new WorkflowRunnerConfigDAO(Paths.get(customerProjectPath, MigrationConstants.MIGRATION_PROJECT_APPS).toString());

        //Load customer projects
        CustomerProjectLoader loader = new CustomerProjectLoader(queryService, launcherDAO, modelDAO);
        List<WorkflowProject> projects = loader.getWorkflowProjects(customerProjectPath);

        MavenProjectDAO mavenProjectDAO;
        if (loader.isCloudManagerReady(customerProjectPath)) {
            String containerProjectPath = loader.getContainerProjectPath(customerProjectPath);
            ContainerProjectDAO containerProjectDAO = new ContainerProjectDAO(containerProjectPath);
            mavenProjectDAO = new MavenProjectDAO(customerProjectPath, changeTracker, containerProjectDAO);
        } else {
            mavenProjectDAO = new MavenProjectDAO(customerProjectPath, changeTracker);
        }

        //Initialize Transformer Objects
        LauncherDisabler launcherDisabler = new LauncherDisabler(launcherDAO, changeTracker);
        ModelTransformer modelTransformer = new ModelTransformer(config, modelDAO, changeTracker);
        ProcessingProfileCreator ppCreator = new ProcessingProfileCreator(mapperFactory, ppDAO, contentFilterDAO, changeTracker, mavenProjectDAO);
        WorkflowRunnerConfigCreator runnerConfigCreator = new WorkflowRunnerConfigCreator(runnerConfigDAO, appsFilterDAO, changeTracker, mavenProjectDAO);
        MigrationReportWriter reportWriter = new MigrationReportWriter(changeTracker);

        //Execute migration
        for (WorkflowProject wfProject : projects) {
            launcherDisabler.disableLaunchers(wfProject);
            ppCreator.createProfiles(wfProject);

            for (Workflow workflow : wfProject.getWorkflows()) {
                if (workflow.getWorkflowModel() != null) {
                    modelTransformer.transformModel(workflow);
                    runnerConfigCreator.createWorkflowConfigs(workflow);
                }
            }

            FilterFileDAO wfProjectFilterDAO = new FilterFileDAO(wfProject.getPath());
            VarNodeCleaner varNodeCleaner = new VarNodeCleaner(wfProjectFilterDAO, changeTracker);
            varNodeCleaner.cleanNodes(wfProject);
        }

        reportWriter.write(new File(reportOutputDirectory));
        Logger.INFO("Migration complete.  A report file has been created at " + Paths.get(reportOutputDirectory, MigrationConstants.REPORT_FILENAME, ".").toString());
    }
}

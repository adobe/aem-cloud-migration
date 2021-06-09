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

package com.adobe.skyline.migration.transformer.processingprofile;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.dao.FilterFileDAO;
import com.adobe.skyline.migration.dao.MavenProjectDAO;
import com.adobe.skyline.migration.dao.ProcessingProfileDAO;
import com.adobe.skyline.migration.exception.ProjectCreationException;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.model.ProcessingProfile;
import com.adobe.skyline.migration.model.workflow.*;
import com.adobe.skyline.migration.testutils.matchers.ProcessingProfileMatcher;
import com.adobe.skyline.migration.testutils.matchers.RenditionConfigMatcher;
import com.adobe.skyline.migration.transformer.processingprofile.ProcessingProfileCreator;
import com.adobe.skyline.migration.transformer.processingprofile.ProfileMapperFactory;
import com.adobe.skyline.migration.transformer.processingprofile.ProfileMapperFactoryImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ProcessingProfileCreatorTest extends SkylineMigrationBaseTest {

    private static final String WORKFLOW_NAME = "Test Workflow";

    @Mock
    private ProcessingProfileDAO ppDao;

    @Mock
    private FilterFileDAO ffDao;

    @Mock
    private MavenProjectDAO mavenProjectDAO;

    private ChangeTrackingService changeTracker;

    private ProcessingProfileCreator creator;

    @Before
    public void setUp() {
        super.setUp();

        ProfileMapperFactory mapperFactory = new ProfileMapperFactoryImpl();
        changeTracker = new ChangeTrackingService();

        this.creator = new ProcessingProfileCreator(mapperFactory, ppDao, ffDao, changeTracker, mavenProjectDAO);
    }

    @Test
    public void testPdfProfileAdded() throws ProjectCreationException {
        WorkflowStep step = getPdfPreviewWorkflowStep();
        WorkflowProject project = wrapStepInProject(step);

        creator.createProfiles(project);

        RenditionConfigMatcher renMatcher = new RenditionConfigMatcher();
        renMatcher.expectName("cqdam.preview.png");
        renMatcher.expectFormat("png");
        renMatcher.expectHeight(2048);
        renMatcher.expectWidth(2048);

        ProcessingProfileMatcher profMatcher = new ProcessingProfileMatcher();
        profMatcher.expectName("Migrated from " + WORKFLOW_NAME);
        profMatcher.expectRendition(renMatcher);

        verify(ppDao).addProfile(argThat(profMatcher));
        verify(ffDao).addPath(MigrationConstants.PROCESSING_PROFILE_JCR_PATH);
        verify(mavenProjectDAO).createProject(MigrationConstants.MIGRATION_PROJECT_CONTENT);

        List<ProcessingProfile> createdProfiles = changeTracker.getProcessingProfilesCreated();
        assertEquals(1, createdProfiles.size());
        assertEquals("Migrated from " + WORKFLOW_NAME, createdProfiles.get(0).getName());
    }

    @Test
    public void testUnsupportedWorkflowStepsSkipped() throws ProjectCreationException {
        WorkflowStep step = new WorkflowStep();
        step.setProcess("com.adobe.test.CustomWorkflowProcess");
        WorkflowProject project = wrapStepInProject(step);

        creator.createProfiles(project);

        verify(ppDao, never()).addProfile(any());
        verify(ffDao, never()).addPath(any());
        verify(mavenProjectDAO, never()).createProject(any());
    }

    @Test
    public void testDefaultMimeTypeIncludes() throws ProjectCreationException {
        WorkflowStep step = getPdfPreviewWorkflowStep();
        WorkflowProject project = wrapStepInProject(step);

        creator.createProfiles(project);

        RenditionConfigMatcher renMatcher = new RenditionConfigMatcher();
        renMatcher.expectIncludedMimetype(MigrationConstants.DEFAULT_MIMETYPE); // If no matchers are included, apply the default

        ProcessingProfileMatcher profMatcher = new ProcessingProfileMatcher();
        profMatcher.expectRendition(renMatcher);

        verify(ppDao).addProfile(argThat(profMatcher));
    }

    @Test
    public void testMimetypeRestrictionsFromLaunchersIncluded() throws ProjectCreationException {
        WorkflowStep step = getPdfPreviewWorkflowStep();
        WorkflowModel model = wrapStepInModel(step);

        List<String> conditions = new ArrayList<>();
        conditions.add("jcr:content/jcr:mimeType=image/.*");
        conditions.add("jcr:content/jcr:mimeType!=video/.*");
        conditions.add("jcr:content/jcr:mimeType!=audio/.*");

        WorkflowLauncher launcher = new WorkflowLauncher();
        launcher.setConditions(conditions);

        Workflow workflow = new Workflow();
        workflow.setWorkflowModel(model);
        workflow.addLauncher(launcher);

        WorkflowProject project = wrapWorkflowInProject(workflow);

        creator.createProfiles(project);

        RenditionConfigMatcher renMatcher = new RenditionConfigMatcher();
        renMatcher.expectIncludedMimetype("image/.*");
        renMatcher.expectExcludedMimetype("video/.*");
        renMatcher.expectExcludedMimetype("audio/.*");

        ProcessingProfileMatcher profMatcher = new ProcessingProfileMatcher();
        profMatcher.expectRendition(renMatcher);

        verify(ppDao).addProfile(argThat(profMatcher));
    }

    @Test
    public void testMimetypeNormalization() throws ProjectCreationException {
        WorkflowStep step = getPdfPreviewWorkflowStep();
        WorkflowModel model = wrapStepInModel(step);

        List<String> conditions = new ArrayList<>();
        conditions.add("jcr:content/jcr:mimeType=video/*"); // No dot
        conditions.add("jcr:content/jcr:mimeType==image/(.*)"); // Double equals, includes parentheses
        conditions.add("jcr:content/jcr:mimeType==[audio/mpeg,video/(.*)]"); //Multi-value
        WorkflowLauncher launcher = new WorkflowLauncher();
        launcher.setConditions(conditions);

        Workflow workflow = new Workflow();
        workflow.setWorkflowModel(model);
        workflow.addLauncher(launcher);

        WorkflowProject project = wrapWorkflowInProject(workflow);

        creator.createProfiles(project);

        RenditionConfigMatcher renMatcher = new RenditionConfigMatcher();
        renMatcher.expectIncludedMimetype("audio/mpeg");
        renMatcher.expectIncludedMimetype("video/.*");
        renMatcher.expectIncludedMimetype("image/.*");

        ProcessingProfileMatcher profMatcher = new ProcessingProfileMatcher();
        profMatcher.expectRendition(renMatcher);

        verify(ppDao).addProfile(argThat(profMatcher));
    }

    @Test
    public void testMultipleMimeTypesExtracted() throws ProjectCreationException {
        WorkflowStep step = getPdfPreviewWorkflowStep();
        WorkflowModel model = wrapStepInModel(step);

        List<String> conditions = new ArrayList<>();
        conditions.add("jcr:content/jcr:mimeType=[audio/mpeg, video/.*]"); // No dot
        WorkflowLauncher launcher = new WorkflowLauncher();
        launcher.setConditions(conditions);

        Workflow workflow = new Workflow();
        workflow.setWorkflowModel(model);
        workflow.addLauncher(launcher);

        WorkflowProject project = wrapWorkflowInProject(workflow);

        creator.createProfiles(project);

        RenditionConfigMatcher renMatcher = new RenditionConfigMatcher();
        renMatcher.expectIncludedMimetype("audio/mpeg");
        renMatcher.expectIncludedMimetype("video/.*");

        ProcessingProfileMatcher profMatcher = new ProcessingProfileMatcher();
        profMatcher.expectRendition(renMatcher);

        verify(ppDao).addProfile(argThat(profMatcher));
    }

    @Test
    public void testLauncherConditionsAdditivelyMerged() throws ProjectCreationException {
        WorkflowStep step = getPdfPreviewWorkflowStep();
        WorkflowModel model = wrapStepInModel(step);

        //The first launcher states to not process videos, but it actually means process everything _but_ videos
        List<String> conditions = new ArrayList<>();
        conditions.add("jcr:content/jcr:mimeType!=video/.*");
        WorkflowLauncher launcher = new WorkflowLauncher();
        launcher.setConditions(conditions);

        //The second launchers says to process videos specifically - in this case, adding these two conditions means we should process everything
        List<String> conditions2 = new ArrayList<>();
        conditions2.add("jcr:content/jcr:mimeType==video/.*");
        WorkflowLauncher launcher2 = new WorkflowLauncher();
        launcher2.setConditions(conditions2);

        Workflow workflow = new Workflow();
        workflow.setWorkflowModel(model);
        workflow.addLauncher(launcher);
        workflow.addLauncher(launcher2);

        WorkflowProject project = wrapWorkflowInProject(workflow);

        creator.createProfiles(project);

        RenditionConfigMatcher renMatcher = new RenditionConfigMatcher();
        renMatcher.expectIncludedMimetype("video/.*");

        ProcessingProfileMatcher profMatcher = new ProcessingProfileMatcher();
        profMatcher.expectRendition(renMatcher);

        verify(ppDao).addProfile(argThat(profMatcher));
    }

    @Test
    public void testLauncherProcessConditionsSubtractivelyMerged() throws ProjectCreationException {
        List<String> conditions = new ArrayList<>();
        conditions.add("jcr:content/jcr:mimeType==image/.*"); // Process all images
        conditions.add("jcr:content/jcr:mimeType!=video/.*"); // Skip videos
        WorkflowLauncher launcher = new WorkflowLauncher();
        launcher.setConditions(conditions);

        WorkflowStep step = getWebEnabledWorkflowStep();
        step.getMetadata().put("KEEP_FORMAT_LIST", "image/jpeg"); // Process all images
        step.getMetadata().put("SKIP", "image/gif"); // Skip GIFs
        WorkflowModel model = wrapStepInModel(step);

        Workflow workflow = new Workflow();
        workflow.setWorkflowModel(model);
        workflow.addLauncher(launcher);

        WorkflowProject project = wrapWorkflowInProject(workflow);

        creator.createProfiles(project);

        //Replace the includes, append the excludes
        RenditionConfigMatcher renMatcher = new RenditionConfigMatcher();
        renMatcher.expectIncludedMimetype("image/jpeg");
        renMatcher.expectExcludedMimetype("video/.*");
        renMatcher.expectExcludedMimetype("image/gif");
        renMatcher.expectNonIncludedMimetype("image/.*");

        ProcessingProfileMatcher profMatcher = new ProcessingProfileMatcher();
        profMatcher.expectRendition(renMatcher);
        profMatcher.expectNumRenditions(1);

        verify(ppDao).addProfile(argThat(profMatcher));
    }

    @Test
    public void testSingleProfileCreatedPerWorkflow() throws ProjectCreationException {
        WorkflowStep step1 = getPdfPreviewWorkflowStep();
        WorkflowModel model = wrapStepInModel(step1);

        WorkflowStep step2 = getFFMpegWorkflowStep();
        model.getWorkflowSteps().add(step2);

        Workflow workflow = new Workflow();
        workflow.setWorkflowModel(model);

        WorkflowProject project = wrapWorkflowInProject(workflow);

        creator.createProfiles(project);

        verify(ppDao, times(1)).addProfile(any());
    }

    @Test
    public void testNullWorkflowModelSkipped() throws ProjectCreationException {
        Workflow workflow = new Workflow();
        WorkflowProject project = wrapWorkflowInProject(workflow);
        creator.createProfiles(project);
        verify(ppDao, never()).addProfile(any());
    }

    @Test
    public void testOotbRenditionsSkipped() throws ProjectCreationException {
        //Configuration matches one of the OOTB renditions
        Map<String, String> processMetadata = new HashMap<>();
        processMetadata.put("WIDTH", "319");
        processMetadata.put("HEIGHT", "319");
        processMetadata.put("MIME_TYPE", "image/png"); //output mimetype

        WorkflowStep step = new WorkflowStep();
        step.setProcess("com.day.cq.dam.core.process.CreateWebEnabledImageProcess");
        step.setMetadata(processMetadata);
        WorkflowModel model = wrapStepInModel(step);

        Workflow workflow = new Workflow();
        workflow.setWorkflowModel(model);

        WorkflowProject project = wrapWorkflowInProject(workflow);

        creator.createProfiles(project);

        verify(ppDao, never()).addProfile(any());
    }

    @Test
    public void testCustomPropertiesAreNotConsideredOotb() throws ProjectCreationException {
        //Configuration matches one of the OOTB renditions, except...
        Map<String, String> processMetadata = new HashMap<>();
        processMetadata.put("WIDTH", "319");
        processMetadata.put("HEIGHT", "319");
        processMetadata.put("MIME_TYPE", "image/jpeg"); //OOTB is PNG

        WorkflowStep step = new WorkflowStep();
        step.setProcess("com.day.cq.dam.core.process.CreateWebEnabledImageProcess");
        step.setMetadata(processMetadata);
        WorkflowModel model = wrapStepInModel(step);

        Workflow workflow = new Workflow();
        workflow.setWorkflowModel(model);

        WorkflowProject project = wrapWorkflowInProject(workflow);

        creator.createProfiles(project);

        verify(ppDao, times(1)).addProfile(any());
    }

    private WorkflowStep getPdfPreviewWorkflowStep() {
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put("MAX_HEIGHT", "2048");
        metadataMap.put("MAX_WIDTH", "2048");

        WorkflowStep step = new WorkflowStep();
        step.setProcess("com.day.cq.dam.core.process.CreatePdfPreviewProcess");
        step.setMetadata(metadataMap);
        return step;
    }

    private WorkflowStep getFFMpegWorkflowStep() {
        Map<String, String> processMetadata = new HashMap<>();
        processMetadata.put("CONFIGS", "[100:100:false, 200:200:false]"); //Thumbnail sizes
        processMetadata.put("COUNT", "2"); //Number of thumbnails to make
        processMetadata.put("INDEX", "1"); //Which thumbnail to use for the video
        processMetadata.put("START", "30"); //Seconds to offset for the first thumbnail

        WorkflowStep step = new WorkflowStep();
        step.setProcess("com.day.cq.dam.video.FFMpegThumbnailProcess");
        step.setMetadata(processMetadata);
        return step;
    }

    private WorkflowStep getWebEnabledWorkflowStep() {
        Map<String, String> processMetadata = new HashMap<>();
        processMetadata.put("WIDTH", "150");
        processMetadata.put("HEIGHT", "100");
        processMetadata.put("QUALITY", "90");
        processMetadata.put("MIME_TYPE", "image/png"); //output mimetype

        WorkflowStep step = new WorkflowStep();
        step.setProcess("com.day.cq.dam.core.process.CreateWebEnabledImageProcess");
        step.setMetadata(processMetadata);

        return step;
    }

    private WorkflowProject wrapStepInProject(WorkflowStep step) {
        WorkflowModel model = wrapStepInModel(step);

        Workflow workflow = new Workflow();
        workflow.setWorkflowModel(model);

        return wrapWorkflowInProject(workflow);
    }

    private WorkflowModel wrapStepInModel(WorkflowStep step) {
        List<WorkflowStep> workflowSteps = new ArrayList<>();
        workflowSteps.add(step);

        WorkflowModel model = new WorkflowModel();
        model.setName(WORKFLOW_NAME);
        model.setWorkflowSteps(workflowSteps);
        return model;
    }

    private WorkflowProject wrapWorkflowInProject(Workflow workflow) {
        WorkflowLauncher launcher = new WorkflowLauncher();
        workflow.addLauncher(launcher);

        List<Workflow> workflows = new ArrayList<>();
        workflows.add(workflow);

        WorkflowProject project = new WorkflowProject();
        project.setWorkflows(workflows);
        return project;
    }

}

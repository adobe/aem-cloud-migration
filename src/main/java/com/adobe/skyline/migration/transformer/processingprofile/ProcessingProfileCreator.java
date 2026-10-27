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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.dao.FilterFileDAO;
import com.adobe.skyline.migration.dao.MavenProjectDAO;
import com.adobe.skyline.migration.dao.ProcessingProfileDAO;
import com.adobe.skyline.migration.exception.ProjectCreationException;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.model.ProcessingProfile;
import com.adobe.skyline.migration.model.RenditionConfig;
import com.adobe.skyline.migration.model.VideoProfileConfig;
import com.adobe.skyline.migration.model.workflow.Workflow;
import com.adobe.skyline.migration.model.workflow.WorkflowModel;
import com.adobe.skyline.migration.model.workflow.WorkflowProject;
import com.adobe.skyline.migration.model.workflow.WorkflowStep;

/**
 * The ProcessingProfileCreator leverages ProfileMappers to generate ProcessingProfiles for any supported WorkflowSteps.
 * It also contains logic to merge the mimetype includes and excludes that are found on the workflow launchers and step
 * definitions.
 */
public class ProcessingProfileCreator {

    private ProfileMapperFactory mapperFactory;
    private ProcessingProfileDAO ppDao;
    private FilterFileDAO filterFileDAO;
    private ChangeTrackingService changeTracker;
    private MavenProjectDAO mavenProjectDAO;

    public ProcessingProfileCreator(ProfileMapperFactory mapperFactory, ProcessingProfileDAO ppDao,
                                    FilterFileDAO filterFileDAO, ChangeTrackingService changeTracker, MavenProjectDAO mavenProjectDAO) {
        this.mapperFactory = mapperFactory;
        this.ppDao = ppDao;
        this.filterFileDAO = filterFileDAO;
        this.changeTracker = changeTracker;
        this.mavenProjectDAO = mavenProjectDAO;
    }

    public void createProfiles(WorkflowProject workflowProject) throws ProjectCreationException {
        boolean projectExists = mavenProjectDAO.projectExists(MigrationConstants.MIGRATION_PROJECT_CONTENT);

        for (Workflow workflow : workflowProject.getWorkflows()) {
            if (workflow.getWorkflowModel() != null) {

                ProcessingProfile profile = createProfile(workflow);

                if (profile.getRenditions().size() > 0) {
                    MimeTypeMerger merger = new MimeTypeMerger(workflow);
                    merger.mergeRenditionMimetypes(profile);

                    //Only create the project if we will actually be writing content to it
                    if (!projectExists) {
                        mavenProjectDAO.createProject(MigrationConstants.MIGRATION_PROJECT_CONTENT);
                        projectExists = true;
                    }

                    changeTracker.trackCreatedProcessingProfile(profile);
                    // A filter on the processing folder would delete the profiles the customer created in AEM, so the
                    // filter covers only the page content and the migrated profiles.
                    filterFileDAO.addPath(MigrationConstants.PROCESSING_PROFILE_JCR_PATH + "/" + MigrationConstants.JCR_CONTENT);
                    filterFileDAO.addPath(ppDao.addProfile(profile));
                }
            }
        }
    }

    private ProcessingProfile createProfile(Workflow workflow) {
        ProcessingProfile profile = new ProcessingProfile();
        profile.setName("Migrated from " + workflow.getWorkflowModel().getName());

        WorkflowModel model = workflow.getWorkflowModel();

        for (WorkflowStep step : model.getWorkflowSteps()) {
            ProfileMapper mapper = mapperFactory.getMapper(step);

            if (mapper != null) {
                List<RenditionConfig> renditions = mapper.mapToRenditions(model, step);

                //Merge the mimetype includes and excludes from the launcher into those defined by the rendition configuration
                for (RenditionConfig rendition : renditions) {
                    if (!isOotbRendition(rendition)) {
                        addRendition(profile, rendition, model);
                    }
                }
            }
        }

        return profile;
    }

    /**
     * Adds a rendition unless the profile already has one with the same file name. Identical definitions are merged by
     * combining their included mimetypes; a different definition is reported, and the first one wins.
     */
    private void addRendition(ProcessingProfile profile, RenditionConfig rendition, WorkflowModel model) {
        for (RenditionConfig existing : profile.getRenditions()) {
            if (Objects.equals(existing.getFileName(), rendition.getFileName())) {
                if (isSameDefinition(existing, rendition)) {
                    if (existing.getIncludeMimeTypes().isEmpty() || rendition.getIncludeMimeTypes().isEmpty()) {
                        existing.setIncludeMimeTypes(new HashSet<>());
                    } else {
                        Set<String> includes = new HashSet<>(existing.getIncludeMimeTypes());
                        includes.addAll(rendition.getIncludeMimeTypes());
                        existing.setIncludeMimeTypes(includes);
                    }
                } else {
                    changeTracker.trackFailedMappings("Rendition " + rendition.getFileName() + " in " + model.getName(),
                            "Another step of the workflow already creates this rendition with different settings. Only the first definition was migrated.");
                }
                return;
            }
        }

        profile.addRendition(rendition);
    }

    private boolean isSameDefinition(RenditionConfig a, RenditionConfig b) {
        boolean same = a.getClass().equals(b.getClass()) &&
                Objects.equals(a.getFormat(), b.getFormat()) &&
                a.getWidth() == b.getWidth() &&
                a.getHeight() == b.getHeight() &&
                a.getQuality() == b.getQuality() &&
                a.getExcludeMimeTypes().equals(b.getExcludeMimeTypes());

        if (same && a instanceof VideoProfileConfig) {
            VideoProfileConfig videoA = (VideoProfileConfig) a;
            VideoProfileConfig videoB = (VideoProfileConfig) b;
            same = videoA.getBitRate() == videoB.getBitRate() && Objects.equals(videoA.getCodec(), videoB.getCodec());
        }

        return same;
    }

    private boolean isOotbRendition(RenditionConfig rendition) {
        return ((rendition.getWidth() == 319 && rendition.getHeight() == 319 && rendition.getFormat().equals(MigrationConstants.PNG_EXTENSION)) ||
                (rendition.getWidth() == 140 && rendition.getHeight() == 100 && rendition.getFormat().equals(MigrationConstants.PNG_EXTENSION)) ||
                (rendition.getWidth() == 48 && rendition.getHeight() == 48 && rendition.getFormat().equals(MigrationConstants.PNG_EXTENSION)) ||
                (rendition.getWidth() == 1280 && rendition.getHeight() == 1280 && rendition.getFormat().equals(MigrationConstants.JPEG_EXTENSION) && rendition.getQuality() == 90));
    }

}

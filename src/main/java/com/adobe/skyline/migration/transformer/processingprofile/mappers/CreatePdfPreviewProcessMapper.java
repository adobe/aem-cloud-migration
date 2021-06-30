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

package com.adobe.skyline.migration.transformer.processingprofile.mappers;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.model.ProcessingProfile;
import com.adobe.skyline.migration.model.RenditionConfig;
import com.adobe.skyline.migration.model.workflow.WorkflowModel;
import com.adobe.skyline.migration.model.workflow.WorkflowStep;
import com.adobe.skyline.migration.transformer.processingprofile.ProfileMapper;
import com.adobe.skyline.migration.util.StringUtil;

import java.util.*;

public class CreatePdfPreviewProcessMapper implements ProfileMapper {

    private static final String MAX_HEIGHT = "MAX_HEIGHT";
    private static final String MAX_WIDTH = "MAX_WIDTH";
    private static final String MIME_TYPES = "MIME_TYPES";
    private static final String PROCESS_ID = "com.day.cq.dam.core.process.CreatePdfPreviewProcess";
    private static final String RENDITION_PREFIX = "cqdam";
    private static final String NODE_NAME = "preview";
    private static final String EXTENSION = MigrationConstants.PNG_EXTENSION;

    @Override
    public String[] getProcessIds() {
        return new String[] {PROCESS_ID};
    }

    @Override
    public List<RenditionConfig> mapToRenditions(ProcessingProfile processingProfile, WorkflowModel model, WorkflowStep step) {
        List<RenditionConfig> renditions = new ArrayList<>();

        if (!isDeletePreviewProcessPresent(model)) {
            Map<String, String> metadata = step.getMetadata();

            RenditionConfig rendition = new RenditionConfig();

            if (metadata.containsKey(MAX_HEIGHT)) {
                rendition.setHeight(Integer.parseInt(metadata.get(MAX_HEIGHT)));
            }

            if (metadata.containsKey(MAX_WIDTH)) {
                rendition.setWidth(Integer.parseInt(metadata.get(MAX_WIDTH)));
            }

            if (metadata.containsKey(MIME_TYPES)) {
                Set<String> mimeTypes = new HashSet<>(StringUtil.getListFromString(metadata.get(MIME_TYPES)));
                rendition.setIncludeMimeTypes(mimeTypes);
            }

            rendition.setNodeName(NODE_NAME);
            rendition.setFileName(RENDITION_PREFIX + "." + NODE_NAME + "." + EXTENSION);
            rendition.setFormat(EXTENSION);

            renditions.add(rendition);
        }

        return renditions;
    }

    private boolean isDeletePreviewProcessPresent(WorkflowModel model) {
        for (WorkflowStep step : model.getWorkflowSteps()) {
            if (step.getProcess().equals(MigrationConstants.DELETE_PREVIEW_PROCESS)) {
                return true;
            }
        }

        return false;
    }
}

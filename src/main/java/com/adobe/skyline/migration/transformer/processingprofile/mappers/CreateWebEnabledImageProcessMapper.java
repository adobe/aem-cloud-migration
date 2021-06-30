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

import java.util.*;

public class CreateWebEnabledImageProcessMapper implements ProfileMapper {

    private static final String WIDTH = "WIDTH";
    private static final String HEIGHT = "HEIGHT";
    private static final String QUALITY = "QUALITY";
    private static final String MIME_TYPE = "MIME_TYPE"; //output mimetype
    private static final String KEEP_FORMAT_LIST = "KEEP_FORMAT_LIST"; //mimetypes to process
    private static final String SKIP = "SKIP"; //mimetypes to skip
    private static final String PROCESS_ID = "com.day.cq.dam.core.process.CreateWebEnabledImageProcess";
    private static final String RENDITION_PREFIX = "cq5dam";
    private static final String NODE_NAME = "web";

    @Override
    public String[] getProcessIds() {
        return new String[] {PROCESS_ID};
    }

    @Override
    public List<RenditionConfig> mapToRenditions(ProcessingProfile processingProfile, WorkflowModel model, WorkflowStep step) {
        Map<String, String> metadata = step.getMetadata();

        RenditionConfig rendition = new RenditionConfig();

        int height = 0;
        if (metadata.containsKey(HEIGHT)) {
            height = Integer.parseInt(metadata.get(HEIGHT));
            rendition.setHeight(height);
        }

        int width = 0;
        if (metadata.containsKey(WIDTH)) {
            width = Integer.parseInt(metadata.get(WIDTH));
            rendition.setWidth(width);
        }

        if (metadata.containsKey(QUALITY)) {
            rendition.setQuality(Integer.parseInt(metadata.get(QUALITY)));
        }

        String extension = MigrationConstants.PNG_EXTENSION;
        if (metadata.containsKey(MIME_TYPE)) {
            extension = metadata.get(MIME_TYPE).split("/")[1];
        }
        rendition.setFormat(extension);

        if (metadata.containsKey(KEEP_FORMAT_LIST)) {
            String[] mimeTypesToInclude = metadata.get(KEEP_FORMAT_LIST).split(",");

            Set<String> includeMimeTypes = new HashSet<>();
            for (String entry : mimeTypesToInclude) {
                includeMimeTypes.add(entry.trim());
            }

            rendition.setIncludeMimeTypes(includeMimeTypes);
        }

        if (metadata.containsKey(SKIP)) {
            String[] mimeTypesToExclude = metadata.get(SKIP).split(",");

            Set<String> excludeMimeTypes = new HashSet<>();
            for (String entry : mimeTypesToExclude) {
                excludeMimeTypes.add(entry.trim());
            }

            rendition.setExcludeMimeTypes(excludeMimeTypes);
        }

        rendition.setNodeName(NODE_NAME);
        rendition.setFileName(RENDITION_PREFIX + "." + NODE_NAME + "." + width + "." + height + "." + extension);

        return Collections.singletonList(rendition);
    }
}

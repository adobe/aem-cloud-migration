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

import com.adobe.skyline.migration.model.ProcessingProfile;
import com.adobe.skyline.migration.model.RenditionConfig;
import com.adobe.skyline.migration.model.workflow.WorkflowModel;
import com.adobe.skyline.migration.model.workflow.WorkflowStep;
import com.adobe.skyline.migration.transformer.processingprofile.ProfileMapper;
import com.adobe.skyline.migration.util.StringUtil;

import java.util.*;

public class FFMpegThumbnailProcessMapper implements ProfileMapper {

    private static final String PROCESS_ID = "com.day.cq.dam.video.FFMpegThumbnailProcess";
    private static final String NODE_NAME = "thumbnail";
    private static final String RENDITION_PREFIX = "cq5dam";
    private static final String PROCESS_ARGS_PROP = "PROCESS_ARGS";
    private static final String CONFIGS_PROP = "CONFIGS";

    @Override
    public String[] getProcessIds() {
        return new String[] {PROCESS_ID};
    }

    @Override
    public List<RenditionConfig> mapToRenditions(ProcessingProfile processingProfile, WorkflowModel model, WorkflowStep step) {
        List<RenditionConfig> renditions = new ArrayList<>();

        Map<String, String> metadata = step.getMetadata();

        if (metadata.containsKey(PROCESS_ARGS_PROP)) {
            //old-style
            String[] configs = metadata.get(PROCESS_ARGS_PROP).split(",");

            for (String config : configs) {
                config = config.trim();

                if (config.startsWith("[")) {
                    config = StringUtil.removeBrackets(config);
                    renditions.add(getRendition(config));
                }
            }
        } else {
            //new-style
            List<String> configs = StringUtil.getListFromString(metadata.get(CONFIGS_PROP));

            for (String config : configs) {
                renditions.add(getRendition(config));
            }
        }

        return renditions;
    }

    private RenditionConfig getRendition(String config) {
        String[] dimensions = config.split(":");
        int width = Integer.parseInt(dimensions[0].trim());
        int height = Integer.parseInt(dimensions[1].trim());

        Set<String> mimeTypes = new HashSet<>();
        mimeTypes.add("video/.*");

        return RenditionBuilder.buildRendition(width, height, NODE_NAME, RENDITION_PREFIX, mimeTypes, null);
    }
}

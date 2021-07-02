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

public class ThumbnailProcessMapper implements ProfileMapper {

    private static final String PROCESS_ID = "com.day.cq.dam.core.process.ThumbnailProcess";
    private static final String IMPL_PROCESS_ID = "com.day.cq.dam.core.impl.process.ThumbnailProcess";

    private static final String PROCESS_ARGS_PROP = "PROCESS_ARGS";
    private static final String CONFIGS_PROP = "CONFIGS";
    private static final String SKIP_MIME_TYPES_PROP = "SKIP_MIME_TYPES";
    private static final String HEIGHT_PROP = "HEIGHT";
    private static final String WIDTH_PROP = "WIDTH";
    private static final String KEEP_FORMAT_LIST_PROP = "KEEP_FORMAT_LIST";
    private static final String SKIP_PROP = "SKIP";
    private static final String MIME_TYPE_PROP = "MIME_TYPE";
    private static final String QUALITY_PROP = "QUALITY";
    private static final String FPO_CREATION_ENABLED_PROP = "FPO_CREATION_ENABLED";
    private static final String TRUE = "true";
    private static final String CREATE_FPO_MIMETYPES_PROP = "CREATE_FPO_MIMETYPES";
    private static final String FPO_QUALITY_PROP = "FPO_QUALITY";

    private static final String RENDITION_PREFIX = "cq5dam";
    private static final String THUMB_NODE_NAME = "thumbnail";
    private static final String FPO_NODE_NAME = "fpo";

    @Override
    public String[] getProcessIds() {
        return new String[] {PROCESS_ID, IMPL_PROCESS_ID};
    }

    @Override
    public List<RenditionConfig> mapToRenditions(WorkflowModel model, WorkflowStep step) {
        List<RenditionConfig> renditions = new ArrayList<>();

        Map<String, String> metadata = step.getMetadata();

        if (metadata.containsKey(PROCESS_ARGS_PROP)) {
            //Old-style configuration set by a single property
            String processArgs = metadata.get(PROCESS_ARGS_PROP);
            String[] thumbnailConfigs = processArgs.split(",");
            List<String> thumbnailSizes = new ArrayList<>();
            for (String thumb:thumbnailConfigs) {
                String dimensions = StringUtil.removeBrackets(thumb.trim());
                thumbnailSizes.add(dimensions);
            }
            List<RenditionConfig> thumbRenditions = generateThumbnailRenditions(thumbnailSizes, null);
            renditions.addAll(thumbRenditions);
        } else {
            //New-style configuration with multiple configurations

            //Thumbnails
            String configsProp = metadata.get(CONFIGS_PROP);

            if (configsProp.startsWith("[")) {
                configsProp = StringUtil.removeBrackets(configsProp);
            }

            String[] thumbnailConfigs = configsProp.split(",");
            List<String> thumbnailSizes = new ArrayList<>();
            for (String thumb:thumbnailConfigs) {
                thumbnailSizes.add(thumb.trim());
            }

            String thumbSkipMimeTypes = metadata.get(SKIP_MIME_TYPES_PROP);
            List<RenditionConfig> thumbRenditions = generateThumbnailRenditions(thumbnailSizes, thumbSkipMimeTypes);
            renditions.addAll(thumbRenditions);

            //Web-enabled Rendition
            String webHeight = metadata.get(HEIGHT_PROP);
            String webWidth = metadata.get(WIDTH_PROP);
            String webMimeTypes = metadata.get(KEEP_FORMAT_LIST_PROP);
            String webSkipped = metadata.get(SKIP_PROP);
            String webType = metadata.get(MIME_TYPE_PROP);
            String webQual = metadata.get(QUALITY_PROP);
            RenditionConfig webRendition = generateWebEnabledRendition(webHeight, webWidth, webMimeTypes, webSkipped, webType, webQual);
            renditions.add(webRendition);

            //FPO Rendition
            if (metadata.containsKey(FPO_CREATION_ENABLED_PROP) && metadata.get(FPO_CREATION_ENABLED_PROP).equals(TRUE)) {
                String fpoMimetypes = metadata.get(CREATE_FPO_MIMETYPES_PROP);
                String fpoQual = metadata.get(FPO_QUALITY_PROP);
                RenditionConfig fpoRendition = generateFpoRendition(fpoMimetypes, fpoQual);
                renditions.add(fpoRendition);
            }
        }

        return renditions;
    }

    private List<RenditionConfig> generateThumbnailRenditions(List<String> thumbnailSizes, String thumbSkipMimeTypes) {

        List<RenditionConfig> thumbnails = new ArrayList<>();

        Set<String> excludedMimetypes = extractExcludedMimetypes(thumbSkipMimeTypes);

        for (String size:thumbnailSizes) {
            String[] params = size.split(":");
            int width = Integer.parseInt(params[0]);
            int height = Integer.parseInt(params[1]);

            RenditionConfig rendition = RenditionBuilder.buildRendition(width, height, THUMB_NODE_NAME, RENDITION_PREFIX, null, excludedMimetypes);

            thumbnails.add(rendition);
        }

        return thumbnails;
    }

    private RenditionConfig generateWebEnabledRendition(String webHeight, String webWidth, String webMimeTypes, String webSkipped, String webType, String webQual) {

        RenditionConfig rendition = new RenditionConfig();

        if (webMimeTypes != null) {
            String[] includedTypes = webMimeTypes.split(",");
            Set<String> includedMimeTypes = new HashSet<>();
            for (String type:includedTypes) {
                includedMimeTypes.add(type.trim());
            }
            rendition.setIncludeMimeTypes(includedMimeTypes);
        }

        Set<String> excludeMimeTypes = extractExcludedMimetypes(webSkipped);

        if (excludeMimeTypes.size() > 0) {
            rendition.setExcludeMimeTypes(excludeMimeTypes);
        }

        int width = 0;
        if (webWidth != null) {
            width = Integer.parseInt(webWidth);
            rendition.setWidth(width);
        }

        int height = 0;
        if (webHeight != null) {
            height = Integer.parseInt(webHeight);
            rendition.setHeight(height);
        }

        if (webQual != null) {
            int qual = Integer.parseInt(webQual);
            rendition.setQuality(qual);
        }

        String format = MigrationConstants.PNG_EXTENSION;
        if (webType != null) {
            format = webType.split("/")[1];
        }
        rendition.setFormat(format);

        rendition.setNodeName(THUMB_NODE_NAME);
        rendition.setFileName(RENDITION_PREFIX + "." + THUMB_NODE_NAME + "." + width + "." + height + "." + format);

        return rendition;
    }

    private Set<String> extractExcludedMimetypes(String mimeTypes) {
        Set<String> excludedMimeTypes = new HashSet<>();

        if (mimeTypes != null) {
            List<String> excludedTypes = StringUtil.getListFromString(mimeTypes);
            for (String type:excludedTypes) {
                if (type.startsWith("skip:")) {
                    type = type.substring(5);
                }
                excludedMimeTypes.add(type.trim());
            }
        }

        return excludedMimeTypes;
    }

    private RenditionConfig generateFpoRendition(String fpoMimetypes, String fpoQual) {
        RenditionConfig rendition = new RenditionConfig();

        String[] includedTypes = fpoMimetypes.split(",");
        Set<String> includedMimeTypes = new HashSet<>();
        for (String type:includedTypes) {
            includedMimeTypes.add(type.trim());
        }
        rendition.setIncludeMimeTypes(includedMimeTypes);

        rendition.setFormat(MigrationConstants.JPEG_EXTENSION);
        rendition.setQuality(Integer.parseInt(fpoQual));
        rendition.setNodeName(FPO_NODE_NAME);
        rendition.setFileName(RENDITION_PREFIX + "." + FPO_NODE_NAME + "." + MigrationConstants.JPEG_EXTENSION);

        return rendition;
    }
}

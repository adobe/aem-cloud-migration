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
import com.adobe.skyline.migration.exception.MigrationRuntimeException;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.model.ProcessingProfile;
import com.adobe.skyline.migration.model.RenditionConfig;
import com.adobe.skyline.migration.model.VideoProfileConfig;
import com.adobe.skyline.migration.model.workflow.UpdateAssetWorkflowModel;
import com.adobe.skyline.migration.model.workflow.WorkflowModel;
import com.adobe.skyline.migration.model.workflow.WorkflowStep;
import com.adobe.skyline.migration.transformer.processingprofile.ProfileMapper;
import com.adobe.skyline.migration.util.Logger;
import com.adobe.skyline.migration.util.StringUtil;
import com.adobe.skyline.migration.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.adobe.skyline.migration.MigrationConstants.CONTENT_XML;
import static com.adobe.skyline.migration.MigrationConstants.SUPPORTED_CODEC;
import static com.adobe.skyline.migration.MigrationConstants.SUPPORTED_FORMAT;

public class FFMpegTranscodeProcessMapper implements ProfileMapper {

    private static final String PROCESS_ID = "com.day.cq.dam.video.FFMpegTranscodeProcess";
    private static final String NODE_NAME = "video";
    private static final String RENDITION_PREFIX = "cq5dam";
    private static final String PROCESS_ARGS_PROP = "PROCESS_ARGS";
    private static final String CONFIGS_PROP = "CONFIGS";
    private ChangeTrackingService changeTrackingService;

    public FFMpegTranscodeProcessMapper(ChangeTrackingService changeTrackingService) {
        this.changeTrackingService = changeTrackingService;
    }

    @Override
    public String[] getProcessIds() {
        return new String[] {PROCESS_ID};
    }

    @Override
    public List<RenditionConfig> mapToRenditions(ProcessingProfile processingProfile, WorkflowModel model, WorkflowStep step) {
        List<RenditionConfig> renditions = new ArrayList<>();
        Map<String, String> metadata = step.getMetadata();
        String videoProfilePath = "";
        try {
            UpdateAssetWorkflowModel updateAssetWorkflowModel = (UpdateAssetWorkflowModel) model;
            videoProfilePath = updateAssetWorkflowModel.getVideoProfilePath();
            if (metadata.containsKey(PROCESS_ARGS_PROP)) {
                //old-style
                String[] configs = metadata.get(PROCESS_ARGS_PROP).split(",");

                for (String config : configs) {
                    config = config.trim();

                    if (config.startsWith("[")) {
                        config = StringUtil.removeBrackets(config);
                    }
                    RenditionConfig renditionConfig = (getRendition(processingProfile, config, videoProfilePath));
                    if (renditionConfig != null) {
                        renditions.add(renditionConfig);
                    }
                }
            } else {
                //new-style
                List<String> configs = StringUtil.getListFromString(metadata.get(CONFIGS_PROP));

                for (String config : configs) {
                    RenditionConfig renditionConfig = (getRendition(processingProfile, config, videoProfilePath));
                    if (renditionConfig != null) {
                        renditions.add(renditionConfig);
                    }
                }
            }
        } catch (Exception e) {
            Logger.ERROR("Unable to parse details for FFMpegTranscodeProcess, make sure that video config file at " + videoProfilePath + " s present and in correct format " + e.getMessage());
            return new ArrayList<>();
        }
        return renditions;
    }

    private RenditionConfig getRendition(ProcessingProfile processingProfile, String config, String videoProfilePath) throws MigrationRuntimeException{
        String[] tokens = config.split(":");
        String profile = tokens[1].trim();
        return getRenditionFromProfileNode(processingProfile, profile, videoProfilePath);
    }

    private RenditionConfig getRenditionFromProfileNode(ProcessingProfile processingProfile, String profile, String videoProfilePath) throws MigrationRuntimeException {
        File profileFile = new File(videoProfilePath + File.separator + profile + File.separator + CONTENT_XML);
        try {
            Document profileXml = XmlUtil.loadXml(profileFile);
            Node transcodingDetailsNode = profileXml.getElementsByTagName(MigrationConstants.JCR_CONTENT).item(0);
            NamedNodeMap transcodingDetailsMap = transcodingDetailsNode.getAttributes();
            Integer width = transcodingDetailsMap.getNamedItem("width") != null ? Integer.parseInt(transcodingDetailsMap.getNamedItem("width").getTextContent()) : null;
            Integer height = transcodingDetailsMap.getNamedItem("height") != null ? Integer.parseInt(transcodingDetailsMap.getNamedItem("height").getTextContent()) : null;
            String codec = transcodingDetailsMap.getNamedItem("videoCodec") != null ? transcodingDetailsMap.getNamedItem("videoCodec").getTextContent() : null;
            Integer bitRate = transcodingDetailsMap.getNamedItem("videoBitrate") != null ? Integer.parseInt(transcodingDetailsMap.getNamedItem("videoBitrate").getTextContent()) : null;
            String format = transcodingDetailsMap.getNamedItem("extension") != null ? transcodingDetailsMap.getNamedItem("extension").getTextContent() : null;
            if (!SUPPORTED_CODEC.equals(codec) || !SUPPORTED_FORMAT.equals(format)) {
                Logger.WARN("The only supported codec & format on AEMaaCS are " + SUPPORTED_CODEC + " " + SUPPORTED_FORMAT + " respectively, the processing profile will not be created for " + codec + " " + format);
                changeTrackingService.trackFailedProcessingProfile(processingProfile);
                return null;
            }
            Set<String> excludedMimeTypes = new HashSet<>();
            excludedMimeTypes.add("image/.*");
            excludedMimeTypes.add("application/.*");
            Set<String> mimeTypes = new HashSet<>();
            mimeTypes.add("video/.*");
            RenditionConfig renditionConfig = RenditionBuilder.buildVideoRendition(width, height, NODE_NAME, RENDITION_PREFIX, mimeTypes, excludedMimeTypes);
            if (format != null) {
                renditionConfig.setFormat(format);
            }
            VideoProfileConfig videoProfileConfig = (VideoProfileConfig) renditionConfig;
            if (bitRate != null) {
                videoProfileConfig.setBitRate(bitRate);
            }
            if (codec != null) {
                videoProfileConfig.setCodec(codec);
            }
            return renditionConfig;
        } catch (Exception e) {
            throw new MigrationRuntimeException(e);
        }
    }
}

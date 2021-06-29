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

import com.adobe.skyline.migration.model.RenditionConfig;
import com.adobe.skyline.migration.model.VideoProfileConfig;
import com.adobe.skyline.migration.model.workflow.UpdateAssetWorkflowModel;
import com.adobe.skyline.migration.model.workflow.WorkflowStep;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FFMpegTranscodeProcessMapperTest {

    private FFMpegTranscodeProcessMapper mapper;

    @Before
    public void setUp() {
        this.mapper = new FFMpegTranscodeProcessMapper();
    }

    @Test
    public void testMapping() {
        Map<String, String> processMetadata = new HashMap<>();
        processMetadata.put("CONFIGS", "[profile:format_ogg]"); //Thumbnail sizes
        WorkflowStep step = new WorkflowStep();
        step.setMetadata(processMetadata);
        UpdateAssetWorkflowModel workflowModel = new UpdateAssetWorkflowModel();
        workflowModel.setVideoProfilePath(getClass().getClassLoader().getResource("archetype17").getPath() + "/ui.content/src/main/content/jcr_root/conf/global/settings/dam/video");
        List<RenditionConfig> renditions = mapper.mapToRenditions(workflowModel, step);

        validateRenditions(renditions);
    }

    @Test
    public void testMappingProcessArgs() {
        Map<String, String> processMetadata = new HashMap<>();
        processMetadata.put("PROCESS_ARGS", "profile:format_ogg"); //Older-style configuration - included for BC
        WorkflowStep step = new WorkflowStep();
        step.setMetadata(processMetadata);
        UpdateAssetWorkflowModel workflowModel = new UpdateAssetWorkflowModel();
        workflowModel.setVideoProfilePath(getClass().getClassLoader().getResource("archetype17").getPath() + "/ui.content/src/main/content/jcr_root/conf/global/settings/dam/video");
        List<RenditionConfig> renditions = mapper.mapToRenditions(workflowModel, step);

        validateRenditions(renditions);
    }

    private void validateRenditions(List<RenditionConfig> renditions) {
        assertEquals(1, renditions.size());
        RenditionConfig firstRendition = renditions.get(0);
        assertRendition(firstRendition, 320, 240);
    }

    private void assertRendition(RenditionConfig rendition, int width, int height) {
        assertEquals(height, rendition.getHeight());
        assertEquals(width, rendition.getWidth());
        assertTrue(rendition instanceof VideoProfileConfig);
        assertEquals(((VideoProfileConfig)rendition).getBitRate(), 4096);
        assertEquals("ogg", rendition.getFormat());
        assertEquals("video", rendition.getNodeName());
        assertEquals("cq5dam.video." + width + "." + height, rendition.getFileName()); //cq5dam.thumbnail.WIDTH.HEIGHT.EXTENSION

        Set<String> mimeTypes = rendition.getIncludeMimeTypes();
        assertEquals(1, mimeTypes.size());
        assertTrue(mimeTypes.contains("video/.*"));
        assertEquals(2, rendition.getExcludeMimeTypes().size());
        assertTrue(rendition.getExcludeMimeTypes().contains("image/.*"));
        assertTrue(rendition.getExcludeMimeTypes().contains("application/.*"));
    }


}
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
import com.adobe.skyline.migration.model.workflow.WorkflowStep;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class FFMpegThumbnailProcessMapperTest {

    private FFMpegThumbnailProcessMapper mapper;

    @Before
    public void setUp() {
        this.mapper = new FFMpegThumbnailProcessMapper();
    }

    @Test
    public void testMapping() {
        Map<String, String> processMetadata = new HashMap<>();
        processMetadata.put("CONFIGS", "[100:100:false, 200:200:false]"); //Thumbnail sizes
        processMetadata.put("COUNT", "2"); //Number of thumbnails to make
        processMetadata.put("INDEX", "1"); //Which thumbnail to use for the video
        processMetadata.put("START", "30"); //Seconds to offset for the first thumbnail

        WorkflowStep step = new WorkflowStep();
        step.setMetadata(processMetadata);

        List<RenditionConfig> renditions = mapper.mapToRenditions(null, step);

        validateRenditions(renditions);
    }

    @Test
    public void testMappingProcessArgs() {
        Map<String, String> processMetadata = new HashMap<>();
        processMetadata.put("PROCESS_ARGS", "count:2,index:1,[100:100],[200:200]"); //Older-style configuration - included for BC

        WorkflowStep step = new WorkflowStep();
        step.setMetadata(processMetadata);

        List<RenditionConfig> renditions = mapper.mapToRenditions(null, step);

        validateRenditions(renditions);
    }

    private void validateRenditions(List<RenditionConfig> renditions) {
        assertEquals(2, renditions.size());

        RenditionConfig firstRendition = renditions.get(0);
        if (firstRendition.getHeight() == 100) {
            assertRendition(firstRendition, 100, 100);
            assertRendition(renditions.get(1), 200, 200);
        } else {
            assertRendition(firstRendition, 200, 200);
            assertRendition(renditions.get(1), 100, 100);
        }
    }

    private void assertRendition(RenditionConfig rendition, int width, int height) {
        assertEquals(height, rendition.getHeight());
        assertEquals(width, rendition.getWidth());
        assertEquals("png", rendition.getFormat());
        assertEquals("thumbnail", rendition.getNodeName());
        assertEquals("cq5dam.thumbnail." + width + "." + height + ".png", rendition.getFileName()); //cq5dam.thumbnail.WIDTH.HEIGHT.EXTENSION

        Set<String> mimeTypes = rendition.getIncludeMimeTypes();
        assertEquals(1, mimeTypes.size());
        assertTrue(mimeTypes.contains("video/.*"));
    }


}
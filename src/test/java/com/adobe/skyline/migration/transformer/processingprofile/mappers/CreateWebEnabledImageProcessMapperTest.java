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

public class CreateWebEnabledImageProcessMapperTest {

    private CreateWebEnabledImageProcessMapper mapper;

    @Before
    public void setUp() {
        this.mapper = new CreateWebEnabledImageProcessMapper();
    }

    @Test
    public void testMapping() {
        Map<String, String> processMetadata = new HashMap<>();
        processMetadata.put("WIDTH", "150");
        processMetadata.put("HEIGHT", "100");
        processMetadata.put("QUALITY", "90");
        processMetadata.put("MIME_TYPE", "image/png"); //output mimetype
        processMetadata.put("KEEP_FORMAT_LIST", "image/jpeg, image/tiff"); //mimetypes to process
        processMetadata.put("SKIP", "image/gif, image/bmp"); //mimetypes to skip

        WorkflowStep step = new WorkflowStep();
        step.setMetadata(processMetadata);

        List<RenditionConfig> renditions = mapper.mapToRenditions(null, step);

        RenditionConfig rendition = renditions.get(0);
        assertEquals(100, rendition.getHeight());
        assertEquals(150, rendition.getWidth());
        assertEquals("png", rendition.getFormat());
        assertEquals("web", rendition.getNodeName());
        assertEquals("cq5dam.web.150.100.png", rendition.getFileName()); //cq5dam.web.WIDTH.HEIGHT.EXTENSION

        Set<String> mimeTypes = rendition.getIncludeMimeTypes();
        assertEquals(2, mimeTypes.size());
        assertTrue(mimeTypes.contains("image/jpeg"));
        assertTrue(mimeTypes.contains("image/tiff"));

        Set<String> excludedMimeTypes = rendition.getExcludeMimeTypes();
        assertEquals(2, mimeTypes.size());
        assertTrue(excludedMimeTypes.contains("image/gif"));
        assertTrue(excludedMimeTypes.contains("image/bmp"));
    }

}
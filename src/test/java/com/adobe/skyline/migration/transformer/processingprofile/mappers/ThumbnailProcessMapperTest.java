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

public class ThumbnailProcessMapperTest {

    private static final String WORKFLOW_NAME = "Test Workflow";

    private ThumbnailProcessMapper mapper;

    @Before
    public void setUp() {
        this.mapper = new ThumbnailProcessMapper();
    }

    @Test
    public void testMapping() {
        Map<String, String> processMetadata = new HashMap<>();
        addThumbnailParams(processMetadata);
        addWebEnabledParams(processMetadata, "image/jpeg", 90);
        addFPOParams(processMetadata, true);

        WorkflowStep step = new WorkflowStep();
        step.setMetadata(processMetadata);

        List<RenditionConfig> renditions = mapper.mapToRenditions(null, null, step);

        assertEquals(4, renditions.size());

        boolean thumb140Matched = false;
        boolean thumb48Matched = false;
        boolean webIncluded = false;
        boolean fpoIncluded = false;

        for (RenditionConfig rendition : renditions) {
            // Use mimetypes as a shortcut to determine which rendition this is.
            // Zero included mimetypes were only set for thumbnail renditions and image/* was set for web-enabled.
            if (rendition.getIncludeMimeTypes().size() == 0) {
                Set<String> excludedMimeTypes = rendition.getExcludeMimeTypes();
                assertEquals(2, excludedMimeTypes.size());
                assertTrue(excludedMimeTypes.contains("audio/mpeg"));
                assertTrue(excludedMimeTypes.contains("video/(.*)"));

                if (rendition.getHeight() == 100) {
                    assertThumbnail(rendition, 140, 100);
                    thumb140Matched = true;
                } else if (rendition.getHeight() == 48) {
                    assertThumbnail(rendition, 48, 48);
                    thumb48Matched = true;
                }
            } else if (rendition.getIncludeMimeTypes().contains("image/*")) {
                validateWebEnabled(rendition);
                webIncluded = true;
            } else {
                validateFPO(rendition);
                fpoIncluded = true;
            }
        }

        assertTrue(thumb140Matched && thumb48Matched && webIncluded && fpoIncluded);
    }

    @Test
    public void testThumbnailParamWithBrackets() {
        Map<String, String> processMetadata = new HashMap<>();
        processMetadata.put("CONFIGS", "[140:100:false, 48:48:false]"); //Thumbnail sizes
        processMetadata.put("SKIP_MIME_TYPES", "audio/mpeg, video/(.*)"); //Skipped mimetypes for thumbnails

        WorkflowStep step = new WorkflowStep();
        step.setMetadata(processMetadata);

        List<RenditionConfig> renditions = mapper.mapToRenditions(null, null, step);

        int numThumbnails = 0;

        for (RenditionConfig rendition : renditions) {
            // Use excluded mimetypes as a shortcut to identify the thumbnail renditions
            if (rendition.getExcludeMimeTypes().size() == 2) {
                numThumbnails++;
            }
        }

        assertEquals(2, numThumbnails);
        assertEquals(3, renditions.size()); //Web Rendition is always added
    }

    @Test
    public void testThumbnailSkippedMimeTypesWithSkipPrefix() {
        Map<String, String> processMetadata = new HashMap<>();
        processMetadata.put("CONFIGS", "[100:100:false]"); //Thumbnail sizes
        processMetadata.put("SKIP_MIME_TYPES", "skip:audio/mpeg,skip:video/(.*)"); //Skipped mimetypes for thumbnails

        WorkflowStep step = new WorkflowStep();
        step.setMetadata(processMetadata);

        List<RenditionConfig> renditions = mapper.mapToRenditions(null, null, step);

        Set<String> skippedMimeTypes = renditions.get(0).getExcludeMimeTypes();
        assertTrue(skippedMimeTypes.contains("audio/mpeg"));
        assertTrue(skippedMimeTypes.contains("video/(.*)"));
    }

    @Test
    public void testProcessArgs() {
        Map<String, String> processMetadata = new HashMap<>();
        processMetadata.put("PROCESS_ARGS", "[140:100],[48:48]"); //Backwards-compatible configuration, circa CQ5

        WorkflowStep step = new WorkflowStep();
        step.setMetadata(processMetadata);

        List<RenditionConfig> renditions = mapper.mapToRenditions(null, null, step);

        assertEquals(2, renditions.size());

        boolean thumb140Matched = false;
        boolean thumb48Matched = false;

        for (RenditionConfig rendition : renditions) {
            if (rendition.getHeight() == 100) {
                assertThumbnail(rendition, 140, 100);
                thumb140Matched = true;
            } else if (rendition.getHeight() == 48) {
                assertThumbnail(rendition, 48, 48);
                thumb48Matched = true;
            }
        }

        assertTrue(thumb140Matched && thumb48Matched);
    }

    @Test
    public void testNoFPO() {
        Map<String, String> processMetadata = new HashMap<>();
        addThumbnailParams(processMetadata); // 2 Thumbnails
        addWebEnabledParams(processMetadata, "image/jpeg", 90);
        addFPOParams(processMetadata, false); // Disabled FPO

        WorkflowStep step = new WorkflowStep();
        step.setMetadata(processMetadata);

        List<RenditionConfig> renditions = mapper.mapToRenditions(null, null, step);

        assertEquals(3, renditions.size());

        boolean thumb140Matched = false;
        boolean thumb48Matched = false;
        boolean webIncluded = false;
        boolean fpoIncluded = false;

        for (RenditionConfig rendition : renditions) {
            // Use mimetypes as a shortcut to determine which rendition this is.
            // Zero included mimetypes were only set for thumbnail renditions and image/* was set for web-enabled.
            if (rendition.getIncludeMimeTypes().size() == 0) {
                if (rendition.getHeight() == 100) {
                    thumb140Matched = true;
                } else if (rendition.getHeight() == 48) {
                    thumb48Matched = true;
                }
            } else if (rendition.getIncludeMimeTypes().contains("image/*")) {
                webIncluded = true;
            } else {
                fpoIncluded = true;
            }
        }

        assertTrue(thumb140Matched && thumb48Matched && webIncluded);
        assertFalse(fpoIncluded);
    }

    @Test
    public void testWebNoQuality() {
        testWeb("jpeg");
    }

    @Test
    public void testWebPng() {
        testWeb("png");
    }

    private void testWeb(String format) {
        Map<String, String> processMetadata = new HashMap<>();
        addThumbnailParams(processMetadata);
        addWebEnabledParams(processMetadata, "image/" + format, 0);
        addFPOParams(processMetadata, true);

        WorkflowStep step = new WorkflowStep();
        step.setMetadata(processMetadata);

        List<RenditionConfig> renditions = mapper.mapToRenditions(null,null, step);

        boolean webFound = false;

        for (RenditionConfig rendition : renditions) {
            //Shortcut to grab the web-enabled rendition by the included mimetypes
            if (rendition.getIncludeMimeTypes().contains("image/*")) {
                assertThumbnail(rendition, 1280, 1280, format);
                assertEquals(0, rendition.getQuality());

                webFound = true;
            }
        }

        assertTrue(webFound);
    }

    private void addThumbnailParams(Map<String, String> processMetadata) {
        processMetadata.put("CONFIGS", "140:100:false, 48:48:false"); //Thumbnail sizes
        processMetadata.put("SKIP_MIME_TYPES", "audio/mpeg, video/(.*)"); //Skipped mimetypes for thumbnails
    }

    private void addWebEnabledParams(Map<String, String> processMetadata, String format, int quality) {
        processMetadata.put("HEIGHT", "1280"); //Height for web-enabled rendition
        processMetadata.put("WIDTH", "1280"); //Width for web-enabled rendition
        processMetadata.put("KEEP_FORMAT_LIST", "image/*"); //Mimetypes to process for web-enabled rendition
        processMetadata.put("MIME_TYPE", format); //Output mimetype for web-enabled rendition
        processMetadata.put("SKIP", "audio/mpeg, video/(.*)"); //Skipped mimetypes for web-enabled rendition

        if (quality > 0) {
            processMetadata.put("QUALITY", String.valueOf(quality)); //Quality for web-enabled rendition
        }
    }

    private void addFPOParams(Map<String, String> processMetadata, boolean enabled) {
        processMetadata.put("CREATE_FPO_MIMETYPES", "image/tiff, image/png, application/photoshop"); //Mimetypes to create FPO for
        processMetadata.put("FPO_QUALITY", "10");

        if (enabled) {
            processMetadata.put("FPO_CREATION_ENABLED", "true");
        }
    }

    private void validateWebEnabled(RenditionConfig rendition) {
        Set<String> includedMimeTypes = rendition.getIncludeMimeTypes();
        assertEquals(1, includedMimeTypes.size());
        assertTrue(includedMimeTypes.contains("image/*"));

        Set<String> excludedMimeTypes = rendition.getExcludeMimeTypes();
        assertEquals(2, excludedMimeTypes.size());
        assertTrue(excludedMimeTypes.contains("audio/mpeg"));
        assertTrue(excludedMimeTypes.contains("video/(.*)"));

        assertThumbnail(rendition, 1280, 1280, "jpeg");
        assertEquals(90, rendition.getQuality());
    }

    private void validateFPO(RenditionConfig rendition) {
        Set<String> includedMimeTypes = rendition.getIncludeMimeTypes();
        assertEquals(3, includedMimeTypes.size());
        assertTrue(includedMimeTypes.contains("image/tiff"));
        assertTrue(includedMimeTypes.contains("image/png"));
        assertTrue(includedMimeTypes.contains("application/photoshop"));

        assertFPO(rendition, 10);
    }

    private void assertThumbnail(RenditionConfig rendition, int width, int height) {
        assertThumbnail(rendition, width, height, "png");
    }

    private void assertThumbnail(RenditionConfig rendition, int width, int height, String format) {
        assertEquals(height, rendition.getHeight());
        assertEquals(width, rendition.getWidth());
        assertEquals(format, rendition.getFormat());
        assertEquals("thumbnail", rendition.getNodeName());
        assertEquals("cq5dam.thumbnail." + width + "." + height + "." + format, rendition.getFileName()); //cq5dam.thumbnail.WIDTH.HEIGHT.EXTENSION
    }

    @SuppressWarnings("SameParameterValue")
    private void assertFPO(RenditionConfig rendition, int quality) {
        assertEquals(0, rendition.getHeight());
        assertEquals(0, rendition.getWidth());
        assertEquals(quality, rendition.getQuality());
        assertEquals("jpeg", rendition.getFormat());
        assertEquals("fpo", rendition.getNodeName());
        assertEquals("cq5dam.fpo.jpeg", rendition.getFileName()); //cq5dam.thumbnail.WIDTH.HEIGHT.EXTENSION
    }
}
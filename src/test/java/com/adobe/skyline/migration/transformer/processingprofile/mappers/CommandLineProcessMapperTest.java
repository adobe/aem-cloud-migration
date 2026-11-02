/*
 Copyright 2026 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.skyline.migration.transformer.processingprofile.mappers;

import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.model.RenditionConfig;
import com.adobe.skyline.migration.model.workflow.WorkflowModel;
import com.adobe.skyline.migration.model.workflow.WorkflowStep;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandLineProcessMapperTest {

    private ChangeTrackingService changeTracker;
    private CommandLineProcessMapper mapper;
    private WorkflowModel model;

    @Before
    public void setUp() {
        changeTracker = new ChangeTrackingService();
        mapper = new CommandLineProcessMapper(changeTracker);
        model = new WorkflowModel();
        model.setName("Test Workflow");
    }

    @Test
    public void testResizeCommandMapped() {
        List<RenditionConfig> renditions = map("[convert ${filename} -resize 800x600> -quality 85 -strip cq5dam.web.800.600.jpg]",
                "[image/jpeg,image/png]", null);

        assertEquals(1, renditions.size());
        RenditionConfig rendition = renditions.get(0);
        assertEquals("jpeg", rendition.getFormat());
        assertEquals(800, rendition.getWidth());
        assertEquals(600, rendition.getHeight());
        assertEquals(85, rendition.getQuality());
        assertEquals("cq5dam.web.800.600.jpg", rendition.getFileName());
        assertEquals("cq5dam.web.800.600", rendition.getNodeName());
        assertEquals(new HashSet<>(Arrays.asList("image/jpeg", "image/png")), rendition.getIncludeMimeTypes());
        assertTrue(changeTracker.getFailedMappings().isEmpty());
    }

    @Test
    public void testFormatConversionWithPlaceholderInOutputName() {
        List<RenditionConfig> renditions = map("[magick ${filename}[0] cq5dam-${basename}-v.png]", null, null);

        assertEquals(1, renditions.size());
        RenditionConfig rendition = renditions.get(0);
        assertEquals("png", rendition.getFormat());
        assertEquals(0, rendition.getWidth());
        assertEquals(0, rendition.getHeight());
        assertEquals("cq5dam-v.png", rendition.getFileName());
        assertTrue(rendition.getIncludeMimeTypes().isEmpty());
    }

    @Test
    public void testStandardThumbnailCommandSkipped() {
        List<RenditionConfig> renditions = map(
                "[convert -define jpeg:size=319x319 ${filename}[0] -thumbnail 319x319 cq5dam.thumbnail.319.319.png]", null, null);

        assertTrue(renditions.isEmpty());
        assertTrue(changeTracker.getFailedMappings().isEmpty());
    }

    @Test
    public void testStandardThumbnailNameWithOtherGeometryReported() {
        List<RenditionConfig> renditions = map("[convert ${filename} -thumbnail 100x100 cq5dam.thumbnail.319.319.png]", null, null);

        assertTrue(renditions.isEmpty());
        assertEquals(1, changeTracker.getFailedMappings().size());
    }

    @Test
    public void testThumbnailsMapped() {
        List<RenditionConfig> renditions = map(null, "image/.*", "[48:48,140:100,319:319,200:150]");

        assertEquals(1, renditions.size());
        RenditionConfig rendition = renditions.get(0);
        assertEquals("png", rendition.getFormat());
        assertEquals(200, rendition.getWidth());
        assertEquals(150, rendition.getHeight());
        assertEquals("cq5dam.thumbnail.200.150.png", rendition.getFileName());
        assertEquals(Collections.singleton("image/.*"), rendition.getIncludeMimeTypes());
    }

    @Test
    public void testUnmappableCommandsReported() {
        List<RenditionConfig> renditions = map("[convert ${filename} -colorspace Gray gray.png,"
                + "PDFRasterizer -d -p 1 -s 1280 -t PNG -i ${file},"
                + "convert -define png:bit-depth=8 ${filename} depth.png,"
                + "convert -resize 25% ${filename} cq5dam-red-25-per.png,"
                + "convert ${filename} out.bmp,"
                + "convert other.png out.png]", null, null);

        assertTrue(renditions.isEmpty());
        Map<String, String> failures = changeTracker.getFailedMappings();
        assertEquals(6, failures.size());
        for (String mapping : failures.keySet()) {
            assertTrue(mapping, mapping.contains("Test Workflow"));
        }
    }

    private List<RenditionConfig> map(String commands, String mimeTypes, String thumbnails) {
        Map<String, String> metadata = new HashMap<>();
        if (commands != null) {
            metadata.put("COMMANDS", commands);
        }
        if (mimeTypes != null) {
            metadata.put("MIME_TYPES", mimeTypes);
        }
        if (thumbnails != null) {
            metadata.put("THUMBNAILS", thumbnails);
        }

        WorkflowStep step = new WorkflowStep();
        step.setProcess("com.day.cq.dam.core.process.CommandLineProcess");
        step.setMetadata(metadata);
        return mapper.mapToRenditions(model, step);
    }
}

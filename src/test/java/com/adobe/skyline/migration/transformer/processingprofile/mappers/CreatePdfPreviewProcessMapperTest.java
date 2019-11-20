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
import com.adobe.skyline.migration.model.RenditionConfig;
import com.adobe.skyline.migration.model.workflow.WorkflowModel;
import com.adobe.skyline.migration.model.workflow.WorkflowStep;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CreatePdfPreviewProcessMapperTest {

    private CreatePdfPreviewProcessMapper mapper;

    @Before
    public void setUp() {
        this.mapper = new CreatePdfPreviewProcessMapper();
    }

    @Test
    public void testMapping() {
        WorkflowStep previewStep = getPreviewWorkflowStep();

        List<WorkflowStep> steps = new ArrayList<>();
        steps.add(previewStep);

        WorkflowModel model = new WorkflowModel();
        model.setWorkflowSteps(steps);

        List<RenditionConfig> renditions = mapper.mapToRenditions(model, previewStep);

        RenditionConfig rendition = renditions.get(0);
        assertEquals(2048, rendition.getHeight());
        assertEquals(2048, rendition.getWidth());
        assertEquals("png", rendition.getFormat());
        assertEquals("preview", rendition.getNodeName());
        assertEquals("cqdam.preview.png", rendition.getFileName());

        Set<String> mimeTypes = rendition.getIncludeMimeTypes();
        assertEquals(3, mimeTypes.size());
        assertTrue(mimeTypes.contains("application/pdf"));
        assertTrue(mimeTypes.contains("application/postscript"));
        assertTrue(mimeTypes.contains("application/illustrator"));
    }

    @Test
    public void testNoRenditionsReturnedWhenDeletePreviewPresent() {
        WorkflowStep previewStep = getPreviewWorkflowStep();
        WorkflowStep deletePreviewStep = getDeletePreviewStep();

        List<WorkflowStep> steps = new ArrayList<>();
        steps.add(previewStep);
        steps.add(deletePreviewStep);

        WorkflowModel model = new WorkflowModel();
        model.setWorkflowSteps(steps);

        List<RenditionConfig> renditions = mapper.mapToRenditions(model, previewStep);

        assertEquals(0, renditions.size());
    }

    private WorkflowStep getPreviewWorkflowStep() {
        Map<String, String> processMetadata = new HashMap<>();
        processMetadata.put("MAX_HEIGHT", "2048");
        processMetadata.put("MAX_WIDTH", "2048");
        processMetadata.put("MIME_TYPES", "[application/pdf,application/postscript,application/illustrator]");
        processMetadata.put("RESOLUTION", "72");

        WorkflowStep step = new WorkflowStep();
        step.setProcess(mapper.getProcess());
        step.setMetadata(processMetadata);
        return step;
    }

    private WorkflowStep getDeletePreviewStep() {
        WorkflowStep step = new WorkflowStep();
        step.setProcess(MigrationConstants.DELETE_PREVIEW_PROCESS);
        return step;
    }

}
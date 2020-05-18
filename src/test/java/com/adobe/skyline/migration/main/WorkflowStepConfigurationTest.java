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

package com.adobe.skyline.migration.main;

import org.junit.Before;
import org.junit.Test;

import com.adobe.skyline.migration.model.WorkflowStepSupportStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WorkflowStepConfigurationTest {

    private WorkflowStepConfiguration config;

    @Before
    public void setUp() {
        this.config = new WorkflowStepConfiguration();
    }

    @Test
    public void testOptionalWorkflows() {
        assertTrue(config.isOptionalStep("com.day.cq.dam.core.impl.process.SendTransientWorkflowCompletedEmailProcess"));
    }

    @Test
    public void testUnknownSupportedStatus() {
        assertEquals(WorkflowStepSupportStatus.UNKNOWN, config.getStepSupportedStatus("com.customer.custom.WorkflowStep"));
    }
}

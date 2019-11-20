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

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.adobe.skyline.migration.exception.MigrationRuntimeException;
import com.adobe.skyline.migration.model.WorkflowStepSupportStatus;

/**
 * Loads the configuration for the various workflow steps that we may encounter along with their level of support.  The
 * workflow step configurations can be found in src/main/java/resources/workflowSteps.properties.
 */
public class WorkflowStepConfiguration {

    private Map<WorkflowStepSupportStatus, String[]> stepsByStatus;
    private Map<String, WorkflowStepSupportStatus> statusByStep;

    public WorkflowStepConfiguration() {
        loadWorkflowStepProperties();
    }

    public boolean isStepEnabledOnSkyline(String step) {

        WorkflowStepSupportStatus status = statusByStep.get(step);

        // We allow customer workflow steps to run.
        // Thus, if we have not configured a step to be excluded, we should assume that it is supported.
        if (status == null) {
            return true;
        } else {
            return (status == WorkflowStepSupportStatus.SUPPORTED
                    || status == WorkflowStepSupportStatus.OPTIONAL
                    || status == WorkflowStepSupportStatus.REQUIRED);
        }
    }

    // These are workflow steps that we don't want to remove from a workflow if it is one that we plan to run on Skyline,
    // but we wouldn't run a workflow just for the purpose of executing this step.
    public boolean isOptionalStep(String process) {
        String[] optionalSteps = stepsByStatus.get(WorkflowStepSupportStatus.OPTIONAL);
        String[] requiredSteps = stepsByStatus.get(WorkflowStepSupportStatus.REQUIRED);
        return (Arrays.asList(optionalSteps).contains(process) || Arrays.asList(requiredSteps).contains(process));
    }

    public WorkflowStepSupportStatus getStepSupportedStatus(String step) {
        WorkflowStepSupportStatus status = statusByStep.get(step);

        if (status == null) {
            status = WorkflowStepSupportStatus.UNKNOWN;
        }

        return status;
    }

    private void loadWorkflowStepProperties() {
        stepsByStatus = new HashMap<>();
        statusByStep = new HashMap<>();

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("workflowSteps.properties")) {

            if (stream == null) {
                throw new MigrationRuntimeException("Unable to load the workflowSteps.properties file.");
            }

            Properties props = new Properties();
            props.load(stream);

            Enumeration keys = props.propertyNames();
            while(keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String value = props.getProperty(key);

                WorkflowStepSupportStatus status = WorkflowStepSupportStatus.valueOf(key);
                String[] steps = value.split(",");

                stepsByStatus.put(status, steps);

                for (String step : steps) {
                    statusByStep.put(step, status);
                }
            }
        } catch (IOException e) {
            throw new MigrationRuntimeException("Exception occurred when reading properties.", e);
        }
    }
}

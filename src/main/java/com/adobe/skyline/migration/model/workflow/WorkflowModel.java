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

package com.adobe.skyline.migration.model.workflow;

import java.io.File;
import java.util.List;

public class WorkflowModel {
    private String name;

    //Path to the workflow's Component node under /var/workflow or /etc
    private String runtimeComponent;

    //Path to the workflow's configuration Page node under /conf or /etc
    private String configurationPage;
    //File object that is referenced by the configurationPage path
    private File configurationFile;

    private List<WorkflowStep> workflowSteps;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRuntimeComponent() {
        return runtimeComponent;
    }

    public void setRuntimeComponent(String workflowComponent) {
        this.runtimeComponent = workflowComponent;
    }

    public String getConfigurationPage() {
        return configurationPage;
    }

    public void setConfigurationPage(String workflowPage) {
        this.configurationPage = workflowPage;
    }

    public File getConfigurationFile() {
        return configurationFile;
    }

    public void setConfigurationFile(File configurationFile) {
        this.configurationFile = configurationFile;
    }

    public List<WorkflowStep> getWorkflowSteps() {
        return workflowSteps;
    }

    public void setWorkflowSteps(List<WorkflowStep> workflowSteps) {
        this.workflowSteps = workflowSteps;
    }
}
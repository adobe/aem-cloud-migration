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

import java.util.ArrayList;
import java.util.List;

public class Workflow {

    private WorkflowModel workflowModel;
    private List<WorkflowLauncher> launchers;
    private boolean canRunOnSkyline = false;

    public Workflow() {
        this.launchers = new ArrayList<>();
    }

    public WorkflowModel getWorkflowModel() {
        return workflowModel;
    }

    public void setWorkflowModel(WorkflowModel workflowModel) {
        this.workflowModel = workflowModel;
    }

    public List<WorkflowLauncher> getLaunchers() {
        return launchers;
    }

    public void addLauncher(WorkflowLauncher launcher) {
        this.launchers.add(launcher);
    }

    public boolean getCanRunOnSkyline() {
        return canRunOnSkyline;
    }

    public void setCanRunOnSkyline(boolean canRunOnSkyline) {
        this.canRunOnSkyline = canRunOnSkyline;
    }
}
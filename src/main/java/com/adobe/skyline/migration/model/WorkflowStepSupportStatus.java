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

package com.adobe.skyline.migration.model;

public enum WorkflowStepSupportStatus {
    SUPPORTED (Actions.NONE, "This workflow process is supported in AEM Assets as a Cloud Service environments."),
    OPTIONAL (Actions.NONE, "This workflow process is optional in AEM Assets as a Cloud Service environments."),
    REQUIRED (Actions.ADDED, "Required step added to the workflow."),
    UNNECESSARY (Actions.REMOVED, "This process is not necessary in AEM Assets as a Cloud Service."),
    NUI_OOTB (Actions.REMOVED, "This functionality is provided by the Asset Compute Service."),
    DMS7_OOTB (Actions.REMOVED, "This functionality is provided by our OOTB Dynamic Media connectors."),
    NUI_MIGRATED (Actions.REMOVED, "This configuration has been migrated to a processing profile for the Asset Compute Service."),
    UNKNOWN (Actions.NONE, "This workflow step has not been tested for compatibility with AEM Assets as a Cloud Service."),
    UNSUPPORTED (Actions.REMOVED, "This process is not currently supported in AEM Assets as a Cloud Service.");

    private String action;
    private String description;

    WorkflowStepSupportStatus(String action, String description) {
        this.action = action;
        this.description = description;
    }

    public String getAction() {
        return this.action;
    }

    public String getDescription() {
        return this.description;
    }

    private static class Actions {
        static final String NONE = "None";
        static final String ADDED = "Added";
        static final String REMOVED = "Removed";
    }
}

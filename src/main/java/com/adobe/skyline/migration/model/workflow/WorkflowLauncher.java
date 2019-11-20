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

public class WorkflowLauncher {

    private String name;
    private String relativePath;
    private String glob;
    private String excludeList;
    private File launcherFile;
    private List<String> conditions;
    private boolean isEnabled;
    private String modelPath;
    private boolean isSynthetic = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getGlob() {
        return glob;
    }

    public void setGlob(String glob) {
        this.glob = glob;
    }

    public String getExcludeList() {
        return excludeList;
    }

    public void setExcludeList(String excludeList) {
        this.excludeList = excludeList;
    }

    public File getLauncherFile() {
        return launcherFile;
    }

    public void setLauncherFile(File launcherFile) {
        this.launcherFile = launcherFile;
    }

    public List<String> getConditions() {
        return this.conditions;
    }

    public void setConditions(List<String> conditions) {
        this.conditions = conditions;
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public String getModelPath() {
        return this.modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    /**
     * If a workflow launcher is not actually included in the customer's code but is inferred by the default settings, we
     * will sometimes create a "synthetic" workflow launcher to include in the Workflow object.  This is then used when
     * generating Processing Profiles and Workflow Runner Configs, but should be ignored for the purposes of disabling
     * launchers.  This should only be called within the MigrationConstants class.
     */
    public boolean isSynthetic() {
        return this.isSynthetic;
    }

    public void setSynthetic(boolean isSynthetic) {
        this.isSynthetic = isSynthetic;
    }
}

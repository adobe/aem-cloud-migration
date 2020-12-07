/*
 Copyright 2020 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.skyline.migration.transformer;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import com.adobe.skyline.migration.dao.FilterFileDAO;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.model.workflow.WorkflowProject;
import com.adobe.skyline.migration.util.file.FileUtil;

public class VarNodeCleaner {

    private FilterFileDAO filterFileDao;
    private ChangeTrackingService changeTrackingService;

    public VarNodeCleaner(FilterFileDAO filterFileDao, ChangeTrackingService changeTrackingService) {
        this.filterFileDao = filterFileDao;
        this.changeTrackingService = changeTrackingService;
    }

    public void cleanNodes(WorkflowProject project) {
        Pattern varMatcher = Pattern.compile("^/var/workflow.*");

        List<String> varPaths = filterFileDao.findPathsWith(varMatcher);

        for (String path : varPaths) {
            filterFileDao.removePath(path);
        }

        if (varPaths.size() > 0) {
            removeFromFilesystem(project);
        }
    }

    private void removeFromFilesystem(WorkflowProject project) {
        File varWorkflowRoot = new File(project.getPath() + "/src/main/content/jcr_root/var/workflow");
        if (varWorkflowRoot.exists()) {
            FileUtil.deleteRecursively(varWorkflowRoot);
            File varRoot = varWorkflowRoot.getParentFile();
            if (varRoot.listFiles().length == 0) {
                varRoot.delete();
                changeTrackingService.trackVarPathDeleted(varRoot.getPath());
            } else {
                changeTrackingService.trackVarPathDeleted(varWorkflowRoot.getPath());
            }
        }
    }
}

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

package com.adobe.skyline.migration.dao;

import java.io.File;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.exception.CustomerDataException;
import com.adobe.skyline.migration.model.workflow.WorkflowLauncher;
import com.adobe.skyline.migration.util.StringUtil;
import com.adobe.skyline.migration.util.XmlUtil;

/**
 * An object to abstract reading workflow launcher configurations from and writing them to disk.
 */
public class WorkflowLauncherDAO {

    public WorkflowLauncher getWorkflowLauncher(String moduleAbsoluteRoot, String launcherConfigPath) throws CustomerDataException {
        try {
            File launcherFile = new File(launcherConfigPath);
            NamedNodeMap launcherProperties = XmlUtil.loadXml(launcherFile).getFirstChild().getAttributes();

            String launcherName = launcherFile.getParentFile().getName();
            String relativePath = getlauncherRelativePath(moduleAbsoluteRoot, launcherFile);

            String glob = "";
            if (launcherProperties.getNamedItem(MigrationConstants.GLOB_PROP) != null) {
                glob = launcherProperties.getNamedItem(MigrationConstants.GLOB_PROP).getTextContent();
            }

            String excludeList = "";
            if (launcherProperties.getNamedItem(MigrationConstants.EXCLUDE_LIST_PROP) != null) {
                excludeList = launcherProperties.getNamedItem(MigrationConstants.EXCLUDE_LIST_PROP).getTextContent();
            }

            String modelPath = "";
            if (launcherProperties.getNamedItem(MigrationConstants.WORKFLOW_MODEL_PROP) != null) {
                modelPath = launcherProperties.getNamedItem(MigrationConstants.WORKFLOW_MODEL_PROP).getTextContent();
            }

            String conditions;
            if (launcherProperties.getNamedItem(MigrationConstants.CONDITIONS_PROP) != null) {
                conditions = launcherProperties.getNamedItem(MigrationConstants.CONDITIONS_PROP).getTextContent();
            } else {
                //In older launchers, this property was named "condition" with no 's'
                conditions = launcherProperties.getNamedItem(MigrationConstants.CONDITION_PROP).getTextContent();
            }

            boolean enabled = false;
            Node enabledNode = launcherProperties.getNamedItem(MigrationConstants.ENABLED_PROP);
            if (enabledNode != null && enabledNode.getTextContent().equals(MigrationConstants.TRUE_VALUE)) {
                enabled = true;
            }

            List<String> conditionsList = StringUtil.getListFromString(conditions);

            WorkflowLauncher launcher = new WorkflowLauncher();
            launcher.setName(launcherName);
            launcher.setRelativePath(relativePath);
            launcher.setGlob(glob);
            launcher.setExcludeList(excludeList);
            launcher.setConditions(conditionsList);
            launcher.setLauncherFile(launcherFile);
            launcher.setEnabled(enabled);
            launcher.setModelPath(modelPath);

            return launcher;
        } catch (Exception e) {
            throw new CustomerDataException("Unable to parse workflow launcher.", e);
        }
    }

    public void disableLauncher(WorkflowLauncher launcher) throws CustomerDataException {
        try {
            File launcherFile = launcher.getLauncherFile();
            Document xml = XmlUtil.loadXml(launcherFile);
            xml.getFirstChild().getAttributes().getNamedItem(MigrationConstants.ENABLED_PROP).setTextContent(MigrationConstants.FALSE_VALUE);
            XmlUtil.writeXml(xml, launcherFile);
        } catch (Exception e) {
            throw new CustomerDataException("Unable to disable workflow launcher.", e);
        }

    }

    private String getlauncherRelativePath(String moduleAbsoluteRoot, File launcherFile) {
        int projectPathLength = moduleAbsoluteRoot.length();
        String launcherAbsolutePath = launcherFile.getPath();
        return launcherAbsolutePath.substring(projectPathLength);
    }
}

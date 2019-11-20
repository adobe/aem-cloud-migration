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
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.exception.MigrationRuntimeException;
import com.adobe.skyline.migration.util.XmlUtil;

/**
 * An object to abstract reading workflow runner service configurations from and writing them to disk.
 */
public class WorkflowRunnerConfigDAO {

    private File configFile;
    private Document configDoc;

    public WorkflowRunnerConfigDAO(String projectPath) {
        this.configFile = new File(projectPath + MigrationConstants.PATH_TO_JCR_ROOT +
                MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH + "/" + MigrationConstants.WORKFLOW_RUNNER_CONFIG_FILENAME);
    }

    public void createConfigByExpression(String expression, String model) {
        addConfig(MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_EXPRESSION, expression, model);
    }

    public void createConfigByPath(String path, String model) {
        addConfig(MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_PATH, path, model);
    }

    private void addConfig(String configType, String match, String model) {
        try {
            //Only create the config file if this method has been called.  We don't want to create an empty configuration in the constructor.
            if (!configFile.exists()) {
                createEmptyConfig(configFile);
            }

            Element jcrRoot = configDoc.getDocumentElement();
            List<String> mappings = XmlUtil.getStringArrayListFromAttribute(jcrRoot, configType);
            mappings.add(match + ":" + model);
            String outputValue = XmlUtil.getSerializedArrayValueFromList(mappings);
            Attr wfByExp = XmlUtil.getOrCreateAttr(jcrRoot, configType);
            wfByExp.setValue(outputValue);

            XmlUtil.writeXml(configDoc, configFile);
        } catch (Exception e) {
            throw new MigrationRuntimeException(e);
        }
    }

    private void createEmptyConfig(File configFile) throws IOException, ParserConfigurationException {
        configDoc = XmlUtil.createXml();

        Element rootEl = configDoc.createElement(MigrationConstants.JCR_ROOT_NODE);
        configDoc.appendChild(rootEl);

        Attr nsSling = configDoc.createAttribute(MigrationConstants.NS_SLING);
        nsSling.setValue(MigrationConstants.NS_SLING_VALUE);
        rootEl.setAttributeNode(nsSling);

        Attr nsJcr = configDoc.createAttribute(MigrationConstants.NS_JCR);
        nsJcr.setValue(MigrationConstants.NS_JCR_VALUE);
        rootEl.setAttributeNode(nsJcr);

        Attr primType = configDoc.createAttribute(MigrationConstants.JCR_PRIMARY_TYPE_PROP);
        primType.setValue(MigrationConstants.OSGI_CONFIG_TYPE_VALUE);
        rootEl.setAttributeNode(primType);

        configFile.getParentFile().mkdirs();
        configFile.createNewFile();
    }

}

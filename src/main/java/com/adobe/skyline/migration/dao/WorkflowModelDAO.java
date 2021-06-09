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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.exception.CustomerDataException;
import com.adobe.skyline.migration.model.workflow.WorkflowModel;
import com.adobe.skyline.migration.model.workflow.WorkflowStep;
import com.adobe.skyline.migration.util.Logger;
import com.adobe.skyline.migration.util.XmlUtil;

/**
 * An object to abstract reading workflow models from and writing them to disk.
 */
public class WorkflowModelDAO {

    public WorkflowModel loadWorkflowModel(String moduleAbsoluteRoot, String workflowModelPath) throws CustomerDataException {
        Logger.DEBUG("workflowModelPath: " + workflowModelPath);

        String codeRoot = Path.of(moduleAbsoluteRoot, MigrationConstants.PATH_TO_JCR_ROOT).toString();

        String varPath = "";
        String confPath = "";

        /*
         * We need to handle both runtime (var) and configuration (conf) paths.  Additionally, for backwards
         * compatibility, we need to support both /conf and /etc paths.
         */
        if (workflowModelPath.startsWith(MigrationConstants.VAR_ROOT)) { //New style "var" model
            varPath = workflowModelPath;
            confPath = workflowModelPath.replace(MigrationConstants.VAR_ROOT, MigrationConstants.CONF_PATH);
        } else if (workflowModelPath.startsWith(MigrationConstants.CONF_PATH)) { //New style "conf" model
            confPath = workflowModelPath;
            varPath = confPath.replace(MigrationConstants.CONF_PATH, MigrationConstants.VAR_ROOT);
        } else if (workflowModelPath.startsWith(MigrationConstants.ETC_ROOT) && workflowModelPath.endsWith(MigrationConstants.MODEL)) { //Old style "var" model
            varPath = workflowModelPath;
            confPath = workflowModelPath.substring(0, workflowModelPath.lastIndexOf(MigrationConstants.ETC_MODEL_SUFFIX));
        } else if (workflowModelPath.startsWith(MigrationConstants.ETC_ROOT)) { //Old style "conf" model
            confPath = workflowModelPath;
            varPath = confPath + MigrationConstants.ETC_MODEL_SUFFIX;
        }

        return createWorkflowModel(codeRoot, varPath, confPath);
    }

    public void removeWorkflowStepFromModel(String workflowStep, WorkflowModel model) throws CustomerDataException {
        try {
            removeStepFromConfigFile(workflowStep, model);
        } catch (Exception e) {
            throw new CustomerDataException("Unable to update the workflow model for " + model.getName(), e);
        }
    }

    public void addWorkflowStepToModel(WorkflowStep workflowStep, WorkflowModel model) throws CustomerDataException {
        try {
            addStepToConfigFile(workflowStep, model);
        } catch (Exception e) {
            throw new CustomerDataException("Unable to add new workflow step to model: ", e);
        }
    }

    private WorkflowModel createWorkflowModel(String codeRoot, String varPath, String confPath) throws CustomerDataException {
        Logger.DEBUG("codeRoot: " + codeRoot);
        Logger.DEBUG("confPath: " + confPath);

        File confFile = new File(Path.of(codeRoot, confPath, MigrationConstants.CONTENT_XML).toString());

        Logger.DEBUG("confFile path: " + confFile.getPath());

        if (confFile.exists()) {
            String name = confPath.substring(confPath.lastIndexOf("/") + 1);

            WorkflowModel model = new WorkflowModel();
            model.setName(name);
            model.setConfigurationPage(confPath);
            model.setConfigurationFile(confFile);
            model.setRuntimeComponent(varPath);

            try {
                Document workflowDocument = XmlUtil.loadXml(confFile);
                List<WorkflowStep> workflowSteps = extractWorkflowSteps(workflowDocument);
                model.setWorkflowSteps(workflowSteps);
            } catch (Exception e) {
                throw new CustomerDataException("Unable to parse workflow model XML.", e);
            }
            return model;
        } else {
            return null;
        }
    }

    private List<WorkflowStep> extractWorkflowSteps(Document modelXml) {
        List<Node> stepNodes = new ArrayList<>();

        //Each workflow model should have exactly one flow tag
        NodeList flowNodes = modelXml.getElementsByTagName(MigrationConstants.FLOW_NODE);
        if (flowNodes != null && flowNodes.getLength() == 1) {
            stepNodes = XmlUtil.getChildElementNodes(flowNodes.item(0));
        }

        List<WorkflowStep> steps = new ArrayList<>();
        for (Node stepNode : stepNodes) {
            WorkflowStep currStep = createWorkflowStep(stepNode);

            if (currStep.getProcess() == null || currStep.getProcess().isEmpty()) {
                Logger.WARN("Unable to map a workflow step in " + modelXml.getDocumentURI() + " because it does not " +
                        "contain a PROCESS or EXTERNAL_PROCESS value.  Other workflow steps, such as OR splits, are not " +
                        "supported at this time.");

            } else {
                steps.add(currStep);
            }
        }

        return steps;
    }

    private WorkflowStep createWorkflowStep(Node stepXml){
        WorkflowStep step = new WorkflowStep();

        List<Node> stepChildren = XmlUtil.getChildElementNodes(stepXml);

        for (Node currNode : stepChildren) {
            if(currNode.getNodeName().equalsIgnoreCase(MigrationConstants.METADATA_XML_NODE)) {
                step.setNodeName(stepXml.getNodeName());
                String processValue = extractProcessValue(currNode);
                step.setProcess(processValue);

                Map<String, String> metadata = new HashMap<>();
                NamedNodeMap nodeMap = currNode.getAttributes();
                for (int i = 0; i < nodeMap.getLength(); i++) {
                    Node namedNode = nodeMap.item(i);
                    metadata.put(namedNode.getNodeName(), namedNode.getNodeValue());
                }
                step.setMetadata(metadata);

                break;
            }
        }

        return step;
    }

    private void removeStepFromConfigFile(String workflowStep, WorkflowModel model) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        Document modelXml = XmlUtil.loadXml(model.getConfigurationFile());

        //Each workflow model should have exactly one flow tag
        List<Node> stepNodes = XmlUtil.getChildElementNodes(modelXml.getElementsByTagName(MigrationConstants.FLOW_NODE).item(0));

        for (Node stepNode : stepNodes) {
            List<Node> stepChildren = XmlUtil.getChildElementNodes(stepNode);

            for (Node currNode : stepChildren) {
                if(currNode.getNodeName().equalsIgnoreCase(MigrationConstants.METADATA_XML_NODE)) {
                    String processValue = extractProcessValue(currNode);
                    if (workflowStep.equals(processValue)) {
                        stepNode.getParentNode().removeChild(stepNode);
                    }
                }
            }
        }

        XmlUtil.writeXml(modelXml, model.getConfigurationFile());
    }

    private String extractProcessValue(Node metadataNode){
        String processValue = null;

        Node processNode = metadataNode.getAttributes().getNamedItem(MigrationConstants.PROCESS_PROP);

        if (processNode != null) {
            processValue = processNode.getTextContent();
        } else {
            Node externalProcessNode = metadataNode.getAttributes().getNamedItem(MigrationConstants.EXTERNAL_PROCESS_PROP);

            if (externalProcessNode != null) {
            processValue = externalProcessNode.getTextContent();
            }
        }
        return processValue;
    }

    private void addStepToConfigFile(WorkflowStep workflowStep, WorkflowModel model) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        Document modelXml = XmlUtil.loadXml(model.getConfigurationFile());

        Element metadataNode = modelXml.createElement(MigrationConstants.METADATA_XML_NODE);
        metadataNode.setAttribute(MigrationConstants.JCR_PRIMARY_TYPE_PROP, MigrationConstants.NT_UNSTRUCTURED_TYPE_VALUE);
        metadataNode.setAttribute(MigrationConstants.PROCESS_PROP, workflowStep.getProcess());
        metadataNode.setAttribute(MigrationConstants.PROCESS_AUTO_ADVANCE_PROP, "true");

        Element processNode = modelXml.createElement(workflowStep.getNodeName());
        processNode.setAttribute(MigrationConstants.JCR_DESCRIPTION_PROP, workflowStep.getDescription());
        processNode.setAttribute(MigrationConstants.JCR_PRIMARY_TYPE_PROP, MigrationConstants.NT_UNSTRUCTURED_TYPE_VALUE);
        processNode.setAttribute(MigrationConstants.JCR_TITLE_PROP, workflowStep.getTitle());
        processNode.setAttribute(MigrationConstants.SLING_RESOURCE_TYPE_PROP, workflowStep.getResourceType());
        processNode.appendChild(metadataNode);

        Node flowNode = modelXml.getElementsByTagName(MigrationConstants.FLOW_NODE).item(0);
        flowNode.appendChild(processNode);

        XmlUtil.writeXml(modelXml, model.getConfigurationFile());
    }
}

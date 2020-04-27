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

        String codeRoot = moduleAbsoluteRoot + MigrationConstants.PATH_TO_JCR_ROOT;

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

            if (model.getRuntimeFile() != null) {
                removeStepFromVarFile(workflowStep, model);
            }
        } catch (Exception e) {
            throw new CustomerDataException("Unable to update the workflow model for " + model.getName(), e);
        }
    }

    public void addWorkflowStepToModel(WorkflowStep workflowStep, WorkflowModel model) throws CustomerDataException {
        try {
            addStepToConfigFile(workflowStep, model);

            if (model.getRuntimeFile() != null) {
                addStepToVarFile(workflowStep, model);
            }
        } catch (Exception e) {
            throw new CustomerDataException("Unable to add new workflow step to model: ", e);
        }
    }

    private WorkflowModel createWorkflowModel(String codeRoot, String varPath, String confPath) throws CustomerDataException {
        Logger.DEBUG("codeRoot: " + codeRoot);
        Logger.DEBUG("confPath: " + confPath);

        File confFile = new File(codeRoot + confPath + File.separator +  MigrationConstants.CONTENT_XML);
        File varFile = new File(codeRoot + varPath.replace(MigrationConstants.JCR_CONTENT, MigrationConstants.JCR_CONTENT_ON_DISK) + "." + MigrationConstants.XML_EXTENSION);

        Logger.DEBUG("confFile path: " + confFile.getPath());

        if (confFile.exists()) {
            String name = confPath.substring(confPath.lastIndexOf("/") + 1);

            WorkflowModel model = new WorkflowModel();
            model.setName(name);
            model.setConfigurationPage(confPath);
            model.setConfigurationFile(confFile);
            model.setRuntimeComponent(varPath);

            if (varFile.exists()) {
                model.setRuntimeFile(varFile);
            }

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

    private void removeStepFromVarFile(String workflowStep, WorkflowModel model) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        Document varXml = XmlUtil.loadXml(model.getRuntimeFile());
        int nodeIndex = getWorkflowNodeIndexForProcess(varXml, workflowStep);
        if (nodeIndex > -1) {
            removeWorkflowNode(varXml, nodeIndex);
            removeTransition(varXml);
            XmlUtil.writeXml(varXml, model.getRuntimeFile());
        } else {
            Logger.DEBUG("Unable to find " + workflowStep + " in " + model.getRuntimeComponent());
        }

    }

    private int getWorkflowNodeIndexForProcess(Document xml, String workflowStep) {
        List<Node> nodes = XmlUtil.getChildElementNodes(xml.getElementsByTagName(MigrationConstants.NODES_NODE).item(0));
        for (Node currNode : nodes) {
            Node metadataNode = XmlUtil.getChildElementNodes(currNode).get(0);
            String process = extractProcessValue(metadataNode);
            if (workflowStep.equals(process)) {
                return getIndex(currNode);
            }
        }
        return -1;
    }

    private void removeWorkflowNode(Document xml, int nodeIndex) {
        List<Node> nodes = XmlUtil.getChildElementNodes(xml.getElementsByTagName(MigrationConstants.NODES_NODE).item(0));

        boolean nodeRemoved = false;

        for (Node currNode : nodes) {
            //Once we have removed the node, renumber the nodes after it to fill in the gap
            if (nodeRemoved) {
                int currIndex = getIndex(currNode);
                xml.renameNode(currNode, null, MigrationConstants.NODE_PREFIX + (currIndex - 1));
            } else if (getIndex(currNode) == nodeIndex) {
                currNode.getParentNode().removeChild(currNode);
                nodeRemoved = true;
            }
        }
    }

    private void removeTransition(Document xml) {
        List<Node> nodes = XmlUtil.getChildElementNodes(xml.getElementsByTagName(MigrationConstants.TRANSITIONS_NODE).item(0));
        Node lastTransitionNode = nodes.get(nodes.size() - 1);
        lastTransitionNode.getParentNode().removeChild(lastTransitionNode);
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

    private int getIndex(Node workflowNode) {
        String nodeName = workflowNode.getNodeName(); //Current node name will be in a format like node7
        return Integer.parseInt(nodeName.substring(4)); //Remove the node prefix to return the node index
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

    private void addStepToVarFile(WorkflowStep workflowStep, WorkflowModel model) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        Document modelXml = XmlUtil.loadXml(model.getRuntimeFile());

        addProcessNode(workflowStep, modelXml);
        addTransitionNode(modelXml);

        XmlUtil.writeXml(modelXml, model.getRuntimeFile());
    }

    /**
     * The final process node in a runtime model is END.  Therefore, we actually need to add our step just before the
     * final node.
     */
    private void addProcessNode(WorkflowStep workflowStep, Document modelXml) {
        Node nodesNode = modelXml.getElementsByTagName(MigrationConstants.NODES_NODE).item(0);
        List<Node> nodes = XmlUtil.getChildElementNodes(nodesNode);

        Element workflowNode = getNewProcessNode(workflowStep, modelXml, nodes);

        Node endNode = nodes.get(nodes.size()-1); //Get the last item in the list
        modelXml.renameNode(endNode, null, "node" + nodes.size()); //Increment the node name for the END node

        nodesNode.insertBefore(workflowNode, endNode);
    }

    private Element getNewProcessNode(WorkflowStep workflowStep, Document modelXml, List<Node> nodes) {
        int finalNode = nodes.size() - 1;

        Element metadataNode = modelXml.createElement(MigrationConstants.METADATA_XML_NODE);
        metadataNode.setAttribute(MigrationConstants.JCR_PRIMARY_TYPE_PROP, MigrationConstants.NT_UNSTRUCTURED_TYPE_VALUE);
        metadataNode.setAttribute(MigrationConstants.PROCESS_PROP, workflowStep.getProcess());
        metadataNode.setAttribute(MigrationConstants.PROCESS_AUTO_ADVANCE_PROP, "true");

        Element workflowNode = modelXml.createElement(MigrationConstants.NODE_PREFIX + finalNode);
        workflowNode.setAttribute(MigrationConstants.JCR_PRIMARY_TYPE_PROP, MigrationConstants.WORKFLOW_NODE_TYPE_VALUE);
        workflowNode.setAttribute(MigrationConstants.DESCRIPTION_PROP, workflowStep.getDescription());
        workflowNode.setAttribute(MigrationConstants.TITLE_PROP, workflowStep.getTitle());
        workflowNode.setAttribute(MigrationConstants.TYPE_PROP, MigrationConstants.PROCESS_PROP);
        workflowNode.appendChild(metadataNode);

        return workflowNode;
    }

    private void addTransitionNode(Document modelXml) {
        Node transitionsNode = modelXml.getElementsByTagName(MigrationConstants.TRANSITIONS_NODE).item(0);
        List<Node> nodes = XmlUtil.getChildElementNodes(transitionsNode);
        int lastNode = nodes.size();
        int nextNode = lastNode + 1;
        String transitionNodeName = MigrationConstants.NODE_PREFIX + lastNode + MigrationConstants.NODE_SPACE + MigrationConstants.NODE_PREFIX + nextNode;

        Element metadataNode = modelXml.createElement(MigrationConstants.METADATA_XML_NODE);
        metadataNode.setAttribute(MigrationConstants.JCR_PRIMARY_TYPE_PROP, MigrationConstants.NT_UNSTRUCTURED_TYPE_VALUE);

        Element transitionNode = modelXml.createElement(transitionNodeName);
        transitionNode.setAttribute(MigrationConstants.JCR_PRIMARY_TYPE_PROP, MigrationConstants.WORKFLOW_TRANSITION_TYPE_VALUE);
        transitionNode.setAttribute(MigrationConstants.FROM_PROP, MigrationConstants.NODE_PREFIX + lastNode);
        transitionNode.setAttribute(MigrationConstants.TO_PROP, MigrationConstants.NODE_PREFIX + nextNode);
        transitionNode.appendChild(metadataNode);

        transitionsNode.appendChild(transitionNode);
    }
}

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

package com.adobe.skyline.migration.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.dao.WorkflowLauncherDAO;
import com.adobe.skyline.migration.dao.WorkflowModelDAO;
import com.adobe.skyline.migration.exception.CustomerDataException;
import com.adobe.skyline.migration.exception.MigrationRuntimeException;
import com.adobe.skyline.migration.model.workflow.Workflow;
import com.adobe.skyline.migration.model.workflow.WorkflowProject;
import com.adobe.skyline.migration.util.Logger;
import com.adobe.skyline.migration.util.XmlUtil;
import com.adobe.skyline.migration.util.file.FileQueryService;

/**
 * Loads the customer project from disk and creates various model objects to represent the existing workflows as configured.
 */
public class CustomerProjectLoader {

    private XPath xPath;
    private FileQueryService queryService;
    private WorkflowLauncherDAO launcherDAO;
    private WorkflowModelDAO modelDAO;

    public CustomerProjectLoader(FileQueryService queryService, WorkflowLauncherDAO launcherDAO, WorkflowModelDAO modelDAO) {
        this.xPath = XPathFactory.newInstance().newXPath();
        this.queryService = queryService;
        this.launcherDAO = launcherDAO;
        this.modelDAO = modelDAO;
    }

    public List<WorkflowProject> getWorkflowProjects(String customerProjectPath) throws CustomerDataException {
        Logger.DEBUG("Loading projects for " + customerProjectPath);

        List<WorkflowProject> projects = new ArrayList<>();

        File customerPom = new File(customerProjectPath + File.separator + MigrationConstants.POM_XML);

        if (!customerPom.exists()) {
            throw new CustomerDataException("Unable to find a the specified pom file: " + customerPom.getPath());
        }

        for (String moduleName : getModuleNames(customerPom)) {
            String modulePath = customerProjectPath + File.separator + moduleName;
            Logger.DEBUG("Module found at " + modulePath);

            Document moduleXml = tryXmlLoad(new File(modulePath + "/" + MigrationConstants.POM_XML));

            if (isContentPackage(moduleXml)) {
                List<String> wfLauncherPaths = getLauncherPaths(modulePath);
                List<String> wfModelPaths = getModelPaths(modulePath);

                if (wfLauncherPaths.size() > 0 || wfModelPaths.size() > 0) {
                    WorkflowProject project = createCustomerProject(modulePath, wfLauncherPaths, wfModelPaths);
                    projects.add(project);
                }
            }
        }

        if (projects.size() < 1) {
            throw new CustomerDataException("Unable to find a project that contains asset workflow configurations.  If you have not made asset workflow customizations, this tool is unnecessary.");
        } else {
            return projects;
        }
    }

    public boolean isCloudManagerReady(String customerProjectPath) throws CustomerDataException {
        File customerPom = new File(customerProjectPath + File.separator + MigrationConstants.POM_XML);

        boolean hasPackageTypesDeclared = false;
        boolean hasContainerProject = false;

        for (String moduleName : getModuleNames(customerPom)) {
            String modulePath = customerProjectPath + File.separator + moduleName;
            Logger.DEBUG("Module found at " + modulePath);

            Document moduleXml = tryXmlLoad(new File(modulePath + "/" + MigrationConstants.POM_XML));

            try {
                if (hasPackageType(moduleXml)) {
                    hasPackageTypesDeclared = true;
                }

                if (isContainerProject(moduleXml)) {
                    hasContainerProject = true;
                }
            } catch (Exception e) {
                throw new MigrationRuntimeException("Exception occurred while checking Maven project for new-style content packages.", e);
            }
        }

        return hasPackageTypesDeclared && hasContainerProject;
    }

    public String getContainerProjectPath(String customerProjectPath) throws CustomerDataException {
        File customerPom = new File(customerProjectPath + File.separator + MigrationConstants.POM_XML);

        if (!customerPom.exists()) {
            throw new CustomerDataException("Unable to find a the specified pom file: " + customerPom.getPath());
        }

        for (String moduleName : getModuleNames(customerPom)) {
            String modulePath = customerProjectPath + File.separator + moduleName;
            Logger.DEBUG("Module found at " + modulePath);

            try {
                Document moduleXml = tryXmlLoad(new File(modulePath + "/" + MigrationConstants.POM_XML));
                if (isContentPackage(moduleXml) && isContainerProject(moduleXml)) {
                    return modulePath;
                }
            } catch (Exception e) {
                throw new MigrationRuntimeException("Exception occurred when searching for the container content package.", e);
            }
        }

        return null;
    }

    private List<String> getModuleNames(File customerPom) throws CustomerDataException {
        List<String> moduleNames = new ArrayList<>();

        Document customerXml = tryXmlLoad(customerPom);
        NodeList moduleNodes = customerXml.getDocumentElement().getElementsByTagName(MigrationConstants.MODULE_TAG_NAME);

        for (int i = 0; i < moduleNodes.getLength(); i++) {
            moduleNames.add(moduleNodes.item(i).getFirstChild().getTextContent());
        }

        return moduleNames;
    }

    private boolean isContentPackage(Document moduleXml) throws CustomerDataException{
        NodeList packagingNodes = moduleXml.getElementsByTagName(MigrationConstants.PACKAGING_TAG_NAME);
        if (packagingNodes.getLength() > 0) {
            String packaging = packagingNodes.item(0).getTextContent();
            return packaging.equals(MigrationConstants.CONTENT_PACKAGE_PACKAGING);
        } else {
            return false;
        }
    }

    private List<String> getLauncherPaths(String modulePath) throws CustomerDataException {
        List<String> wfConfigPaths = new ArrayList<>();

        List<String> filterPaths = getFilterPaths(modulePath);

        for (String filterPath : filterPaths) {
            if (filterPath.startsWith(MigrationConstants.CONF_ROOT) || filterPath.startsWith(MigrationConstants.ETC_PATH)) {
                String absolutePath = modulePath + MigrationConstants.PATH_TO_JCR_ROOT + filterPath;
                List<String> launchers = queryService.findFilesByNodeProperty(MigrationConstants.JCR_PRIMARY_TYPE_PROP, MigrationConstants.WORKFLOW_LAUNCHER_TYPE_VALUE, new File(absolutePath));
                wfConfigPaths.addAll(launchers);
            }
        }

        return wfConfigPaths;
    }

    private List<String> getModelPaths(String modulePath) throws CustomerDataException {
        Logger.DEBUG("Getting model paths for module at: " + modulePath);
        List<String> wfConfigPaths = new ArrayList<>();

        List<String> filterPaths = getFilterPaths(modulePath);

        for (String filterPath : filterPaths) {
            if (filterPath.startsWith(MigrationConstants.CONF_ROOT) || filterPath.startsWith(MigrationConstants.ETC_PATH)) {
                String absolutePath = modulePath + MigrationConstants.PATH_TO_JCR_ROOT + filterPath;
                List<String> models =  queryService.findFilesByNodeProperty(MigrationConstants.SLING_RESOURCE_TYPE_PROP, MigrationConstants.WORKFLOW_MODEL_RESOURCE_TYPE_VALUE, new File(absolutePath));

                for (String fullPath : models) {
                    //Store the relative model path, to agree with the launcher configuration
                    Logger.DEBUG("Getting relative model path for fullPath: " + fullPath);
                    Logger.DEBUG("Getting relative model path for modulePath: " + modulePath);
                    String relativePath = getRelativeModelPath(fullPath, modulePath);
                    Logger.DEBUG("relativePath: " + relativePath);
                    wfConfigPaths.add(relativePath);
                }
            }
        }

        return wfConfigPaths;
    }

    private String getRelativeModelPath(String fullPath, String modulePath) {
        String pathToJcrRoot = modulePath + MigrationConstants.PATH_TO_JCR_ROOT;
        String relativePath = fullPath.substring(fullPath.indexOf(pathToJcrRoot) + pathToJcrRoot.length());

        return relativePath
                .replace("/jcr:content/model", "")
                .replace("/.content.xml", "");
    }

    private List<String> getFilterPaths(String modulePath) throws CustomerDataException {
        List<String> filterPaths = new ArrayList<>();

        File filterFile = new File(modulePath + MigrationConstants.PATH_TO_FILTER_XML);

        if (filterFile.exists()) {
            Document filterXml = tryXmlLoad(filterFile);
            NodeList filterNodes = filterXml.getDocumentElement().getElementsByTagName(MigrationConstants.FILTER_TAG_NAME);

            for (int i = 0; i < filterNodes.getLength(); i++) {
                String filterRoot = filterNodes.item(i).getAttributes().getNamedItem(MigrationConstants.ROOT_PROPERTY).getTextContent();
                filterPaths.add(filterRoot);
            }
        }

        return filterPaths;
    }

    private Document tryXmlLoad(File xmlFile) throws CustomerDataException {
        try {
            return XmlUtil.loadXml(xmlFile);
        } catch (Exception e) {
            throw new CustomerDataException("Unable to read the xml file at " + xmlFile.getPath(), e);
        }
    }

    private WorkflowProject createCustomerProject(String modulePath, List<String> wfLauncherPaths, List<String> workflowModelPaths) {
        WorkflowProject project = new WorkflowProject();


        WorkflowBuilder workflowBuilder = new WorkflowBuilder(launcherDAO, modelDAO, modulePath);
        List<Workflow> workflows = workflowBuilder.buildWorkflows(wfLauncherPaths, workflowModelPaths);
        project.setWorkflows(workflows);

        return project;
    }

    private boolean isContainerProject(Document moduleXml) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        return hasEmbeddeds(moduleXml) && !hasPackageType(moduleXml);
    }

    private boolean hasEmbeddeds(Document moduleXml) throws XPathExpressionException {
        String embeddedsExpr = "//plugin[artifactId='filevault-package-maven-plugin']/configuration/embeddeds";
        NodeList embeddedsList = (NodeList) xPath.compile(embeddedsExpr).evaluate(moduleXml, XPathConstants.NODESET);
       return embeddedsList.getLength() > 0;
    }

    private boolean hasPackageType(Document moduleXml) throws XPathExpressionException {
        String packageTypeExpr = "//plugin[artifactId='filevault-package-maven-plugin']/configuration/packageType";
        NodeList packageTypeList = (NodeList) xPath.compile(packageTypeExpr).evaluate(moduleXml, XPathConstants.NODESET);
        return packageTypeList.getLength() > 0;
    }
}

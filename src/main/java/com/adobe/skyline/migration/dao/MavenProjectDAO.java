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
import java.util.jar.JarFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.exception.CustomerDataException;
import com.adobe.skyline.migration.exception.ProjectCreationException;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.util.XmlUtil;
import com.adobe.skyline.migration.util.file.FileUtil;

public class MavenProjectDAO {

    private String existingProjectPath;
    private ChangeTrackingService changeTracker;
    private ContainerProjectDAO containerProjectDAO;

    private File reactorPomFile;
    private Document reactorPomXml;

    private String reactorGroupId;
    private String reactorArtifactId;
    private String reactorVersion;

    /**
     * Overloaded constructor to support use cases where the customer has already migrated to the new cloud service
     * structure for their content packages as well as cases where they haven't.
     */
    public MavenProjectDAO(String existingProjectPath, ChangeTrackingService changeTracker, ContainerProjectDAO containerProjectDao) throws CustomerDataException {
        this.containerProjectDAO = containerProjectDao;
        init(existingProjectPath, changeTracker);
    }

    public MavenProjectDAO(String existingProjectPath, ChangeTrackingService changeTracker) throws CustomerDataException {
        init(existingProjectPath, changeTracker);
    }

    /**
     * Check to see if a project already exists.  This is used to determine whether it should be created.
     */
    public boolean projectExists(String projectName) {
        String fullPath = this.existingProjectPath + File.separator + projectName;
        return (new File(fullPath)).exists();
    }

    /**
     * Create the projects where we will be outputting our content.
     */
    public void createProject(String projectName) throws ProjectCreationException {
        try {
            String targetPath = this.existingProjectPath + File.separator + projectName;
            String templatePath = "";

            if (projectName.equals(MigrationConstants.MIGRATION_PROJECT_CONTENT)) {
                templatePath = MigrationConstants.TEMPLATE_PROJECT_CONTENT_PATH;
            } else if (projectName.equals(MigrationConstants.MIGRATION_PROJECT_APPS)) {
                templatePath = MigrationConstants.TEMPLATE_PROJECT_APPS_PATH;
            }

            changeTracker.trackProjectCreated(projectName);
            copyProject(templatePath, targetPath);
            addProjectToReactor(projectName);

            if (containerProjectDAO != null) {
                containerProjectDAO.addProject(reactorGroupId, projectName);
            }
        } catch (IOException | TransformerException e) {
            throw new ProjectCreationException(e);
        }
    }

    private void init(String existingProjectPath, ChangeTrackingService changeTracker) throws CustomerDataException {
        try {
            this.existingProjectPath = existingProjectPath;
            this.changeTracker = changeTracker;
            this.reactorPomFile = new File(existingProjectPath, MigrationConstants.POM_XML);
            this.reactorPomXml = XmlUtil.loadXml(this.reactorPomFile);

            Element projectTag = reactorPomXml.getDocumentElement();
            this.reactorGroupId = projectTag.getElementsByTagName(MigrationConstants.GROUPID_TAG_NAME).item(0).getTextContent();
            this.reactorArtifactId = projectTag.getElementsByTagName(MigrationConstants.ARTIFACTID_TAG_NAME).item(0).getTextContent();
            this.reactorVersion = projectTag.getElementsByTagName(MigrationConstants.VERSION_TAG_NAME).item(0).getTextContent();
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new CustomerDataException("Unable to parse reactor POM file.", e);
        }
    }

    private void copyProject(String templatePath, String destPath) throws IOException {
        String filePath = this.getClass().getResource(templatePath).getFile();
        File destination = new File(destPath);

        if (filePath.startsWith("file")) {
            //We are running from the jar file
            String jarPath = filePath.substring(5, filePath.indexOf("!")); //Trim "file: from the beginning of the string"
            jarPath = jarPath.replaceAll("%20", " "); //Unencode the URL String that was returned by getResource()
            JarFile source = new JarFile(jarPath);
            FileUtil.copyDirectoryFromJar(source, templatePath, destPath);
        } else {
            //We are running in the IDE or through unit tests
            File source = new File(filePath);
            FileUtil.copyDirectoryRecursively(source, destination);
        }

        File pom = new File(destination, MigrationConstants.POM_XML);
        replaceParentProperties(pom);
    }

    private void replaceParentProperties(File pom) throws IOException {
        FileUtil.findAndReplaceInFile(pom, "\\$\\{PARENT-GROUPID\\}", reactorGroupId);
        FileUtil.findAndReplaceInFile(pom, "\\$\\{PARENT-ARTIFACTID\\}", reactorArtifactId);
        FileUtil.findAndReplaceInFile(pom, "\\$\\{PARENT-VERSION\\}", reactorVersion);
    }

    private void addProjectToReactor(String projectName) throws TransformerException, IOException {
        Node modulesNode = reactorPomXml.getElementsByTagName(MigrationConstants.MODULES_TAG_NAME).item(0); //There should only be one modules list per POM

        Element contentModule = reactorPomXml.createElement(MigrationConstants.MODULE_TAG_NAME);
        contentModule.setTextContent(projectName);
        modulesNode.appendChild(contentModule);

        XmlUtil.writeXml(reactorPomXml, reactorPomFile);
    }

}

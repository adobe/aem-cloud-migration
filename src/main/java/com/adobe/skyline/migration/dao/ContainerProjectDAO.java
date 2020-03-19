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

package com.adobe.skyline.migration.dao;

import java.io.File;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.exception.MigrationRuntimeException;
import com.adobe.skyline.migration.util.XmlUtil;

public class ContainerProjectDAO {

    private XPath xPath;

    private File allPomFile;
    private Document allPomXml;
    private FilterFileDAO filterDAO;

    public ContainerProjectDAO(String projectRoot) {
        this.xPath = XPathFactory.newInstance().newXPath();

        try {
            this.allPomFile = new File(projectRoot, MigrationConstants.POM_XML);
            this.allPomXml = XmlUtil.loadXml(this.allPomFile);
            this.filterDAO = new FilterFileDAO(projectRoot);
        } catch (Exception e) {
            throw new MigrationRuntimeException("Failed to initialize ContainerProjectDAO.", e);
        }
    }

    public void addProject(String groupId, String projectName) {
        try {
            addProjectToEmbeddeds(groupId, projectName);
            addProjectToDependencies(groupId, projectName);
            XmlUtil.writeXml(allPomXml, allPomFile);
            filterDAO.addPath(MigrationConstants.MIGRATION_PACKAGE_PATH); //FilterFileDAO checks for dupes, so no need to get fancy here
        } catch (Exception e) {
            throw new MigrationRuntimeException("Exception occurred when adding a new project to the container project POM.", e);
        }
    }

    private void addProjectToEmbeddeds(String groupId, String projectName) throws XPathExpressionException {
        String expression = "//embeddeds";
        NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(allPomXml, XPathConstants.NODESET);
        Node embeddedNode = nodeList.item(0);

        Element projectEmbed = allPomXml.createElement("embedded");
        embeddedNode.appendChild(projectEmbed);

        Element groupIdEl = allPomXml.createElement("groupId");
        groupIdEl.appendChild(allPomXml.createTextNode(groupId));
        projectEmbed.appendChild(groupIdEl);

        Element artifactId = allPomXml.createElement("artifactId");
        artifactId.appendChild(allPomXml.createTextNode(projectName));
        projectEmbed.appendChild(artifactId);

        Element type = allPomXml.createElement("type");
        type.appendChild(allPomXml.createTextNode("zip"));
        projectEmbed.appendChild(type);

        String installPath = MigrationConstants.MIGRATION_PACKAGE_PATH;
        if (projectName.equals(MigrationConstants.MIGRATION_PROJECT_CONTENT)) {
            installPath += "/content/install";
        } else if (projectName.equals(MigrationConstants.MIGRATION_PROJECT_APPS)) {
            installPath += "/application/install";
        }

        Element target = allPomXml.createElement("target");
        target.appendChild(allPomXml.createTextNode(installPath));
        projectEmbed.appendChild(target);
    }

    private void addProjectToDependencies(String groupId, String projectName) throws XPathExpressionException {
        String expression = "/project/dependencies";
        NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(allPomXml, XPathConstants.NODESET);
        Node dependenciesNode = nodeList.item(0);

        Element dependency = allPomXml.createElement("dependency");
        dependenciesNode.appendChild(dependency);

        Element groupIdEl = allPomXml.createElement("groupId");
        groupIdEl.appendChild(allPomXml.createTextNode(groupId));
        dependency.appendChild(groupIdEl);

        Element artifactId = allPomXml.createElement("artifactId");
        artifactId.appendChild(allPomXml.createTextNode(projectName));
        dependency.appendChild(artifactId);

        Element version = allPomXml.createElement("version");
        version.appendChild(allPomXml.createTextNode("${project.version}"));
        dependency.appendChild(version);

        Element type = allPomXml.createElement("type");
        type.appendChild(allPomXml.createTextNode("zip"));
        dependency.appendChild(type);
    }
}

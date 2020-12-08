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
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.exception.MigrationRuntimeException;
import com.adobe.skyline.migration.util.XmlUtil;

/**
 * An object to abstract reading from and writing to filter files in Maven content package projects.
 */
public class FilterFileDAO {

    private File filterFile;

    public FilterFileDAO(String projectPath) {
        this.filterFile = new File(projectPath + MigrationConstants.PATH_TO_FILTER_XML);
    }

    public boolean hasPath(String path) {
        try {
            Document filterXml = XmlUtil.loadXml(filterFile);
            Element workspaceFilterElement = filterXml.getDocumentElement();

            return hasEntry(workspaceFilterElement, path);
        } catch (Exception e) {
            throw new MigrationRuntimeException(e);
        }
    }

    public void addPath(String path) {
        try {
            Document filterXml = XmlUtil.loadXml(filterFile);
            Element workspaceFilterElement = filterXml.getDocumentElement();

            if (!hasEntry(workspaceFilterElement, path)) {
                addEntry(path, filterXml, workspaceFilterElement);
            }
        } catch (Exception e) {
            throw new MigrationRuntimeException(e);
        }
    }

    public List<String> findPathsWith(Pattern pattern) {
        try {
            Document filterXml = XmlUtil.loadXml(filterFile);
            Element workspaceFilterElement = filterXml.getDocumentElement();

            return match(workspaceFilterElement, pattern);
        } catch (Exception e) {
            throw new MigrationRuntimeException(e);
        }
    }

    public void removePath(String entry) {
        try {
            Document filterXml = XmlUtil.loadXml(filterFile);
            Element workspaceFilterElement = filterXml.getDocumentElement();

            List<Node> filterNodes = XmlUtil.getChildElementNodes(workspaceFilterElement);

            for (Node currNode : filterNodes) {
                String path = ((Element) currNode).getAttribute(MigrationConstants.ROOT_PROPERTY);
                if (entry.equals(path)) {
                    workspaceFilterElement.removeChild(currNode);
                    XmlUtil.writeXml(filterXml, filterFile);
                }
            }

        } catch (Exception e) {
            throw new MigrationRuntimeException(e);
        }
    }


    private boolean hasEntry(Element workspaceFilterElement, String entry) {
        List<Node> filterNodes = XmlUtil.getChildElementNodes(workspaceFilterElement);

        boolean matched = false;
        for (Node currNode : filterNodes) {
            String path = ((Element) currNode).getAttribute(MigrationConstants.ROOT_PROPERTY);
            if (entry.equals(path)) {
                matched = true;
                break;
            }
        }

        return matched;
    }

    private List<String> match(Element workspaceFilterElement, Pattern pattern) {
        List<Node> filterNodes = XmlUtil.getChildElementNodes(workspaceFilterElement);

        List<String> matches = new ArrayList<>();

        for (Node currNode : filterNodes) {
            String path = ((Element) currNode).getAttribute(MigrationConstants.ROOT_PROPERTY);
            if (pattern.matcher(path).matches()) {
                matches.add(path);
            }
        }

        return matches;
    }

    private void addEntry(String path, Document filterXml, Element workspaceFilterElement) throws TransformerException, IOException {
        Element filterNode = filterXml.createElement(MigrationConstants.FILTER_TAG_NAME);
        filterNode.setAttribute(MigrationConstants.ROOT_PROPERTY, path);
        workspaceFilterElement.appendChild(filterNode);
        XmlUtil.writeXml(filterXml, filterFile);
    }
}

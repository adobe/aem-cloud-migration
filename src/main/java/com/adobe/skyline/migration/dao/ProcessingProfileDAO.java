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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.adobe.skyline.migration.model.VideoProfileConfig;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.exception.MigrationRuntimeException;
import com.adobe.skyline.migration.model.ProcessingProfile;
import com.adobe.skyline.migration.model.RenditionConfig;
import com.adobe.skyline.migration.util.JcrUtil;
import com.adobe.skyline.migration.util.StringUtil;
import com.adobe.skyline.migration.util.XmlUtil;

/**
 * An object to abstract reading processing profiles from and writing them to disk.
 */
public class ProcessingProfileDAO {

    private File rootPage;

    public ProcessingProfileDAO(String projectPath) {
        this.rootPage = new File(projectPath + "/" + MigrationConstants.PROCESSING_PROFILE_DISK_PATH + "/" + MigrationConstants.CONTENT_XML);
    }

    public void addProfile(ProcessingProfile profile) {
        try {
            //Only create the config file if this method has been called.  We don't want to create an empty configuration in the constructor.
            if (!rootPage.exists()) {
                initConfig();
            }

            File profileFile = createProfileFile(profile);

            for (RenditionConfig rendition : profile.getRenditions()) {
                createRenditionFile(profileFile, rendition);
            }

        } catch (Exception e) {
            throw new MigrationRuntimeException(e);
        }
    }

    private void initConfig() throws ParserConfigurationException, TransformerException, IOException {
        Document pageDoc = XmlUtil.createXml();

        Element rootEl = createRootElement(pageDoc);

        Element jcrContentNode = addJcrContentNode(pageDoc, rootEl);
        jcrContentNode.setAttribute(MigrationConstants.MERGE_LIST_PROPERTY, MigrationConstants.TRUE_VALUE);

        createXml(pageDoc, rootPage);
    }

    private File createProfileFile(ProcessingProfile profile) throws ParserConfigurationException, IOException, TransformerException {
        Document profileXml = XmlUtil.createXml();

        Element rootEl = createRootElement(profileXml);
        addSlingNamespace(profileXml, rootEl);

        Element jcrContentNode = addJcrContentNode(profileXml, rootEl);
        jcrContentNode.setAttribute(MigrationConstants.JCR_TITLE_PROP, profile.getName());
        jcrContentNode.setAttribute(MigrationConstants.SLING_RESOURCE_TYPE_PROP, MigrationConstants.PROCESSING_PROFILE_RESOURCE_TYPE);

        String safeName = getUniqueNodeName(profile.getName(), rootPage.getParent());

        File profileFile = new File(rootPage.getParent() + "/" + safeName + "/" + MigrationConstants.CONTENT_XML);
        createXml(profileXml, profileFile);
        return profileFile;
    }

    private void createRenditionFile(File profileFile, RenditionConfig rendition) throws ParserConfigurationException, IOException, TransformerException {
        Document renditionXml = XmlUtil.createXml();

        Element rootEl = createRootElement(renditionXml);
        addSlingNamespace(renditionXml, rootEl);

        Element jcrContentNode = addJcrContentNode(renditionXml, rootEl);
        if (rendition instanceof VideoProfileConfig) {
            jcrContentNode.setAttribute(MigrationConstants.SLING_RESOURCE_TYPE_PROP, MigrationConstants.VIDEO_RESOURCE_TYPE);
            VideoProfileConfig videoProfileConfig = (VideoProfileConfig) rendition;
            jcrContentNode.setAttribute(MigrationConstants.BITRATE_PROP, String.valueOf(videoProfileConfig.getBitRate()));
            jcrContentNode.setAttribute(MigrationConstants.CODEC_PROP, videoProfileConfig.getCodec());
        } else {
            jcrContentNode.setAttribute(MigrationConstants.SLING_RESOURCE_TYPE_PROP, MigrationConstants.RENDITION_RESOURCE_TYPE);
        }
        jcrContentNode.setAttribute(MigrationConstants.FMT_PROP, rendition.getFormat());
        jcrContentNode.setAttribute(MigrationConstants.NAME_PROP, rendition.getFileName());
        jcrContentNode.setAttribute(MigrationConstants.WIDTH_PROP, String.valueOf(rendition.getWidth()));
        jcrContentNode.setAttribute(MigrationConstants.HEIGHT_PROP, String.valueOf(rendition.getHeight()));
        jcrContentNode.setAttribute(MigrationConstants.EXCLUDE_MIMETYPES_PROP, StringUtil.concatenateCollectionToCsv(rendition.getExcludeMimeTypes()));
        jcrContentNode.setAttribute(MigrationConstants.INCLUDE_MIMETYPES_PROP, StringUtil.concatenateCollectionToCsv(rendition.getIncludeMimeTypes()));
        if (rendition.getQuality() > 0) {
            jcrContentNode.setAttribute(MigrationConstants.QUALITY_PROP, String.valueOf(rendition.getQuality()));
        }

        String safeName = getUniqueNodeName(rendition.getNodeName(), profileFile.getParent());

        File renditionFile = new File(profileFile.getParent() + "/" + safeName + "/" + MigrationConstants.CONTENT_XML);
        createXml(renditionXml, renditionFile);
    }

    private Element createRootElement(Document doc) {
        Element rootEl = doc.createElement(MigrationConstants.JCR_ROOT_NODE);
        doc.appendChild(rootEl);

        addCommonNamespaces(doc, rootEl);
        addCqPagePrimaryType(doc, rootEl);

        return rootEl;
    }

    private void addCommonNamespaces(Document doc, Element el) {
        addCqNamespace(doc, el);
        addJcrNamespace(doc, el);
        addNtNamespace(doc, el);
    }

    private void addSlingNamespace(Document doc, Element el) {
        Attr nsSling = doc.createAttribute(MigrationConstants.NS_SLING);
        nsSling.setValue(MigrationConstants.NS_SLING_VALUE);
        el.setAttributeNode(nsSling);
    }

    private void addCqNamespace(Document doc, Element el) {
        Attr nsSling = doc.createAttribute(MigrationConstants.NS_CQ);
        nsSling.setValue(MigrationConstants.NS_CQ_VALUE);
        el.setAttributeNode(nsSling);
    }

    private void addJcrNamespace(Document doc, Element el) {
        Attr nsSling = doc.createAttribute(MigrationConstants.NS_JCR);
        nsSling.setValue(MigrationConstants.NS_JCR_VALUE);
        el.setAttributeNode(nsSling);
    }

    private void addNtNamespace(Document doc, Element el) {
        Attr nsSling = doc.createAttribute(MigrationConstants.NS_NT);
        nsSling.setValue(MigrationConstants.NS_NT_VALUE);
        el.setAttributeNode(nsSling);
    }

    private void addCqPagePrimaryType(Document doc, Element el) {
        Attr primType = doc.createAttribute(MigrationConstants.JCR_PRIMARY_TYPE_PROP);
        primType.setValue(MigrationConstants.CQ_PAGE_TYPE);
        el.setAttributeNode(primType);
    }

    private Element addJcrContentNode(Document doc, Element rootEl) {
        Element jcrContentNode = doc.createElement(MigrationConstants.JCR_CONTENT);
        jcrContentNode.setAttribute(MigrationConstants.JCR_PRIMARY_TYPE_PROP, MigrationConstants.NT_UNSTRUCTURED_TYPE_VALUE);
        rootEl.appendChild(jcrContentNode);
        return jcrContentNode;
    }

    private String getUniqueNodeName(String name, String path) {
        String safeName = JcrUtil.getJcrSafeNodeName(name);

        //Handle duplicate names
        int idx = 1;
        String currName = safeName;
        while (true) {
            File profileRoot = new File(path + "/" + currName);
            if (profileRoot.exists()) {
                currName = safeName + "-" + idx;
                idx++;
            } else {
                safeName = currName;
                break;
            }
        }

        return safeName;
    }

    private void createXml(Document xml, File file) throws IOException, TransformerException {
        file.getParentFile().mkdirs();
        file.createNewFile();
        XmlUtil.writeXml(xml, file);
    }
}

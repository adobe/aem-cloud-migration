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

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.model.ProcessingProfile;
import com.adobe.skyline.migration.model.RenditionConfig;
import com.adobe.skyline.migration.testutils.TestConstants;
import com.adobe.skyline.migration.util.XmlUtil;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class ProcessingProfileDAOTest extends SkylineMigrationBaseTest {

    private static final String PROFILE_NAME = "TestProfile";

    private String projectRootPath;

    private ProcessingProfileDAO dao;

    @Before
    public void setUp() {
        super.setUp();

        File tempProjectRoot = projectLoader.copyConfProjectToTemp(temp);
        this.projectRootPath = tempProjectRoot + "/" + TestConstants.CONF_WORKFLOW_PROJECT_NAME;

        this.dao = new ProcessingProfileDAO(projectRootPath);
    }

    @Test
    public void testProcessingRootPageCreated() throws ParserConfigurationException, SAXException, IOException {
        File profileRootFile = new File(projectRootPath + "/" + MigrationConstants.PROCESSING_PROFILE_DISK_PATH + "/.content.xml");
        assertFalse(profileRootFile.exists());

        ProcessingProfile profile = createTestProfile();
        dao.addProfile(profile);

        Document xmlDoc = XmlUtil.loadXml(profileRootFile);
        Element rootElem = xmlDoc.getDocumentElement();
        assertEquals(MigrationConstants.NS_CQ_VALUE, rootElem.getAttribute(MigrationConstants.NS_CQ));
        assertEquals(MigrationConstants.NS_JCR_VALUE, rootElem.getAttribute(MigrationConstants.NS_JCR));
        assertEquals(MigrationConstants.NS_NT_VALUE, rootElem.getAttribute(MigrationConstants.NS_NT));

        assertEquals(MigrationConstants.CQ_PAGE_TYPE, rootElem.getAttribute(MigrationConstants.JCR_PRIMARY_TYPE_PROP));

        List<Node> childNodes = XmlUtil.getChildElementNodes(rootElem);
        boolean jcrContentExists = false;
        for (Node node:childNodes) {
            if (node.getNodeName().equals("jcr:content")) {
                jcrContentExists = true;
                assertEquals("{Boolean}true", ((Element) node).getAttribute(MigrationConstants.MERGE_LIST_PROPERTY));
            }
        }
        assertTrue(jcrContentExists);
    }

    @Test
    public void testProfileAdded() throws ParserConfigurationException, SAXException, IOException {
        File profilePath = new File(projectRootPath + "/" + MigrationConstants.PROCESSING_PROFILE_DISK_PATH + "/" + PROFILE_NAME);
        assertFalse(profilePath.exists());

        ProcessingProfile profile = createTestProfile();
        dao.addProfile(profile);

        boolean contentNodeExists = false;
        int numRenditions = 0;

        for (File child:profilePath.listFiles()) {
            //Validate the processing profile node
            if (child.getName().equals(".content.xml")) {
                Document profileXml = XmlUtil.loadXml(child);
                Element rootElem = profileXml.getDocumentElement();
                List<Node> childNodes = XmlUtil.getChildElementNodes(rootElem);

                for (Node node:childNodes) {
                    if (node.getNodeName().equals("jcr:content")) {
                        contentNodeExists = true;
                        assertEquals(PROFILE_NAME, ((Element) node).getAttribute(MigrationConstants.JCR_TITLE_PROP));
                        assertEquals(MigrationConstants.PROCESSING_PROFILE_RESOURCE_TYPE, ((Element) node).getAttribute(MigrationConstants.SLING_RESOURCE_TYPE_PROP));
                    }
                }
            } else {
                //is a rendition, recur into its child .content.xml to inspect it
                File renditionContent = new File(child, ".content.xml");

                Document xml = XmlUtil.loadXml(renditionContent);
                Element rootElem = xml.getDocumentElement();
                List<Node> childNodes = XmlUtil.getChildElementNodes(rootElem);

                for (Node node:childNodes) {
                    if (node.getNodeName().equals("jcr:content")) {
                        assertEquals("image/*", ((Element) node).getAttribute(MigrationConstants.INCLUDE_MIMETYPES_PROP));
                        assertTrue(((Element) node).getAttribute(MigrationConstants.EXCLUDE_MIMETYPES_PROP).contains("video/*"));
                        assertTrue(((Element) node).getAttribute(MigrationConstants.EXCLUDE_MIMETYPES_PROP).contains("audio/*"));
                        if ("cq5dam.thumbnail.100.100.png".equals(((Element) node).getAttribute(MigrationConstants.NAME_PROP))) {
                            numRenditions++;
                            assertEquals("100", ((Element) node).getAttribute(MigrationConstants.WIDTH_PROP));
                            assertEquals("100", ((Element) node).getAttribute(MigrationConstants.HEIGHT_PROP));
                            assertEquals("png", ((Element) node).getAttribute(MigrationConstants.FMT_PROP));
                            assertEquals(MigrationConstants.RENDITION_RESOURCE_TYPE, ((Element) node).getAttribute(MigrationConstants.SLING_RESOURCE_TYPE_PROP));
                            assertFalse(((Element) node).hasAttribute(MigrationConstants.QUALITY_PROP));
                        } else if ("cq5dam.fpo.jpeg".equals(((Element) node).getAttribute(MigrationConstants.NAME_PROP))) {
                            numRenditions++;
                            assertEquals("jpeg", ((Element) node).getAttribute(MigrationConstants.FMT_PROP));
                            assertEquals("10", ((Element) node).getAttribute(MigrationConstants.QUALITY_PROP));
                            assertEquals(MigrationConstants.RENDITION_RESOURCE_TYPE, ((Element) node).getAttribute(MigrationConstants.SLING_RESOURCE_TYPE_PROP));
                        }
                    }
                }
            }
        }

        assertTrue(contentNodeExists);
        assertEquals(2, numRenditions);
    }

    @Test
    public void testEmptyMimetypesIncluded() throws ParserConfigurationException, SAXException, IOException {
        File profilePath = new File(projectRootPath + "/" + MigrationConstants.PROCESSING_PROFILE_DISK_PATH + "/" + PROFILE_NAME);

        RenditionConfig renditionConfig1 = createThumbnailRendition();
        ProcessingProfile profile = new ProcessingProfile();
        profile.setName(PROFILE_NAME);
        profile.addRendition(renditionConfig1);

        dao.addProfile(profile);

        boolean renditionCreated = false;

        for (File child:profilePath.listFiles()) {
            if (child.getName().equals(".content.xml")) {
                // Is the jcr:content node
            } else {
                //is a rendition, recur into its child .content.xml to inspect it
                File renditionContent = new File(child, ".content.xml");
                Document xml = XmlUtil.loadXml(renditionContent);
                Element rootElem = xml.getDocumentElement();
                List<Node> childNodes = XmlUtil.getChildElementNodes(rootElem);

                for (Node node:childNodes) {
                    if (node.getNodeName().equals("jcr:content")) {
                        renditionCreated = true;
                        assertTrue(((Element)node).hasAttribute(MigrationConstants.INCLUDE_MIMETYPES_PROP));
                        assertTrue(((Element)node).hasAttribute(MigrationConstants.EXCLUDE_MIMETYPES_PROP));
                    }
                }
            }
        }

        assertTrue(renditionCreated);
    }

    @Test
    public void testDuplicateProfileNames() {
        File profile1Path = new File(projectRootPath + "/" + MigrationConstants.PROCESSING_PROFILE_DISK_PATH + "/" + PROFILE_NAME);
        File profile2Path = new File(projectRootPath + "/" + MigrationConstants.PROCESSING_PROFILE_DISK_PATH + "/" + PROFILE_NAME + "-1");
        File profile3Path = new File(projectRootPath + "/" + MigrationConstants.PROCESSING_PROFILE_DISK_PATH + "/" + PROFILE_NAME + "-2");

        assertFalse(profile1Path.exists());
        assertFalse(profile2Path.exists());
        assertFalse(profile3Path.exists());

        //Add three profiles with the same name
        ProcessingProfile profile1 = createTestProfile();
        dao.addProfile(profile1);
        ProcessingProfile profile2 = createTestProfile();
        dao.addProfile(profile2);
        ProcessingProfile profile3 = createTestProfile();
        dao.addProfile(profile3);

        //Assert that they are added at different paths
        assertTrue(profile1Path.exists());
        assertTrue(profile2Path.exists());
        assertTrue(profile3Path.exists());
    }

    @Test
    public void testDuplicateRenditionNames() {
        //Two renditions with the same name
        ProcessingProfile profile = new ProcessingProfile();
        profile.setName(PROFILE_NAME);
        RenditionConfig rendition1 = createThumbnailRendition();
        RenditionConfig rendition2 = createThumbnailRendition();
        profile.addRendition(rendition1);
        profile.addRendition(rendition2);

        dao.addProfile(profile);

        File rendition1Path = new File(projectRootPath + "/" + MigrationConstants.PROCESSING_PROFILE_DISK_PATH + "/" + PROFILE_NAME + "/thumbnail");
        assertTrue(rendition1Path.exists());
        File rendition2Path = new File(projectRootPath + "/" + MigrationConstants.PROCESSING_PROFILE_DISK_PATH + "/" + PROFILE_NAME + "/thumbnail-1");
        assertTrue(rendition2Path.exists());
    }

    private ProcessingProfile createTestProfile() {
        Set<String> includedMimeTypes = new HashSet<>();
        includedMimeTypes.add("image/*");

        Set<String> excludedMimeTypes = new HashSet<>();
        excludedMimeTypes.add("video/*");
        excludedMimeTypes.add("audio/*");

        RenditionConfig renditionConfig1 = createThumbnailRendition();
        renditionConfig1.setIncludeMimeTypes(includedMimeTypes);
        renditionConfig1.setExcludeMimeTypes(excludedMimeTypes);

        RenditionConfig renditionConfig2 = new RenditionConfig();
        renditionConfig2.setQuality(10);
        renditionConfig2.setNodeName("fpo");
        renditionConfig2.setFileName("cq5dam.fpo.jpeg");
        renditionConfig2.setFormat("jpeg");
        renditionConfig2.setIncludeMimeTypes(includedMimeTypes);
        renditionConfig2.setExcludeMimeTypes(excludedMimeTypes);

        ProcessingProfile profile = new ProcessingProfile();
        profile.setName(PROFILE_NAME);
        profile.addRendition(renditionConfig1);
        profile.addRendition(renditionConfig2);

        return profile;
    }

    private RenditionConfig createThumbnailRendition() {
        RenditionConfig ren = new RenditionConfig();
        ren.setWidth(100);
        ren.setHeight(100);
        ren.setNodeName("thumbnail");
        ren.setFileName("cq5dam.thumbnail.100.100.png");
        ren.setFormat("png");
        return ren;
    }
}
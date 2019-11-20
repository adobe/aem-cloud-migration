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

package com.adobe.skyline.migration.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RenditionConfig {

    private Set<String> includeMimeTypes;
    private Set<String> excludeMimeTypes;

    private String format;
    private int height;
    private int width;
    private int qlt;

    private String nodeName; //Maps to node name in the JCR
    private String fileName; //Maps to name property in the JCR

    public RenditionConfig() {
        includeMimeTypes = new HashSet<>();
        excludeMimeTypes = new HashSet<>();
    }

    public Set<String> getIncludeMimeTypes() {
        return includeMimeTypes;
    }

    public void setIncludeMimeTypes(Set<String> includeMimeTypes) {
        this.includeMimeTypes = includeMimeTypes;
    }

    public Set<String> getExcludeMimeTypes() {
        return excludeMimeTypes;
    }

    public void setExcludeMimeTypes(Set<String> excludeMimeTypes) {
        this.excludeMimeTypes = excludeMimeTypes;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getQuality() {
        return qlt;
    }

    public void setQuality(int qlt) {
        this.qlt = qlt;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    // Method used to merge renditions configurations that only differ in name or mimetype restrictions
    public String getRenditionConfigHash() {
        return format + ":" + height + ":" + width + ":" + qlt;
    }
}

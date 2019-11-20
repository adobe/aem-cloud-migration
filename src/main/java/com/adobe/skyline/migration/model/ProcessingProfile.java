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
import java.util.List;
import java.util.Set;

public class ProcessingProfile {

    private String name;
    private List<RenditionConfig> renditions;

    public ProcessingProfile() {
        renditions = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RenditionConfig> getRenditions() {
        return renditions;
    }

    public void addRendition(RenditionConfig rendition) {
        this.renditions.add(rendition);
    }

    public void setRenditions(List<RenditionConfig> renditions) {
        this.renditions = renditions;
    }

}

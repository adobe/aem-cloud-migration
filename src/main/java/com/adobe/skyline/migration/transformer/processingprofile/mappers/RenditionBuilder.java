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

package com.adobe.skyline.migration.transformer.processingprofile.mappers;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.model.RenditionConfig;

import java.util.Set;

public class RenditionBuilder {

    public static RenditionConfig buildRendition(int width, int height, String nodeName, String renditionPrefix, Set<String> includeMimetypes, Set<String> excludeMimetypes) {
        RenditionConfig rendition = new RenditionConfig();

        rendition.setWidth(width);
        rendition.setHeight(height);
        rendition.setFormat(MigrationConstants.PNG_EXTENSION);
        rendition.setNodeName(nodeName);
        rendition.setFileName(renditionPrefix + "." + nodeName + "." + width + "." + height + "." + MigrationConstants.PNG_EXTENSION);

        if (includeMimetypes != null && includeMimetypes.size() > 0) {
            rendition.setIncludeMimeTypes(includeMimetypes);
        }

        if (excludeMimetypes != null && excludeMimetypes.size() > 0) {
            rendition.setExcludeMimeTypes(excludeMimetypes);
        }

        return rendition;
    }
}

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

package com.adobe.skyline.migration.testutils.matchers;

import com.adobe.skyline.migration.model.RenditionConfig;
import org.mockito.ArgumentMatcher;

import java.util.ArrayList;
import java.util.List;

public class RenditionConfigMatcher implements ArgumentMatcher<RenditionConfig> {

    private List<String> expectedIncludedMimetypes;
    private List<String> expectedExcludedMimetypes;
    private List<String> expectedNonIncludedMimetypes;
    private int height;
    private int width;
    private String format;
    private String name;

    public RenditionConfigMatcher() {
        expectedIncludedMimetypes = new ArrayList<>();
        expectedExcludedMimetypes = new ArrayList<>();
        expectedNonIncludedMimetypes = new ArrayList<>();
    }

    public void expectIncludedMimetype(String mimetype) {
        expectedIncludedMimetypes.add(mimetype);
    }

    public void expectNonIncludedMimetype(String mimetype) {
        expectedNonIncludedMimetypes.add(mimetype);
    }

    public void expectExcludedMimetype(String mimetype) {
        expectedExcludedMimetypes.add(mimetype);
    }

    public void expectHeight(int height) {
        this.height = height;
    }

    public void expectWidth(int width) {
        this.width = width;
    }

    public void expectFormat(String format) {
        this.format = format;
    }

    public void expectName(String name) {
        this.name = name;
    }

    @Override
    public boolean matches(RenditionConfig renditionConfig) {
        boolean matches = true;

        if (height > 0 && height != renditionConfig.getHeight()) {
            matches = false;
        }

        if (width > 0 && width != renditionConfig.getWidth()) {
            matches = false;
        }

        if (format != null && !format.equals(renditionConfig.getFormat())) {
            matches = false;
        }

        if (name != null && !name.equals(renditionConfig.getFileName())) {
            matches = false;
        }

        if (expectedIncludedMimetypes.size() > 0) {
            if (renditionConfig.getIncludeMimeTypes() != null && renditionConfig.getIncludeMimeTypes().size() > 0) {
                for (String expectedMimetype:expectedIncludedMimetypes) {
                    if (!renditionConfig.getIncludeMimeTypes().contains(expectedMimetype)) {
                        matches = false;
                        break;
                    }
                }
            } else { //We have mimetype matchers defined, but no mimetypes specified, fail the matcher
                matches = false;
            }
        }

        if (expectedExcludedMimetypes.size() > 0) {
            if (renditionConfig.getExcludeMimeTypes() != null && renditionConfig.getExcludeMimeTypes().size() > 0) {
                for (String expectedMimetype:expectedExcludedMimetypes) {
                    if (!renditionConfig.getExcludeMimeTypes().contains(expectedMimetype)) {
                        matches = false;
                        break;
                    }
                }
            } else { //We have mimetype matchers defined, but no mimetypes specified, fail the matcher
                matches = false;
            }
        }

        if (expectedNonIncludedMimetypes.size() > 0) {
            if (renditionConfig.getIncludeMimeTypes() != null && renditionConfig.getIncludeMimeTypes().size() > 0) {
                for (String unexpected:expectedNonIncludedMimetypes) {
                    if (renditionConfig.getIncludeMimeTypes().contains(unexpected)) {
                        matches = false;
                        break;
                    }
                }
            }
        }

        return matches;
    }
}

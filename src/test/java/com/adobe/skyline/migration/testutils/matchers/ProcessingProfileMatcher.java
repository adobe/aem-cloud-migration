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

import com.adobe.skyline.migration.model.ProcessingProfile;
import com.adobe.skyline.migration.model.RenditionConfig;
import org.mockito.ArgumentMatcher;

import java.util.ArrayList;
import java.util.List;

public class ProcessingProfileMatcher implements ArgumentMatcher<ProcessingProfile> {

    private String expectedName;
    private List<RenditionConfigMatcher> renditionMatchers;
    private int numRenditions;

    public ProcessingProfileMatcher() {
        renditionMatchers = new ArrayList<>();
        numRenditions = -1;
    }

    public void expectName(String name) {
        this.expectedName = name;
    }

    public void expectRendition(RenditionConfigMatcher matcher) {
        renditionMatchers.add(matcher);
    }

    public void expectNumRenditions(int number) {
        this.numRenditions = number;
    }

    @Override
    public boolean matches(ProcessingProfile processingProfile) {

        boolean matches = true;

        if (expectedName != null) {
            if (!expectedName.equals(processingProfile.getName())) {
                matches = false;
            }
        }

        if (renditionMatchers.size() > 0) { //We have rendition matchers defined, we need to validate each of them
            if (processingProfile.getRenditions() != null && processingProfile.getRenditions().size() > 0) {
                for (RenditionConfigMatcher matcher : renditionMatchers) {
                    boolean renditionMatched = false;

                    //Look at each rendition on the processing profile to see if we have a match
                    for (RenditionConfig renditionConfig : processingProfile.getRenditions()) {
                        if (matcher.matches(renditionConfig)) {
                            renditionMatched = true;
                        }
                    }

                    //If we don't find a match for the given rendition, fail the matcher
                    if (!renditionMatched) {
                        matches = false;
                    }
                }
            } else { //We have rendition matchers defined, but no renditions specified, fail the matcher
                matches = false;
            }
        }

        if (numRenditions > -1 && !(processingProfile.getRenditions().size() == numRenditions)) {
            matches = false;
        }

        return matches;
    }
}
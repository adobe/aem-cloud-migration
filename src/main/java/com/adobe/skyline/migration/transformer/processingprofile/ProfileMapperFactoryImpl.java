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

package com.adobe.skyline.migration.transformer.processingprofile;

import com.adobe.skyline.migration.model.workflow.WorkflowStep;
import com.adobe.skyline.migration.transformer.processingprofile.mappers.CreatePdfPreviewProcessMapper;
import com.adobe.skyline.migration.transformer.processingprofile.mappers.CreateWebEnabledImageProcessMapper;
import com.adobe.skyline.migration.transformer.processingprofile.mappers.FFMpegThumbnailProcessMapper;
import com.adobe.skyline.migration.transformer.processingprofile.mappers.FFMpegTranscodeProcessMapper;
import com.adobe.skyline.migration.transformer.processingprofile.mappers.ThumbnailProcessMapper;

import java.util.HashMap;
import java.util.Map;

public class ProfileMapperFactoryImpl implements ProfileMapperFactory {

    //Directory of mappers by workflow process
    private Map<String, ProfileMapper> mapperMap;

    public ProfileMapperFactoryImpl() {
        mapperMap = new HashMap<>();

        addMapper(new CreatePdfPreviewProcessMapper());
        addMapper(new CreateWebEnabledImageProcessMapper());
        addMapper(new FFMpegThumbnailProcessMapper());
        addMapper(new ThumbnailProcessMapper());
        addMapper(new FFMpegTranscodeProcessMapper());
    }

    private void addMapper(ProfileMapper mapper) {
        for (String id : mapper.getProcessIds()) {
            mapperMap.put(id, mapper);
        }
    }

    //Get the mapper for a particular workflow step
    @Override
    public ProfileMapper getMapper(WorkflowStep step) {
        return mapperMap.get(step.getProcess());
    }
}

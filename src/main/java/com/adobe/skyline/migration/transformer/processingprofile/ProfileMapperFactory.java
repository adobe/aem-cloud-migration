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

/**
 * A factory to return ProfileMappers for WorkflowSteps.  The implementation details of the mappers have been hidden
 * behind the factory pattern to ease adding new mappers in the future and to hide the complexity of these transformations
 * from the rest of the system.
 */
public interface ProfileMapperFactory {

    ProfileMapper getMapper(WorkflowStep step);
}

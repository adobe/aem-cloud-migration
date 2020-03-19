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

package com.adobe.skyline.migration.testutils;

public class TestConstants {
    public static final String TEST_PROJECT_GROUP_ID = "com.adobe.sample";
    public static final String TEST_PROJECT_ARTIFACT_ID = "sample";
    public static final String TEST_PROJECT_VERSION = "1.0-SNAPSHOT";

    public static final String CONF_WORKFLOW_PROJECT_NAME = "ui.content";
    public static final String CONF_WORKFLOW_PATH = "/src/main/content/jcr_root/conf/global/settings/workflow";
    public static final String CONF_LAUNCHER_PATH = CONF_WORKFLOW_PATH + "/launcher/config/";
    public static final String CONF_MODEL_PATH =  CONF_WORKFLOW_PATH + "/models/";

    public static final String ETC_WORKFLOW_PROJECT_NAME = "ui.apps";
    public static final String ETC_WORKFLOW_PATH = "/src/main/content/jcr_root/etc/workflow";
    public static final String ETC_LAUNCHER_PATH = ETC_WORKFLOW_PATH + "/launcher/config/";
    public static final String ETC_MODEL_PATH =  ETC_WORKFLOW_PATH + "/models/";

    public static final String VAR_WORKFLOW_PATH = "/src/main/content/jcr_root/var/workflow";

    public static final String CONTAINER_PACKAGE_PROJECT_NAME = "all";
}

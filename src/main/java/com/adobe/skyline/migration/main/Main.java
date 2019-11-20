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
package com.adobe.skyline.migration.main;

import com.adobe.skyline.migration.exception.CustomerDataException;
import com.adobe.skyline.migration.util.Logger;

/**
 * Main entry point to execute the tool.  As main is not easily testable, this class has been designed to contain as
 * little code as possible.
 */
public class Main {

    private static final String USAGE_INSTRUCTIONS = "Usage: java -jar sky-migrate-x.x.jar path/to/project [path/to/reportOutput]";

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            Logger.ERROR(USAGE_INSTRUCTIONS);
        } else {
            try {
                WorkflowStepConfiguration config = new WorkflowStepConfiguration();
                String customerProjectPath = args[0];
                String reportOutputDir = args.length > 1 ? args[1] : System.getProperty("user.dir");
                MigrationOrchestrator orchestrator = new MigrationOrchestrator(customerProjectPath, config, reportOutputDir);
                orchestrator.exec();
            } catch (CustomerDataException e) {
                Logger.ERROR(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

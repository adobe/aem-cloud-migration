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

package com.adobe.skyline.migration.transformer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.exception.MigrationRuntimeException;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.model.ProcessingProfile;
import com.adobe.skyline.migration.model.WorkflowStepSupportStatus;
import com.adobe.skyline.migration.util.file.FileUtil;

/**
 * The MigrationReportWriter creates a report of all changes that the tool has made.
 */
public class MigrationReportWriter {

    private ChangeTrackingService changeTracker;

    public MigrationReportWriter(ChangeTrackingService changeTracker) {
        this.changeTracker = changeTracker;
    }

    public void write(File outputDir) {
        try {
            File reportFile = generateBaselineFile(outputDir);
            writeLaunchersDisabled(reportFile);
            writeRunnerConfigs(reportFile);
            writeModifiedWorkflowSteps(reportFile);
            writePathsDeleted(reportFile);
            writeProcessingProfiles(reportFile);
            writeProjects(reportFile);
            writeMigrationIssues(reportFile);
        } catch (IOException e) {
            throw new MigrationRuntimeException("Unable to output a migration report.", e);
        }
    }

    /**
     * Copy the template report to the outputDir.  If the file already exists, append a suffix of -1.  If _that_ file
     * already exists, use -2, and so on.
     */
    private File generateBaselineFile(File outputDir) throws IOException {
        File reportFile = new File(outputDir, MigrationConstants.REPORT_FILENAME);

        int index = 0;
        while (reportFile.exists()) {
            index++;
            reportFile = new File(outputDir, MigrationConstants.REPORT_NAME + "-" + index + "." + MigrationConstants.REPORT_EXTENSION);
        }

        try (InputStream template = this.getClass().getResourceAsStream("/" + MigrationConstants.REPORT_TEMPLATE_FILENAME)) {
            Files.copy(template, reportFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return reportFile;
    }

    private void writeLaunchersDisabled(File reportFile) throws IOException {
        StringBuilder launcherMdBuilder = new StringBuilder();

        if (changeTracker.getDisabledLaunchers().size() > 0) {
            List<List<String>> tableValues = new LinkedList<>();

            List<String> headerValues = new LinkedList<>(Arrays.asList("Action", "Launcher"));
            tableValues.add(headerValues);
            List<String> underlineRow = new LinkedList<>(Arrays.asList("------", "--------"));
            tableValues.add(underlineRow);

            for (String launcher : changeTracker.getDisabledLaunchers()) {
                List<String> nextRow = new LinkedList<>(Arrays.asList("Disabled", "`" + launcher + "`"));
                tableValues.add(nextRow);
            }

            String mdTable = createMarkdownTable(tableValues);
            launcherMdBuilder.append(mdTable);

        } else {
            launcherMdBuilder.append(MigrationConstants.NO_LAUNCHER_MSG);
            launcherMdBuilder.append(System.getProperty("line.separator"));
        }

        FileUtil.findAndReplaceInFile(reportFile, "\\$\\{LAUNCHERS_DISABLED\\}", launcherMdBuilder.toString());
    }

    private void writeRunnerConfigs(File reportFile) throws IOException {
        StringBuilder configMdBuilder = new StringBuilder();

        Map<String, String> configsAdded = changeTracker.getWorkflowRunnerConfigsAdded();

        if (configsAdded.size() > 0) {
            List<List<String>> tableValues = new LinkedList<>();

            List<String> headerValues = new LinkedList<>(Arrays.asList("Action", "Glob Pattern", "Workflow Model"));
            tableValues.add(headerValues);
            List<String> underlineRow = new LinkedList<>(Arrays.asList("------", "------------", "--------------"));
            tableValues.add(underlineRow);

            for (String glob : configsAdded.keySet()) {
                List<String> nextRow = new LinkedList<>(Arrays.asList("Created", "`" + glob + "`", "`" + configsAdded.get(glob) + "`"));
                tableValues.add(nextRow);
            }

            String mdTable = createMarkdownTable(tableValues);
            configMdBuilder.append(mdTable);
        } else {
            configMdBuilder.append(MigrationConstants.NO_RUNNER_CFG_MSG);
            configMdBuilder.append(System.getProperty("line.separator"));
        }

        FileUtil.findAndReplaceInFile(reportFile, "\\$\\{RUNNER_CONFIGS_CREATED\\}", configMdBuilder.toString());
    }

    private void writeModifiedWorkflowSteps(File reportFile) throws IOException {
        StringBuilder wfStepBuilder = new StringBuilder();

        Map<String, Map<String, WorkflowStepSupportStatus>> modifiedWorkflows = changeTracker.getModifiedWorkflowSteps();

        if (modifiedWorkflows.size() > 0) {
            for (String model : modifiedWorkflows.keySet()) {
                wfStepBuilder.append("### " + model);
                wfStepBuilder.append(System.getProperty("line.separator"));
                wfStepBuilder.append(System.getProperty("line.separator"));

                List<List<String>> tableValues = new LinkedList<>();
                List<String> headerValues = new LinkedList<>(Arrays.asList("Action", "Step", "Reason"));
                tableValues.add(headerValues);
                List<String> underlineRow = new LinkedList<>(Arrays.asList("------", "----", "------"));
                tableValues.add(underlineRow);

                Map<String, WorkflowStepSupportStatus> modifiedSteps = modifiedWorkflows.get(model);

                for (String step : modifiedSteps.keySet()) {
                    WorkflowStepSupportStatus supportStatus = modifiedSteps.get(step);
                    List<String> nextRow = new LinkedList<>(Arrays.asList(supportStatus.getAction(), "`" + step + "`", supportStatus.getDescription()));
                    tableValues.add(nextRow);
                }

                String mdTable = createMarkdownTable(tableValues);
                wfStepBuilder.append(mdTable);
                wfStepBuilder.append(System.getProperty("line.separator"));
            }
        } else {
            wfStepBuilder.append(MigrationConstants.NO_MODEL_UPDATE_MSG);
            wfStepBuilder.append(System.getProperty("line.separator"));
        }

        FileUtil.findAndReplaceInFile(reportFile, "\\$\\{WORKFLOW_MODELS_TRANSFORMED\\}", wfStepBuilder.toString());
    }

    private void writePathsDeleted(File reportFile) throws IOException {
        StringBuilder mdBuilder = new StringBuilder();

        if (changeTracker.getVarPathsDeleted().size() > 0) {
            List<List<String>> tableValues = new LinkedList<>();

            List<String> headerValues = new LinkedList<>(Arrays.asList("Action", "Path"));
            tableValues.add(headerValues);
            List<String> underlineRow = new LinkedList<>(Arrays.asList("------", "-------"));
            tableValues.add(underlineRow);

            for (String path : changeTracker.getVarPathsDeleted()) {
                List<String> nextRow = new LinkedList<>(Arrays.asList("Deleted", path));
                tableValues.add(nextRow);
            }

            String mdTable = createMarkdownTable(tableValues);
            mdBuilder.append(mdTable);
        } else {
            mdBuilder.append(MigrationConstants.NO_PATHS_DELETED_MSG);
            mdBuilder.append(System.getProperty("line.separator"));
        }

        FileUtil.findAndReplaceInFile(reportFile, "\\$\\{VAR_PATHS_DELETED\\}", mdBuilder.toString());
    }

    private void writeProcessingProfiles(File reportFile) throws IOException {
        StringBuilder profileMdBuilder = new StringBuilder();

        if (changeTracker.getProcessingProfilesCreated().size() > 0 || changeTracker.getFailedMappings().size() > 0) {
            List<List<String>> tableValues = new LinkedList<>();

            List<String> headerValues = new LinkedList<>(Arrays.asList("Action", "Profile"));
            tableValues.add(headerValues);
            List<String> underlineRow = new LinkedList<>(Arrays.asList("------", "-------"));
            tableValues.add(underlineRow);

            for (ProcessingProfile profile : changeTracker.getProcessingProfilesCreated()) {
                List<String> nextRow = new LinkedList<>(Arrays.asList("Created", profile.getName()));
                tableValues.add(nextRow);
            }

            for (Map.Entry<String, String> failureEntry : changeTracker.getFailedMappings().entrySet()) {
                List<String> nextRow = new LinkedList<>(Arrays.asList("Failed", failureEntry.getKey()));
                tableValues.add(nextRow);
            }

            String mdTable = createMarkdownTable(tableValues);
            profileMdBuilder.append(mdTable);
        } else {
            profileMdBuilder.append(MigrationConstants.NO_PROFILE_MSG);
            profileMdBuilder.append(System.getProperty("line.separator"));
        }

        FileUtil.findAndReplaceInFile(reportFile, "\\$\\{PROCESSING_PROFILES_CREATED\\}", profileMdBuilder.toString());
    }

    private void writeProjects(File reportFile) throws IOException {
        StringBuilder projectMdBuilder = new StringBuilder();

        if (changeTracker.getProjectsCreated().size() > 0) {
            List<List<String>> tableValues = new LinkedList<>();

            List<String> headerValues = new LinkedList<>(Arrays.asList("Action", "Project"));
            tableValues.add(headerValues);
            List<String> underlineRow = new LinkedList<>(Arrays.asList("------", "-------"));
            tableValues.add(underlineRow);

            for (String project : changeTracker.getProjectsCreated()) {
                List<String> nextRow = new LinkedList<>(Arrays.asList("Created", project));
                tableValues.add(nextRow);
            }

            String mdTable = createMarkdownTable(tableValues);
            projectMdBuilder.append(mdTable);
        } else {
            projectMdBuilder.append(MigrationConstants.NO_PROJECT_MSG);
            projectMdBuilder.append(System.getProperty("line.separator"));
        }

        FileUtil.findAndReplaceInFile(reportFile, "\\$\\{PROJECTS_CREATED\\}", projectMdBuilder.toString());
    }

    private String createMarkdownTable(List<List<String>> tableValues) {
        StringBuilder table = new StringBuilder();

        for (List<String> row : tableValues) {
            table.append("|");

            for (String cell : row) {
                table.append(" ");
                table.append(cell);
                table.append(" |");
            }

            table.append(System.getProperty("line.separator"));
        }

        return table.toString();
    }

    private void writeMigrationIssues(File reportFile) throws IOException {
        StringBuilder issuesBuilder = new StringBuilder();

        if (changeTracker.getFailedMappings().size() > 0) {
            List<List<String>> tableValues = new LinkedList<>();

            List<String> headerValues = new LinkedList<>(Arrays.asList("Action", "Step", "Reason"));
            tableValues.add(headerValues);
            List<String> underlineRow = new LinkedList<>(Arrays.asList("-------", "-------", "-------"));
            tableValues.add(underlineRow);

            for (Map.Entry<String, String> failureEntry : changeTracker.getFailedMappings().entrySet()) {
                List<String> nextRow = new LinkedList<>(Arrays.asList("Failed", failureEntry.getKey(), failureEntry.getValue()));
                tableValues.add(nextRow);
            }

            String mdTable = createMarkdownTable(tableValues);
            issuesBuilder.append(mdTable);
        } else {
            issuesBuilder.append(MigrationConstants.NO_FAILURE_MSG);
            issuesBuilder.append(System.getProperty("line.separator"));
        }

        FileUtil.findAndReplaceInFile(reportFile, "\\$\\{MIGRATION_ISSUES\\}", issuesBuilder.toString());
    }
}

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.model.ProcessingProfile;

import static com.adobe.skyline.migration.model.WorkflowStepSupportStatus.DMS7_OOTB;
import static com.adobe.skyline.migration.model.WorkflowStepSupportStatus.NUI_MIGRATED;
import static com.adobe.skyline.migration.model.WorkflowStepSupportStatus.NUI_OOTB;
import static com.adobe.skyline.migration.model.WorkflowStepSupportStatus.REQUIRED;
import static com.adobe.skyline.migration.model.WorkflowStepSupportStatus.UNNECESSARY;
import static com.adobe.skyline.migration.model.WorkflowStepSupportStatus.UNSUPPORTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MigrationReportWriterTest {

    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    private ChangeTrackingService changeTracker;
    private MigrationReportWriter writer;

    @Before
    public void setUp() {
        this.changeTracker = new ChangeTrackingService();
        this.writer = new MigrationReportWriter(changeTracker);
    }

    @Test
    public void testReportWritten() throws FileNotFoundException {
        writer.write(temp.getRoot());

        File migrationReport = new File(temp.getRoot(), MigrationConstants.REPORT_FILENAME);
        assertTrue(migrationReport.exists());

        Scanner scanner = new Scanner(migrationReport);
        String firstLine = scanner.nextLine();
        assertEquals("# AEM Assets as a Cloud Service - Workflow Migration Report", firstLine);
    }

    @Test
    public void testWrittenWithNextAvailableSuffixIfExisting() throws IOException {
        File baseFolder = temp.newFolder();

        File migrationReport = new File(baseFolder, MigrationConstants.REPORT_FILENAME);
        Files.createFile(migrationReport.toPath());

        File migrationReport1 = new File(baseFolder, MigrationConstants.REPORT_NAME + "-1." + MigrationConstants.REPORT_EXTENSION);
        Files.createFile(migrationReport1.toPath());

        File migrationReport2 = new File(baseFolder, MigrationConstants.REPORT_NAME + "-2." + MigrationConstants.REPORT_EXTENSION);
        assertFalse(migrationReport2.exists());

        writer.write(baseFolder);

        assertTrue(migrationReport2.exists());
    }

    @Test
    public void testLaunchersWritten() throws FileNotFoundException {
        changeTracker.trackLauncherDisabled("launcher1");
        changeTracker.trackLauncherDisabled("launcher2");

        writer.write(temp.getRoot());

        File migrationReport = new File(temp.getRoot(), MigrationConstants.REPORT_FILENAME);
        Scanner scanner = new Scanner(migrationReport);

        boolean matched1 = false;
        boolean matched2 = false;

        while (scanner.hasNext()) {
            String line = scanner.nextLine();

            if (line.equals("## Workflow Launchers")) {
                skipLines(scanner, 4);
                String firstListed = scanner.nextLine();
                String secondListed = scanner.nextLine();

                if (firstListed.contains("launcher1") || secondListed.contains("launcher1")) {
                    matched1 = true;
                } if (firstListed.contains("launcher2") || secondListed.contains("launcher2")) {
                    matched2 = true;
                }

                break;
            }
        }

        assertTrue(matched1 && matched2);
    }

    @Test
    public void testRunnerConfigurationWritten() throws FileNotFoundException {
        changeTracker.trackWorkflowRunnerConfigAdded("glob1", "model1");
        changeTracker.trackWorkflowRunnerConfigAdded("glob2", "model2");

        writer.write(temp.getRoot());

        File migrationReport = new File(temp.getRoot(), MigrationConstants.REPORT_FILENAME);
        Scanner scanner = new Scanner(migrationReport);

        boolean matched1 = false;
        boolean matched2 = false;

        while (scanner.hasNext()) {
            String line = scanner.nextLine();

            if (line.equals("## Custom Workflow Runner configuration")) {
                skipLines(scanner, 4);

                String firstListed = scanner.nextLine();
                String secondListed = scanner.nextLine();

                if ((firstListed.contains("glob1") && firstListed.contains("model1")) ||
                        (secondListed.contains("glob1") && secondListed.contains("model1"))) {
                    matched1 = true;
                } if ((firstListed.contains("glob2") && firstListed.contains("model2")) ||
                        (secondListed.contains("glob2") && secondListed.contains("model2"))) {
                    matched2 = true;
                }

                break;
            }
        }

        assertTrue(matched1 && matched2);
    }

    @Test
    public void testModelUpdatesWritten() throws FileNotFoundException {
        changeTracker.trackModifiedWorkflowStep("model1Path", "step1", REQUIRED);
        changeTracker.trackModifiedWorkflowStep("model1Path", "step2", NUI_OOTB);
        changeTracker.trackModifiedWorkflowStep("model1Path", "step3", UNSUPPORTED);

        changeTracker.trackModifiedWorkflowStep("model2Path", "step4", DMS7_OOTB);
        changeTracker.trackModifiedWorkflowStep("model2Path", "step5", NUI_MIGRATED);
        changeTracker.trackModifiedWorkflowStep("model2Path", "step6", UNNECESSARY);

        writer.write(temp.getRoot());

        File migrationReport = new File(temp.getRoot(), MigrationConstants.REPORT_FILENAME);
        Scanner scanner = new Scanner(migrationReport);

        boolean matched1 = false;
        boolean matched2 = false;

        while (scanner.hasNext()) {
            String line = scanner.nextLine();

            if (line.equals("## Workflow Model Updates")) {
                skipLines(scanner, 4);

                String m1 = scanner.nextLine();
                skipLines(scanner, 3);
                String m1s1 = scanner.nextLine();
                String m1s2 = scanner.nextLine();
                String m1s3 = scanner.nextLine();

                scanner.nextLine(); //Empty space

                String m2 = scanner.nextLine();
                skipLines(scanner, 3);
                String m2s1 = scanner.nextLine();
                String m2s2 = scanner.nextLine();
                String m2s3 = scanner.nextLine();

                if (m1.equals("### model1Path")) {
                    matched1 = true;
                    assertModel1(m1s1, m1s2, m1s3);
                } else if (m1.equals("### model2Path")) {
                    matched2 = true;
                    assertModel2(m1s1, m1s2, m1s3);
                }

                if (m2.equals("### model1Path")) {
                    matched1 = true;
                    assertModel1(m2s1, m2s2, m2s3);
                } else if (m2.equals("### model2Path")) {
                    matched2 = true;
                    assertModel2(m2s1, m2s2, m2s3);
                }

                break;
            }
        }

        assertTrue(matched1 && matched2);
    }

    private void assertModel1(String line1, String line2, String line3) {
        assertTrue(line1.contains(REQUIRED.getAction()) && line1.contains("step1") && line1.contains(REQUIRED.getDescription()));
        assertTrue(line2.contains(NUI_OOTB.getAction()) && line2.contains("step2") && line2.contains(NUI_OOTB.getDescription()));
        assertTrue(line3.contains(UNSUPPORTED.getAction()) && line3.contains("step3") && line3.contains(UNSUPPORTED.getDescription()));
    }
    private void assertModel2(String line1, String line2, String line3) {
        assertTrue(line1.contains(DMS7_OOTB.getAction()) && line1.contains("step4") && line1.contains(DMS7_OOTB.getDescription()));
        assertTrue(line2.contains(NUI_MIGRATED.getAction()) && line2.contains("step5") && line2.contains(NUI_MIGRATED.getDescription()));
        assertTrue(line3.contains(UNNECESSARY.getAction()) && line3.contains("step6") && line3.contains(UNNECESSARY.getDescription()));
    }

    @Test
    public void testDeletedPathsWritten() throws FileNotFoundException {
        changeTracker.trackVarPathDeleted("/path/to/var/workflow");
        changeTracker.trackVarPathDeleted("/path/to/var/workflow2");

        writer.write(temp.getRoot());

        File migrationReport = new File(temp.getRoot(), MigrationConstants.REPORT_FILENAME);
        Scanner scanner = new Scanner(migrationReport);

        boolean matched1 = false;
        boolean matched2 = false;

        while (scanner.hasNext()) {
            String line = scanner.nextLine();

            if (line.equals("## Paths Deleted")) {
                skipLines(scanner, 4);

                String firstListed = scanner.nextLine();
                String secondListed = scanner.nextLine();

                if (firstListed.contains("/path/to/var/workflow") || secondListed.contains("/path/to/var/workflow")) {
                    matched1 = true;
                } if (firstListed.contains("/path/to/var/workflow2") || secondListed.contains("/path/to/var/workflow2")) {
                    matched2 = true;
                }

                break;
            }
        }

        assertTrue(matched1 && matched2);
    }

    @Test
    public void testProcessingProfilesWritten() throws FileNotFoundException {
        ProcessingProfile profile1 = new ProcessingProfile();
        profile1.setName("profile1");
        changeTracker.trackCreatedProcessingProfile(profile1);

        ProcessingProfile profile2 = new ProcessingProfile();
        profile2.setName("profile2");
        changeTracker.trackCreatedProcessingProfile(profile2);

        writer.write(temp.getRoot());

        File migrationReport = new File(temp.getRoot(), MigrationConstants.REPORT_FILENAME);
        Scanner scanner = new Scanner(migrationReport);

        boolean matched1 = false;
        boolean matched2 = false;

        while (scanner.hasNext()) {
            String line = scanner.nextLine();

            if (line.equals("## Asset Compute Service Processing Profiles")) {
                skipLines(scanner, 4);

                String firstListed = scanner.nextLine();
                String secondListed = scanner.nextLine();

                if (firstListed.contains("profile1") || secondListed.contains("profile1")) {
                    matched1 = true;
                } if (firstListed.contains("profile2") || secondListed.contains("profile2")) {
                    matched2 = true;
                }

                break;
            }
        }

        assertTrue(matched1 && matched2);
    }

    @Test
    public void testProjectsWritten() throws FileNotFoundException {
        changeTracker.trackProjectCreated("project1");
        changeTracker.trackProjectCreated("project2");

        writer.write(temp.getRoot());

        File migrationReport = new File(temp.getRoot(), MigrationConstants.REPORT_FILENAME);
        Scanner scanner = new Scanner(migrationReport);

        boolean matched1 = false;
        boolean matched2 = false;

        while (scanner.hasNext()) {
            String line = scanner.nextLine();

            if (line.equals("## Maven Projects Added and Modified")) {
                skipLines(scanner, 4);

                String firstListed = scanner.nextLine();
                String secondListed = scanner.nextLine();

                if (firstListed.contains("project1") || secondListed.contains("project1")) {
                    matched1 = true;
                } if (firstListed.contains("project2") || secondListed.contains("project2")) {
                    matched2 = true;
                }

                break;
            }
        }

        assertTrue(matched1 && matched2);
    }

    @Test
    public void testNoEntryPlaceholders() throws FileNotFoundException {
        writer.write(temp.getRoot());

        File migrationReport = new File(temp.getRoot(), MigrationConstants.REPORT_FILENAME);
        Scanner scanner = new Scanner(migrationReport);

        while (scanner.hasNext()) {
            String line = scanner.nextLine();

            if (line.equals("## Workflow Launchers")) {
                skipLines(scanner, 2);
                assertEquals(MigrationConstants.NO_LAUNCHER_MSG, scanner.nextLine());
            }

            if (line.equals("## Custom Workflow Runner configuration")) {
                skipLines(scanner, 2);
                assertEquals(MigrationConstants.NO_RUNNER_CFG_MSG, scanner.nextLine());
            }

            if (line.equals("## Workflow Model Updates")) {
                skipLines(scanner, 4);
                assertEquals(MigrationConstants.NO_MODEL_UPDATE_MSG, scanner.nextLine());
            }

            if (line.equals("## Paths Deleted")) {
                skipLines(scanner, 2);
                assertEquals(MigrationConstants.NO_PATHS_DELETED_MSG, scanner.nextLine());
            }

            if (line.equals("## Asset Compute Service Processing Profiles")) {
                skipLines(scanner, 2);
                assertEquals(MigrationConstants.NO_PROFILE_MSG, scanner.nextLine());
            }

            if (line.equals("## Maven Projects Added and Modified")) {
                skipLines(scanner, 2);
                assertEquals(MigrationConstants.NO_PROJECT_MSG, scanner.nextLine());
            }
        }
    }

    private void skipLines(Scanner scanner, int numLines) {
        for (int i = 0; i < numLines; i++) {
            scanner.nextLine();
        }
    }
}
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
package com.adobe.skyline.migration.dao;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class WorkflowRunnerConfigDAOTest extends SkylineMigrationBaseTest {

    private static final String PID = "com.adobe.cq.dam.processor.nui.impl.workflow.CustomDamWorkflowRunnerImpl";
    private static final String CONFIG_FILE_REL_PATH = Paths.get("", MigrationConstants.MIGRATION_PROJECT_APPS, MigrationConstants.PATH_TO_JCR_ROOT,
            "apps", "aem-cloud-migration", "config.author", PID + ".cfg.json").toString();

    private static final String PATTERN = "\\/content\\/dam(\\/.*\\/)(marketing\\/seasonal)(\\/.*)";
    private static final String PATTERN_2 = "\\/content\\/dam(\\/.*\\/)(other_path)(\\/.*)";
    private static final String PATTERN_MODEL = "/var/workflow/models/request_for_activation";

    private static final String PATH = "/content/dam";
    private static final String PATH_2 = "/content/dam/subfolder";
    private static final String PATH_MODEL = "/var/workflow/models/dam/dam-autotag-assets";

    private File tempProjectRoot;
    private File configFile;
    private ChangeTrackingService changeTracker;
    private WorkflowRunnerConfigDAO dao;

    @Before
    public void setUp() {
        super.setUp();

        this.tempProjectRoot = projectLoader.copyConfProjectToTemp(temp);
        this.configFile = new File(tempProjectRoot, CONFIG_FILE_REL_PATH);
        this.changeTracker = new ChangeTrackingService();
        this.dao = new WorkflowRunnerConfigDAO(tempProjectRoot.getPath(), changeTracker);
    }

    @Test
    public void testConfigFileCreatedAsAuthorCfgJson() throws IOException {
        assertFalse(configFile.exists());

        dao.createConfigByPath(PATH, PATH_MODEL);

        assertTrue(configFile.exists());
        JsonObject config = readConfig(configFile);
        assertEquals(Arrays.asList(PATH + ":" + PATH_MODEL), getList(config, MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_PATH));
        assertFalse(config.has(MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_EXPRESSION));
    }

    @Test
    public void testCreateConfigByExpression() throws IOException {
        dao.createConfigByExpression(PATTERN, PATTERN_MODEL);
        dao.createConfigByExpression(PATTERN_2, PATTERN_MODEL);

        assertEquals(Arrays.asList(PATTERN + ":" + PATTERN_MODEL, PATTERN_2 + ":" + PATTERN_MODEL),
                getList(readConfig(configFile), MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_EXPRESSION));
    }

    @Test
    public void testPathEntriesOrderedMostSpecificFirst() throws IOException {
        dao.createConfigByPath(PATH, PATH_MODEL);
        dao.createConfigByPath(PATH_2, PATTERN_MODEL);

        assertEquals(Arrays.asList(PATH_2 + ":" + PATTERN_MODEL, PATH + ":" + PATH_MODEL),
                getList(readConfig(configFile), MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_PATH));
    }

    @Test
    public void testDuplicateEntriesAddedOnce() throws IOException {
        dao.createConfigByPath(PATH, PATH_MODEL);
        dao.createConfigByPath(PATH, PATH_MODEL);

        assertEquals(1, getList(readConfig(configFile), MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_PATH).size());
    }

    @Test
    public void testExistingAuthorConfigsMergedAndRemoved() throws IOException {
        File customerXml = write("ui.config/src/main/content/jcr_root/apps/myapp/config/" + PID + ".xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<jcr:root xmlns:sling=\"http://sling.apache.org/jcr/sling/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\"\n"
                + "    jcr:primaryType=\"sling:OsgiConfig\"\n"
                + "    postProcWorkflowsByPath=\"[/content/dam/a:/var/workflow/models/a,/content/dam:/var/workflow/models/dam/dam-autotag-assets]\"/>\n");
        File customerJson = write("ui.config/src/main/content/jcr_root/apps/myapp/config.author/" + PID + ".cfg.json",
                "{\n  // existing\n  \"postProcWorkflowsByExpression\": [\"/content/dam/b/.*:/var/workflow/models/b\"]\n}\n");
        File oldToolOutput = write(MigrationConstants.MIGRATION_PROJECT_APPS + "/src/main/content/jcr_root/apps/aem-cloud-migration/config/" + PID + ".xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<jcr:root xmlns:sling=\"http://sling.apache.org/jcr/sling/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\"\n"
                + "    jcr:primaryType=\"sling:OsgiConfig\"\n"
                + "    postProcWorkflowsByPath=\"[/content/dam/c:/var/workflow/models/c]\"/>\n");

        dao.createConfigByPath(PATH, PATH_MODEL);

        JsonObject config = readConfig(configFile);
        assertEquals(Arrays.asList(
                "/content/dam/a:/var/workflow/models/a",
                "/content/dam/c:/var/workflow/models/c",
                PATH + ":" + PATH_MODEL), getList(config, MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_PATH));
        assertEquals(Arrays.asList("/content/dam/b/.*:/var/workflow/models/b"),
                getList(config, MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_EXPRESSION));

        assertFalse(customerXml.exists());
        assertFalse(customerJson.exists());
        assertFalse(oldToolOutput.exists());
        assertEquals(3, changeTracker.getWorkflowRunnerConfigsMerged().size());
    }

    @Test
    public void testRunModeSpecificConfigsLeftAlone() throws IOException {
        File prodConfig = write("ui.config/src/main/content/jcr_root/apps/myapp/config.author.prod/" + PID + ".cfg.json",
                "{ \"postProcWorkflowsByPath\": [\"/content/dam/p:/var/workflow/models/p\"] }\n");

        dao.createConfigByPath(PATH, PATH_MODEL);

        assertTrue(prodConfig.exists());
        assertEquals(Arrays.asList(PATH + ":" + PATH_MODEL),
                getList(readConfig(configFile), MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_PATH));
        assertEquals(1, changeTracker.getFailedMappings().size());
    }

    private File write(String relPath, String content) throws IOException {
        Path file = tempProjectRoot.toPath().resolve(relPath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file.toFile();
    }

    private JsonObject readConfig(File file) throws IOException {
        return JsonParser.parseString(Files.readString(file.toPath())).getAsJsonObject();
    }

    private List<String> getList(JsonObject config, String prop) {
        List<String> values = new ArrayList<>();
        JsonArray array = config.getAsJsonArray(prop);
        for (JsonElement element : array) {
            values.add(element.getAsString());
        }
        return values;
    }
}

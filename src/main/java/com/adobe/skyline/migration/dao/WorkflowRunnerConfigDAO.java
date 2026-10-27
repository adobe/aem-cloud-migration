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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.exception.MigrationRuntimeException;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.util.StringUtil;
import com.adobe.skyline.migration.util.XmlUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * An object to abstract reading workflow runner service configurations from and writing them to disk.
 * <p>
 * Only one configuration of the Custom Workflow Runner can be active, so the entries of every author configuration
 * already in the project are merged into a single .cfg.json in the migration project and the merged files are removed.
 * Run mode specific configurations are left in place and reported.
 */
public class WorkflowRunnerConfigDAO {

    private static final List<String> CONFIG_EXTENSIONS = Arrays.asList(".cfg.json", ".xml", ".config", ".cfg");
    private static final Set<String> AUTHOR_FOLDERS = new HashSet<>(Arrays.asList(MigrationConstants.CONFIG, MigrationConstants.CONFIG_AUTHOR));
    private static final Set<String> SKIPPED_FOLDERS = new HashSet<>(Arrays.asList("target", "node_modules", ".git"));
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path projectRoot;
    private final Path migrationProject;
    private final Path configFile;
    private final ChangeTrackingService changeTracker;

    private JsonObject config;
    private List<String> byPath;
    private List<String> byExpression;
    private List<Path> mergedFiles;

    public WorkflowRunnerConfigDAO(String projectRoot, ChangeTrackingService changeTracker) {
        this.projectRoot = Paths.get(projectRoot);
        this.migrationProject = this.projectRoot.resolve(MigrationConstants.MIGRATION_PROJECT_APPS);
        this.configFile = Paths.get(migrationProject.toString(), MigrationConstants.PATH_TO_JCR_ROOT,
                MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH, MigrationConstants.WORKFLOW_RUNNER_CONFIG_FILENAME);
        this.changeTracker = changeTracker;
    }

    public void createConfigByExpression(String expression, String model) {
        addConfig(MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_EXPRESSION, expression, model);
    }

    public void createConfigByPath(String path, String model) {
        addConfig(MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_PATH, path, model);
    }

    private void addConfig(String configType, String match, String model) {
        try {
            //Only create the config file if this method has been called.  We don't want to create an empty configuration in the constructor.
            if (config == null) {
                loadExistingConfigs();
            }

            List<String> entries = MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_PATH.equals(configType) ? byPath : byExpression;
            addEntry(entries, match + ":" + model);
            save();
        } catch (IOException e) {
            throw new MigrationRuntimeException(e);
        }
    }

    private void loadExistingConfigs() throws IOException {
        config = new JsonObject();
        byPath = new ArrayList<>();
        byExpression = new ArrayList<>();
        mergedFiles = new ArrayList<>();

        for (Path file : findRunnerConfigs()) {
            String folder = file.getParent().getFileName().toString();
            String name = file.getFileName().toString();

            if (!AUTHOR_FOLDERS.contains(folder)) {
                trackNotMerged(file, "This run mode specific configuration was not merged. Check that it does not override " + relative(configFile) + ".");
            } else if (name.endsWith(".cfg.json")) {
                JsonObject json;
                try {
                    json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                } catch (RuntimeException e) {
                    trackNotMerged(file, "The configuration could not be parsed (" + e.getMessage() + "). Merge its entries into " + relative(configFile) + " by hand.");
                    continue;
                }
                if (file.equals(configFile)) {
                    config = json;
                }
                addEntries(byPath, json.get(MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_PATH));
                addEntries(byExpression, json.get(MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_EXPRESSION));
                markMerged(file);
            } else if (name.endsWith(".xml")) {
                Element root;
                try {
                    root = XmlUtil.loadXml(file.toFile()).getDocumentElement();
                } catch (Exception e) {
                    trackNotMerged(file, "The configuration could not be parsed (" + e.getMessage() + "). Merge its entries into " + relative(configFile) + " by hand.");
                    continue;
                }
                addXmlEntries(byPath, root, MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_PATH);
                addXmlEntries(byExpression, root, MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_EXPRESSION);
                markMerged(file);
            } else {
                trackNotMerged(file, "Only .cfg.json and .xml configurations are merged. Merge its entries into " + relative(configFile) + " by hand.");
            }
        }
    }

    // Customer configurations first, then earlier output of this tool, each in a stable order
    private List<Path> findRunnerConfigs() throws IOException {
        List<Path> found = new ArrayList<>();
        Files.walkFileTree(projectRoot, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return !dir.equals(projectRoot) && SKIPPED_FOLDERS.contains(dir.getFileName().toString())
                        ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (isRunnerConfig(file)) {
                    found.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        found.sort(Comparator.comparing((Path file) -> file.startsWith(migrationProject)).thenComparing(Path::toString));
        return found;
    }

    private boolean isRunnerConfig(Path file) {
        String name = file.getFileName().toString();
        String folder = file.getParent().getFileName().toString();
        if (!folder.equals(MigrationConstants.CONFIG) && !folder.startsWith(MigrationConstants.CONFIG + ".")) {
            return false;
        }
        for (String extension : CONFIG_EXTENSIONS) {
            if (name.equals(MigrationConstants.WORKFLOW_RUNNER_PID + extension)) {
                return true;
            }
        }
        return false;
    }

    private void addEntries(List<String> entries, JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return;
        }
        if (value.isJsonArray()) {
            for (JsonElement element : value.getAsJsonArray()) {
                addEntry(entries, element.getAsString());
            }
        } else {
            addEntry(entries, value.getAsString());
        }
    }

    private void addXmlEntries(List<String> entries, Element root, String prop) {
        // Drop a type hint such as {String}
        String value = root.getAttribute(prop).replaceFirst("^\\{[A-Za-z]+}", "");
        for (String entry : StringUtil.getListFromString(value)) {
            addEntry(entries, entry);
        }
    }

    private void addEntry(List<String> entries, String entry) {
        if (!entry.isEmpty() && !entries.contains(entry)) {
            entries.add(entry);
        }
    }

    private void markMerged(Path file) {
        if (!file.equals(configFile)) {
            mergedFiles.add(file);
        }
    }

    private void save() throws IOException {
        // A path entry for a subfolder must come before the entry for its parent
        byPath.sort(Comparator.comparingInt(WorkflowRunnerConfigDAO::pathDepth).reversed());

        setOrRemove(MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_PATH, byPath);
        setOrRemove(MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_EXPRESSION, byExpression);

        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, GSON.toJson(config) + System.lineSeparator());

        for (Path file : mergedFiles) {
            Files.deleteIfExists(file);
            changeTracker.trackWorkflowRunnerConfigMerged(relative(file));
        }
        mergedFiles.clear();
    }

    private void setOrRemove(String prop, List<String> entries) {
        if (entries.isEmpty()) {
            config.remove(prop);
        } else {
            JsonArray array = new JsonArray();
            entries.forEach(array::add);
            config.add(prop, array);
        }
    }

    private static int pathDepth(String entry) {
        int separator = entry.lastIndexOf(":/");
        String path = separator > 0 ? entry.substring(0, separator) : entry;
        return path.replaceAll("/+$", "").split("/").length;
    }

    private void trackNotMerged(Path file, String reason) {
        changeTracker.trackFailedMappings("Custom Workflow Runner configuration " + relative(file), reason);
    }

    private String relative(Path file) {
        return projectRoot.relativize(file).toString().replace(File.separatorChar, '/');
    }
}

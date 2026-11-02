/*
 Copyright 2026 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.skyline.migration.transformer.processingprofile.mappers;

import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.model.RenditionConfig;
import com.adobe.skyline.migration.model.workflow.WorkflowModel;
import com.adobe.skyline.migration.model.workflow.WorkflowStep;
import com.adobe.skyline.migration.transformer.processingprofile.ProfileMapper;
import com.adobe.skyline.migration.util.StringUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps the ImageMagick commands and thumbnails of a CommandLineProcess step to processing profile renditions.
 * Commands that do more than resize, change quality or convert the format cannot be expressed as a rendition;
 * they are reported as failed mappings so a custom Asset Compute worker can be built for them.
 */
public class CommandLineProcessMapper implements ProfileMapper {

    private static final String PROCESS_ID = "com.day.cq.dam.core.process.CommandLineProcess";

    private static final String COMMANDS_PROP = "COMMANDS";
    private static final String MIME_TYPES_PROP = "MIME_TYPES";
    private static final String THUMBNAILS_PROP = "THUMBNAILS";

    private static final String INPUT_PLACEHOLDER = "${filename}";
    private static final Pattern STANDARD_THUMBNAIL = Pattern.compile("^cq5dam\\.thumbnail\\.(48\\.48|140\\.100|319\\.319)\\.png$");
    private static final Pattern GEOMETRY = Pattern.compile("^(\\d+)x(\\d+)>?$");
    private static final Pattern THUMBNAIL_SIZE = Pattern.compile("^(\\d+):(\\d+)");
    private static final Pattern EXTENSION = Pattern.compile("\\.([A-Za-z0-9]+)$");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{[^}]+}");
    private static final Set<String> IGNORED_OPTIONS = new HashSet<>(Arrays.asList("-strip", "-flatten", "-auto-orient"));
    private static final Set<String> RESIZE_OPTIONS = new HashSet<>(Arrays.asList("-thumbnail", "-resize", "-scale"));
    private static final Map<String, String> FORMATS = new HashMap<>();

    static {
        FORMATS.put("jpg", "jpeg");
        FORMATS.put("jpeg", "jpeg");
        FORMATS.put("png", "png");
        FORMATS.put("gif", "gif");
        FORMATS.put("webp", "webp");
        FORMATS.put("tif", "tiff");
        FORMATS.put("tiff", "tiff");
    }

    private final ChangeTrackingService changeTracker;

    public CommandLineProcessMapper(ChangeTrackingService changeTracker) {
        this.changeTracker = changeTracker;
    }

    @Override
    public String[] getProcessIds() {
        return new String[] {PROCESS_ID};
    }

    @Override
    public List<RenditionConfig> mapToRenditions(WorkflowModel model, WorkflowStep step) {
        Map<String, String> metadata = step.getMetadata();
        Set<String> includes = new HashSet<>();
        if (metadata.containsKey(MIME_TYPES_PROP)) {
            includes.addAll(StringUtil.getListFromString(metadata.get(MIME_TYPES_PROP)));
        }

        List<RenditionConfig> renditions = new ArrayList<>();

        if (metadata.containsKey(COMMANDS_PROP)) {
            for (String command : StringUtil.getListFromString(metadata.get(COMMANDS_PROP))) {
                RenditionConfig rendition = mapCommand(model, command, includes);
                if (rendition != null) {
                    renditions.add(rendition);
                }
            }
        }

        if (metadata.containsKey(THUMBNAILS_PROP)) {
            for (String size : StringUtil.getListFromString(metadata.get(THUMBNAILS_PROP))) {
                Matcher matcher = THUMBNAIL_SIZE.matcher(size);
                if (matcher.find()) {
                    String fileName = "cq5dam.thumbnail." + matcher.group(1) + "." + matcher.group(2) + ".png";
                    if (!STANDARD_THUMBNAIL.matcher(fileName).matches()) {
                        renditions.add(buildRendition(fileName, "png", Integer.parseInt(matcher.group(1)),
                                Integer.parseInt(matcher.group(2)), 0, includes));
                    }
                }
            }
        }

        return renditions;
    }

    private RenditionConfig mapCommand(WorkflowModel model, String command, Set<String> includes) {
        String[] tokens = command.trim().split("\\s+");
        int i;
        if (tokens.length > 1 && tokens[0].equals("magick") && tokens[1].equals("convert")) {
            i = 2;
        } else if (tokens[0].equals("convert") || tokens[0].equals("magick")) {
            i = 1;
        } else {
            return fail(model, command, "it is not an ImageMagick convert command");
        }

        String output = tokens[tokens.length - 1];
        boolean hasInput = false;
        String geometry = null;
        int quality = 0;
        List<String> unknown = new ArrayList<>();

        for (; i < tokens.length - 1; i++) {
            String token = tokens[i];
            if (token.contains(INPUT_PLACEHOLDER)) {
                hasInput = true;
            } else if (token.equals("-define")) {
                // jpeg:size only speeds up decoding; any other -define changes the output
                String value = i + 1 < tokens.length - 1 ? tokens[++i] : "";
                if (!value.toLowerCase().startsWith("jpeg:size=")) {
                    unknown.add("-define " + value);
                }
            } else if (RESIZE_OPTIONS.contains(token) && i + 1 < tokens.length - 1) {
                geometry = tokens[++i];
            } else if (token.equals("-quality") && i + 1 < tokens.length - 1 && tokens[i + 1].matches("\\d+")) {
                quality = Integer.parseInt(tokens[++i]);
            } else if (!IGNORED_OPTIONS.contains(token)) {
                unknown.add(token);
            }
        }

        Matcher extension = EXTENSION.matcher(output);
        String format = extension.find() ? FORMATS.get(extension.group(1).toLowerCase()) : null;

        if (!hasInput) {
            return fail(model, command, "its input is not " + INPUT_PLACEHOLDER);
        }
        if (format == null) {
            return fail(model, command, "the output format of " + output + " has no processing profile equivalent");
        }
        if (!unknown.isEmpty()) {
            return fail(model, command, "the options " + String.join(" ", unknown) + " have no processing profile equivalent");
        }

        int width = 0;
        int height = 0;
        if (geometry != null) {
            Matcher size = GEOMETRY.matcher(geometry);
            if (!size.matches()) {
                return fail(model, command, "the geometry " + geometry + " is not supported, processing profiles only support WIDTHxHEIGHT");
            }
            width = Integer.parseInt(size.group(1));
            height = Integer.parseInt(size.group(2));
        }

        Matcher standard = STANDARD_THUMBNAIL.matcher(output);
        if (standard.matches()) {
            // Every asset gets this rendition already; anything else written under its name cannot be a rendition
            if (standard.group(1).equals(width + "." + height)) {
                return null;
            }
            return fail(model, command, output + " is the name of a standard rendition ("
                    + standard.group(1).replace('.', 'x') + "), but this command produces "
                    + (geometry != null ? geometry : "the original size"));
        }

        String fileName = PLACEHOLDER.matcher(output).replaceAll("")
                .replaceAll("-{2,}", "-")
                .replaceAll("\\.{2,}", ".")
                .replace("-.", ".")
                .replaceAll("^[-.]+", "");

        return buildRendition(fileName, format, width, height, quality, includes);
    }

    private RenditionConfig buildRendition(String fileName, String format, int width, int height, int quality, Set<String> includes) {
        RenditionConfig rendition = new RenditionConfig();
        rendition.setFileName(fileName);
        rendition.setNodeName(fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName);
        rendition.setFormat(format);
        rendition.setWidth(width);
        rendition.setHeight(height);
        rendition.setQuality(quality);
        rendition.setIncludeMimeTypes(new HashSet<>(includes));
        return rendition;
    }

    private RenditionConfig fail(WorkflowModel model, String command, String reason) {
        changeTracker.trackFailedMappings("Command '" + command + "' in " + model.getName(),
                "Not migrated to a processing profile because " + reason + ". A custom Asset Compute worker is required.");
        return null;
    }
}

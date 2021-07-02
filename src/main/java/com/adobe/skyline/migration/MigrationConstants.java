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

package com.adobe.skyline.migration;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.adobe.skyline.migration.model.workflow.WorkflowLauncher;
import com.adobe.skyline.migration.model.workflow.WorkflowStep;

public class MigrationConstants {

    //Node Name Constants
    public static final String VAR = "var";
    public static final String CONF = "conf";
    public static final String GLOBAL = "global";
    public static final String SETTINGS = "settings";
    public static final String ETC = "etc";
    public static final String JCR_CONTENT = "jcr:content";
    public static final String WORKFLOW = "workflow";
    public static final String LAUNCHER = "launcher";
    public static final String CONFIG = "config";
    public static final String MODEL = "model";
    public static final String CONTENT = "content";
    public static final String DAM = "dam";
    public static final String APPS = "apps";
    public static final String MIGRATION_PROJECT_NODE = "aem-cloud-migration";
    public static final String PROCESSING = "processing";
    public static final String VIDEO = "video";
    public static final String SUPPORTED_CODEC = "h264";
    public static final String SUPPORTED_FORMAT = "mp4";

    public static final String JCR_ROOT_ON_DISK = "jcr_root";
    public static final String JCR_CONTENT_ON_DISK = "_jcr_content";

    //Path Constants
    public static final String CONF_ROOT = "/" + CONF;
    public static final String CONF_PATH = CONF_ROOT + "/" + GLOBAL + "/" + SETTINGS;
    public static final String VAR_ROOT = "/" + VAR;
    public static final String ETC_ROOT = "/" + ETC;
    public static final String ETC_PATH = ETC_ROOT + "/" + WORKFLOW;
    public static final String CONTENT_DAM_PATH = "/" + CONTENT + "/" + DAM;
    public static final String COLLECTIONS = "collections";
    public static final String METADATA = "metadata";
    public static final String MIGRATION_PACKAGE_PATH = "/apps/aem-cloud-migration-packages";
    //Filter File Constants
    public static final String FILTER_XML = "filter.xml";
    public static final String FILTER_TAG_NAME = "filter";
    public static final String ROOT_PROPERTY = "root";

    //Maven Constants
    public static final String SRC = "src";
    public static final String MAIN = "main";
    public static final String META_INF = "META-INF";
    public static final String VAULT = "vault";
    public static final String POM_XML = "pom.xml";
    public static final String PARENT_TAG_NAME = "parent";
    public static final String GROUPID_TAG_NAME = "groupId";
    public static final String ARTIFACTID_TAG_NAME = "artifactId";
    public static final String VERSION_TAG_NAME = "version";
    public static final String RELATIVEPATH_TAG_NAME = "relativePath";
    public static final String MODULES_TAG_NAME = "modules";
    public static final String MODULE_TAG_NAME = "module";
    public static final String PACKAGING_TAG_NAME = "packaging";
    public static final String CONTENT_PACKAGE_PACKAGING = "content-package";
    public static final String CONTENT_XML = ".content.xml";
    public static final String PATH_TO_CONTENT = Path.of("", SRC, MAIN, CONTENT).toString();
    public static final String PATH_TO_FILTER_XML = Path.of(PATH_TO_CONTENT, META_INF, VAULT, FILTER_XML).toString();
    public static final String PATH_TO_JCR_ROOT = Path.of(PATH_TO_CONTENT, JCR_ROOT_ON_DISK).toString();

    public static final String MIGRATION_PROJECT_BASE_NAME = "aem-cloud-migration";
    public static final String MIGRATION_PROJECT_CONTENT = MIGRATION_PROJECT_BASE_NAME + ".content";
    public static final String MIGRATION_PROJECT_APPS = MIGRATION_PROJECT_BASE_NAME + ".apps";
    public static final String TEMPLATE_ROOT = "/template/";
    public static final String TEMPLATE_PROJECT_CONTENT_PATH = TEMPLATE_ROOT + MIGRATION_PROJECT_CONTENT;
    public static final String TEMPLATE_PROJECT_APPS_PATH = TEMPLATE_ROOT + MIGRATION_PROJECT_APPS;

    //Workflow Model Constants
    public static final String ETC_MODEL_SUFFIX = "/" + JCR_CONTENT + "/" + MODEL;
    public static final String MODEL_XML = "model.xml";

    //Workflow Launcher Constants
    public static final String PATH_TO_CONF_GLOBAL_LAUNCHER_CONFIG = File.separator + Path.of(PATH_TO_JCR_ROOT, CONF, GLOBAL,  SETTINGS, WORKFLOW, LAUNCHER, CONFIG).toString();
    public static final String PATH_TO_ETC_LAUNCHER_CONFIG = File.separator + Path.of(PATH_TO_JCR_ROOT, ETC, WORKFLOW, LAUNCHER, CONFIG).toString();
    public static final String PATH_TO_CONF_VIDEO_PROFILE = File.separator + Path.of(PATH_TO_JCR_ROOT, CONF, GLOBAL, SETTINGS, DAM, VIDEO).toString();
    public static final String PATH_TO_ETC_VIDEO_PROFILE = File.separator + Path.of(PATH_TO_JCR_ROOT, ETC, DAM, VIDEO).toString();
    public static final String ORIGINAL_RENDITION_PATTERN_SUFFIX = "renditions/original";

    //Workflow Runner Constants
    public static final String WORKFLOW_RUNNER_CONFIG_PATH = "/" + APPS + "/" + MIGRATION_PROJECT_NODE + "/" + CONFIG;
    public static final String WORKFLOW_RUNNER_CONFIG_FILENAME = "com.adobe.cq.dam.processor.nui.impl.workflow.CustomDamWorkflowRunnerImpl.xml";
    public static final String WORKFLOW_RUNNER_CONFIG_BY_EXPRESSION = "postProcWorkflowsByExpression";
    public static final String WORKFLOW_RUNNER_CONFIG_BY_PATH = "postProcWorkflowsByPath";

    //Processing Profile Constants
    public static final String PROCESSING_PROFILE_JCR_PATH = Path.of(CONF, GLOBAL, SETTINGS, DAM, PROCESSING).toString();
    public static final String PROCESSING_PROFILE_DISK_PATH = Path.of(PATH_TO_JCR_ROOT, PROCESSING_PROFILE_JCR_PATH).toString();
    public static final String PROCESSING_PROFILE_RESOURCE_TYPE = "dam/processing/profile";
    public static final String RENDITION_RESOURCE_TYPE = "dam/processing/profile/rendition";
    public static final String VIDEO_RESOURCE_TYPE = "dam/processing/profile/video";
    public static final String INCLUDE_MIMETYPES_PROP = "includeMimeTypes";
    public static final String EXCLUDE_MIMETYPES_PROP = "excludeMimeTypes";
    public static final String NAME_PROP = "name";
    public static final String WIDTH_PROP = "wid";
    public static final String HEIGHT_PROP = "hei";
    public static final String FMT_PROP = "fmt";
    public static final String BITRATE_PROP = "bitrate";
    public static final String CODEC_PROP = "codec";
    public static final String QUALITY_PROP = "qlt";
    public static final String DEFAULT_MIMETYPE = "image/.*";

    //Report Constants
    public static final String REPORT_NAME = "migration-report";
    public static final String REPORT_EXTENSION = "md";
    public static final String REPORT_FILENAME = REPORT_NAME + "." + REPORT_EXTENSION;
    public static final String REPORT_TEMPLATE_FILENAME = "report-template.md";
    public static final String NO_LAUNCHER_MSG = "No workflow launchers were disabled.";
    public static final String NO_RUNNER_CFG_MSG = "No workflow runner configurations were created.";
    public static final String NO_MODEL_UPDATE_MSG = "No workflow models were modified.";
    public static final String NO_PATHS_DELETED_MSG = "No paths were deleted.";
    public static final String NO_PROFILE_MSG = "No processing profiles were created.";
    public static final String NO_FAILURE_MSG = "No issues were encountered.";
    public static final String NO_PROJECT_MSG = "No Maven projects were created.";

    //Filetype Constants
    public static final String PNG_EXTENSION = "png";
    public static final String JPEG_EXTENSION = "jpeg";
    public static final String XML_EXTENSION = "xml";

    //XML Constants
    public static final String FLOW_NODE = "flow";
    public static final String NODES_NODE = "nodes";
    public static final String NODE_PREFIX = "node";
    public static final String NODE_SPACE = "_x0023_";
    public static final String METADATA_XML_NODE = "metaData";
    public static final String TRANSITIONS_NODE = "transitions";
    public static final String JCR_ROOT_NODE = "jcr:root";

    public static final String NS_SLING = "xmlns:sling";
    public static final String NS_SLING_VALUE = "http://sling.apache.org/jcr/sling/1.0";
    public static final String NS_JCR = "xmlns:jcr";
    public static final String NS_JCR_VALUE = "http://www.jcp.org/jcr/1.0";
    public static final String NS_CQ = "xmlns:cq";
    public static final String NS_CQ_VALUE = "http://www.day.com/jcr/cq/1.0";
    public static final String NS_NT = "xmlns:nt";
    public static final String NS_NT_VALUE = "http://www.jcp.org/jcr/nt/1.0";

    //Property Constants
    public static final String JCR_PRIMARY_TYPE_PROP = "jcr:primaryType";

    public static final String JCR_TITLE_PROP = "jcr:title";
    public static final String JCR_DESCRIPTION_PROP = "jcr:description";

    public static final String SLING_RESOURCE_TYPE_PROP = "sling:resourceType";

    public static final String ENABLED_PROP = "enabled";

    public static final String GLOB_PROP = "glob";
    public static final String CONDITION_PROP = "condition";
    public static final String CONDITIONS_PROP = "conditions";
    public static final String EXCLUDE_LIST_PROP = "excludeList";
    public static final String WORKFLOW_MODEL_PROP = "workflow";

    public static final String DESCRIPTION_PROP = "description";
    public static final String TITLE_PROP = "title";
    public static final String TYPE_PROP = "type";
    public static final String FROM_PROP = "from";
    public static final String TO_PROP = "to";

    public static final String PROCESS_PROP = "PROCESS";
    public static final String EXTERNAL_PROCESS_PROP = "EXTERNAL_PROCESS";
    public static final String PROCESS_AUTO_ADVANCE_PROP = "PROCESS_AUTO_ADVANCE";

    public static final String MERGE_LIST_PROPERTY = "mergeList";

    //Value Constants
    public static final String WORKFLOW_LAUNCHER_TYPE_VALUE = "cq:WorkflowLauncher";
    public static final String WORKFLOW_MODEL_RESOURCE_TYPE_VALUE = "cq/workflow/components/pages/model";
    public static final String WORKFLOW_NODE_TYPE_VALUE = "cq:WorkflowNode";
    public static final String WORKFLOW_TRANSITION_TYPE_VALUE = "cq:WorkflowTransition";
    public static final String CQ_PAGE_TYPE = "cq:Page";
    public static final String OSGI_CONFIG_TYPE_VALUE = "sling:OsgiConfig";
    public static final String NT_UNSTRUCTURED_TYPE_VALUE = "nt:unstructured";

    public static final String TRUE_VALUE = "{Boolean}true";
    public static final String FALSE_VALUE = "{Boolean}false";

    public static final String COMPLETED_PROCESS = "com.day.cq.dam.core.impl.process.DamUpdateAssetWorkflowCompletedProcess";
    public static final String DELETE_PREVIEW_PROCESS = "com.day.cq.dam.core.process.DeleteImagePreviewProcess";

    public static final Map<String, WorkflowLauncher> DEFAULT_ENABLED_MODELS = new HashMap<>() {{
        put("batch-thumbnails", buildWorkflowLauncher("/var/dam/pending-thumbs(/.*)", Arrays.asList("paths!=")));
        put("dynamic-media-encode-video", buildWorkflowLauncher("/content/dam(/.*/)renditions/original", Arrays.asList("jcr:content/jcr:mimeType==video/.*")));
        put("process_subasset", buildWorkflowLauncher("/content/dam(/.*/)(subassets)(/.*/)renditions/original", Arrays.asList("jcr:content/jcr:mimeType!=video/.*")));
        put("update_asset", buildWorkflowLauncher("/content/dam(/((?!/subassets).)*/)renditions/original", new ArrayList<>()));
        put("update_from_lightbox", buildWorkflowLauncher("/var/lightbox", new ArrayList<>()));
    }};

    //Object Constants
    public static final WorkflowStep WORKFLOW_COMPLETED_PROCESS = buildWorkflowCompletedProcess();

    private static WorkflowStep buildWorkflowCompletedProcess() {
        WorkflowStep step = new WorkflowStep();

        step.setProcess(COMPLETED_PROCESS);
        step.setNodeName("damupdateassetworkflowcompletedprocess");
        step.setDescription("This process will send DamEvent.Type.DAM_UPDATE_ASSET_WORKFLOW_COMPLETED event when DAM update asset workflow is completed");
        step.setResourceType("dam/components/workflow/damupdateassetworkflowcompletedprocess");
        step.setTitle("DAM Update Asset Workflow Completed");

        return step;
    }

    private static WorkflowLauncher buildWorkflowLauncher(String glob, List<String> conditions) {
        WorkflowLauncher launcher = new WorkflowLauncher();
        launcher.setGlob(glob);
        launcher.setConditions(conditions);
        launcher.setSynthetic(true);
        return launcher;
    }
}

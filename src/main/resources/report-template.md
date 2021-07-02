# AEM Assets as a Cloud Service - Workflow Migration Report
This report contains the changes that have been made to your Maven project by the workflow migration tool.  After reviewing these changes, you should be able to simply run a `mvn clean install` command against your reactor POM file to build your project with these changes in place.  After validating these changes on a local developer environment, check these changes into your source control repository and run a Cloud Manager build to deploy these changes to your AEM in the Cloud environments.

## Maven Projects Added and Modified
Changes to your workflow models and launchers have been made in-place.  We have also added the following new Maven projects:

${PROJECTS_CREATED}
These projects have been added as modules to your reactor POM.  If you have already migrated your Maven projects to the new mutable/immutable content structures, we have also added these projects to embedded packages and dependencies in your project's container content package.

If you have not yet migrated your Maven projects to this new paradigm or are unsure what this means, please see [Understand the Structure of a Project Content Package in Adobe Experience Manager Cloud Service](https://docs.adobe.com/content/help/en/experience-manager-cloud-service/implementing/developing/aem-project-content-package-structure.html) for more information.

## Workflow Launchers
We have disabled some workflow launchers.  Depending on your source code, these will either be located under `/conf/global/settings/workflow/launcher/config` or `/etc/workflow/launcher/config`.  The Asset Compute Service will handle most asset processing in the cloud and any remaining custom workflow steps to be executed will be handled via the Custom Workflow Runner service.  We have disabled the following launchers:

${LAUNCHERS_DISABLED}
Note that in AEM Assets as a Cloud Service environments, out-of-the-box workflow launchers for DAM Update Asset and DAM Metadata Writeback workflows are disabled by default.

## Custom Workflow Runner configuration
We have created a configuration for the Custom Workflow Runner service in the `aem-cloud-migration.apps` project.  This service replaces the role of traditional workflow launchers for asset processing workflows in AEM Assets as a Cloud Service deployments.  Since the Asset Compute Service will be processing all of our image renditions, we can no longer rely on a simple JCR event to execute our workflows.  Instead, this service will be invoked when the Asset Compute Service has completed processing and the assets are ready for custom steps to be executed against them.  If you use Dynamic Media, note that we will upload assets to Dynamic Media before executing these workflows.  We have configured the following workflows:

${RUNNER_CONFIGS_CREATED}
Note that when using the `postProcWorkflowsByExpression` method of configuring this service, the glob patterns used will differ slightly from the patterns used for workflow launchers.  This is due to the fact that while workflow launchers usually target the original asset binary, the Custom Workflow Runner targets the asset itself.

## Workflow Model Updates
Many workflow models will combine Adobe-provided asset processing steps with custom customer-defined steps.  In these cases, we have removed most of the Adobe-provided steps from the workflow and configured the workflow to be executed by the Custom Workflow Runner.  When executing workflows via the Custom Workflow Runner service, we require that the workflow end with the DAM Update Asset Completed Process workflow step.  This will mark the asset as _Complete_.  As a result, we have added this step in any workflows where it was missing. 

We have made changes to the following workflow models:

${WORKFLOW_MODELS_TRANSFORMED}
## Paths Deleted
Workflow nodes under /var paths are no longer required for deployment to AEM as a Cloud Service.  These nodes will automatically be generated at system startup for any workflow models that have previously been synced.  In order to prevent potential deployment issues, the following /var paths have been removed from your Maven projects and filter files:

${VAR_PATHS_DELETED}
## Asset Compute Service Processing Profiles
By inspecting your workflow step configurations, we were able to automatically generate processing profiles for the Asset Compute Service to encompass configurations that you have customized.  These configurations can be found in the (`aem-cloud-migration.content`) project.

${PROCESSING_PROFILES_CREATED}
Note that while we have created processing profiles from your workflow model configurations, we are unable to attach these profiles to your existing content structures.  After deploying these profiles to your environment, please visit /mnt/overlay/dam/gui/content/processingprofiles/processingprofiles.html to attach these configurations to one or more folders in your repository.

## Migration Issues
These issues were encountered while migrating the project by workflow migration tool.

${MIGRATION_ISSUES}

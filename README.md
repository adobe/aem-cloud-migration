# AEM Assets as a Cloud Service - Workflow Migration Tool

This tool can be used to automatically migrate asset processing workflows from on-premise or AMS deployments of AEM to processing profiles and OSGi configurations for use in AEM Assets as a Cloud Service.

## Goals

The goal of this project is to make it as simple as possible for AEM developers to migrate existing AEM asset processing workflows to the cloud.

## Non-Goals

This project is specifically focused on asset processing workflows.  While there are other types of migrations that may also be necessary for a customer to migrate to the cloud, they would be considered outside of the scope of this particular project.

### Installation

After downloading the latest release from the releases page, the JAR can be executed with the command `java -jar wf-migrator-VERSION.jar [PROJECT_DIR]`.  For additional information and options, please see the documentation.

### Usage

This script will perform an automated migration from custom workflow configurations for asset processing to the corresponding configurations that are required by AEM as a Cloud Service.  After executing the script, the transformed code can be committed to a test branch and deployed to a Cloud Service development environment for testing and validation.

When run, the script will perform the following actions:

#### Create Maven Projects
Up to Maven projects will be created:

- aem-cloud-migration.apps - for immutable content that is to be deployed under /apps
- aem-cloud-migration.content - for mutable content that is to be deployed elsewhere, such as /conf

Each project will only be created if it is required.  The created projects will be added as modules to the reactor POM.  In cases where the project has been migrated to follow the [new package structures](https://docs.adobe.com/content/help/en/experience-manager-cloud-service/implementing/developing/aem-project-content-package-structure.html), we will integrate these projects into the container content package as well.

#### Disable Launchers
The script will disable launchers for asset-based workflows.  The Asset Compute Service will handle most asset processing in the cloud and any remaining custom workflow steps to be executed will need to be handled via the Custom Workflow Runner service.

#### Transform Workflow Models
For any workflow models that contain steps that will still need to be run on AEM as a Cloud Service, such as custom workflow steps, we will transform the existing workflow models to remove all unsupported steps and to add the DAM Update Asset Workflow Completed Process step where needed.

#### Configure Custom Workflow Runner
In cases where asset workflows will still be required, we will create an OSGi configuration for the Custom Workflow Runner.  This service replaces workflow launchers as the way to execute workflows upon the completion of processing via the Asset Compute Service and Dynamic Media.

#### Create Processing Profiles
Processing Profiles for the Asset Compute service will be created based on configurations that have been made for supported out-of-the-box workflow steps.  Note that while we are able to generate the processing profiles and store them in the Maven source project, we are not able to deploy the configurations that are required to actually attach them to the content hierarchy.  After deploying the profiles to your environment, you will need to attach them to the desired folders in your AEM environment via the folder properties or through the Processing Profile UI.

#### Create a Migration Report
Finally, a report will be output, in Markdown format, that outlines all of the changes that the script has made.

### Known Issues
We are not currently able to parse and process AND or OR splits.  The migration tool will do its best to process the workflow steps _around_ the split, but the split itself will not be migrated.  If you receive a message in the command line output regarding one of these splits, you may need to manually inspect and migrate these configurations.

### Building

To build from source, use Maven.  From the root of the project, run `mvn clean install` to build the code and execute the unit tests.  The compiled JAR can then be found in the `target` directory.

### Contributing

Contributions are welcomed! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.

### Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.

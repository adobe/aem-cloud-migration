# AEM Assets as a Cloud Service - Workflow Migration Tool

This tool can be used to automatically migrate asset processing workflows from on-premise or AMS deployments of AEM to processing profiles and OSGi configurations for use in AEM Assets as a Cloud Service.

## Goals

The goal of this project is to make it as simple as possible for AEM developers to migrate existing AEM asset processing workflows to the cloud.

## Non-Goals

This project is specifically focused on asset processing workflows.  While there are other types of migrations that may also be necessary for a customer to migrate to the cloud, they would be considered outside of the scope of this particular project.

### Installation

After downloading the latest release from the [releases](./releases) page, the JAR can be executed with the command `java -jar wf-migrator-0.1.0.jar [PROJECT_DIR]`.  For additional information and options, please see the documentation.

### Usage

Documentation is available at TODO

### Building

To build from source, use Maven.  From the root of the project, run `mvn clean install` to build the code and execute the unit tests.  The compiled JAR can then be found in the `target` directory.

### Contributing

Contributions are welcomed! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.

### Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
# PDS Change History

The PDS Change History project is used to gather release information such as approver and reviewer names as well as approval or review dates for promotable and/or changeable objects.
It allows for easy collection of information from promotion and change workflows, informationn that can then be used to show a table witht his information on Creo drawings. 
There is also an extra function that can be used in conjunction with the PDS Stamping Service feature to create a cover page with this release information as part of the OOTB publishing process.

## Primary Functions

### 1. Setting Change History Attributes on EPMDocuments

The system stores a concatenated string containing release and approval information on EPMDocuments. This is primarily implemented in `ChangeHistoryProcessor.java`.

- Creates a formatted string with revision, reason, date, and release information
- Maintains a historical record of releases (up to a configurable number, default 8)
- Data sources:
   - EPMDocument attributes (revision, etc.)
   - WTChangeOrder2 attributes (reason, etc.)
   - PromotionNotice attributes
- The concatenated string is stored in a configurable IBA (Information Based Attribute)

#### Attribute Construction Flow:
1. When a promotion notice or change order is processed
2. The system collects data from both the EPMDocument and associated change objects
3. A formatted string is created with fields separated by delimiters
4. Multiple entries are combined using `{}` as separators
5. The result is stored in the `releaseInfo` IBA (or configured alternative)

### 2. Modeled Attribute Delegation

The system breaks down the concatenated change history string into separate attributes that can be used in tables and other UI elements. This is implemented primarily in `ChangeHistoryModeledAttributesDelegate.java` and `ChangeHistoryAttributeHelper.java`.

- Parses the stored history string into separate entries
- Formats and exposes individual attributes for each historical entry
- Makes these attributes available to the Windchill Pro/E interface

#### Attribute Delegation Flow:
1. When files are added to a workspace, the delegate is called
2. The delegate retrieves the change history from the EPMDocument
3. It parses the concatenated string into separate `ChangeHistoryInfo` objects
4. For each entry, it creates modeled attributes with standardized naming:
   - `CH_INDEX_n`: Revision information
   - `CH_DESC_n`: Description/reason for the change
   - `CH_DATE_n`: Release date
   - `CH_USER_n`: User who released/approved the change

### 3. Report Generation

The system generates formatted PDF reports containing change history information. This is implemented in the `report` package.

- Creates reports from both types of change objects:
   - WTChangeOrder2 (ECNs)
   - PromotionNotice (Promotion Requests)
- Includes information about the change and affected parts
- Formats the data in a readable PDF format
- Caches reports for efficiency

#### Report Generation Flow:
1. A report is requested for a change object (either a PromotionNotice or WTChangeOrder2)
2. The `ReportProcessor` creates a `ReportModel` with data from the change and affected parts
3. The `ReportGenerator` transforms this model to XML using a Velocity template
4. The XML is then transformed to PDF using Apache FOP
5. The generated PDF is cached for future requests

## Attributes by Object Type

### EPMDocument Attributes
- `PDS_CHANGE_HISTORY_RELEASE_DATE`: Date when document was released
- `PDS_CHANGE_HISTORY_RELEASED_BY`: User who released the document
- `PDS_CHANGE_HISTORY_RELEASE_INFO`: Concatenated string containing all change history entries

### Change Notice and Promotion Request Attributes
The following attributes can be found on both ECNs and Promotion Requests:
- `PDS_CHANGE_HISTORY_REASON`: Detailed reason for the change
- `PDS_CHANGE_HISTORY_REASON_SOURCE`: Source of the change request
- `PDS_CHANGE_HISTORY_SHORT_REASON`: Brief description of the change
- `PDS_CHANGE_HISTORY_INFO_SERVICE`: Information relevant to service department
- `PDS_CHANGE_HISTORY_INFO_MARKETING`: Information relevant to marketing department
- `PDS_CHANGE_HISTORY_IMPACT_TO_EXISTING_PARTS`: Description of impact on existing parts

### Modeled Attributes (Generated)
- `CH_INDEX_n`: Revision information for entry n
- `CH_DESC_n`: Description/reason for entry n
- `CH_DATE_n`: Release date for entry n
- `CH_USER_n`: User who released/approved entry n

## Configuration

All attribute names and behaviors are configurable through properties in the Windchill properties file. Key configuration options:

- `ext.changeHistory.ibaReleasedDate`: IBA name for release date
- `ext.changeHistory.ibaReleasedBy`: IBA name for released by user
- `ext.changeHistory.ibaReleaseInfo`: IBA name for concatenated change history
- `ext.changeHistory.max`: Maximum number of history entries to maintain
- `ext.changeHistory.docType`: Document type to process (default: CADDRAWING)
- `ext.changeHistory.dateFormat`: Format for date display (default: yyyy-MM-dd)
- `ext.changeHistory.filterCategory`: Whether to filter by document category
- `ext.changeHistory.resolvedCNState`: State name for resolved change notices
- `ext.changeHistory.getReasonFromChange`: Whether to get reason from change object

## Data Format

Change history entries are stored in a concatenated string with the following format:
```
revision|reason|date|releaseBy{}revision|reason|date|releaseBy{} ...
```

Each field is separated by a pipe (`|`), and entries are separated by `{}`.


# Gradle Setup
## Requirements

This project is using [Gradle](https://gradle.org) as the build tool.
Either use the included [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) or a local
installation of a recent version.

Independent of the target Java version of this project, Gradle needs
the [current LTS version of Java](https://whichjdk.com).
At this moment this would be [Java 21](https://openjdk.org/projects/jdk/21/).

## Project structure

The structure of this project closely resembles that of a CCD project.
For a detailed explanation
see [CCD PDSVISION Repository Structure Reference](https://tools.pdsvision.com/dlab/bin/view/PDSVISION/Standards/Project_Structure/CCD_PDSVISION_Repository_Structure_Reference/).

## Configuration

### Configuring Java Installations in `gradle.properties`

Gradle allows you to specify multiple Java installations and control whether it should automatically detect installed
JDKs. This can be configured using the `org.gradle.java.installations.paths` and
`org.gradle.java.installations.auto-detect` properties in the `gradle.properties` file.

#### Steps to Configure Java Installations

1. **Locate or Create `gradle.properties`**

   The `gradle.properties` file can be located in the following places:
    - Project-level: `PROJECT_ROOT/gradle.properties`
    - User-level: `USER_HOME/.gradle/gradle.properties`

2. **Specify Java Installation Paths**

   Use the `org.gradle.java.installations.paths` property to specify the paths to your JDK installations. This property
   accepts a comma-separated list of paths.

   ```properties
   org.gradle.java.installations.paths=/path/to/jdk11,/path/to/jdk17
   ```

   For example, if you have JDK 11 and JDK 17 installed at `/usr/lib/jvm/java-11-openjdk` and
   `/usr/lib/jvm/java-17-openjdk`, you would add:

   ```properties
   org.gradle.java.installations.paths=/usr/lib/jvm/java-11-openjdk,/usr/lib/jvm/java-17-openjdk
   ```

3. **Enable or Disable Auto-Detection**

   Use the `org.gradle.java.installations.auto-detect` property to enable or disable the automatic detection of
   installed JDKs. Set this property to `true` to enable auto-detection or `false` to disable it.

   ```properties
   org.gradle.java.installations.auto-detect=true
   ```

   By default, Gradle will attempt to auto-detect installed JDKs. If you want to disable this feature and rely solely on
   the paths specified, set the property to `false`.

   ```properties
   org.gradle.java.installations.auto-detect=false
   ```

4. **Verify Configuration**

   You can verify the Java installations being used by Gradle by running:

   ```sh
   ./gradlew -q javaToolchains
   ```

   This command will list the available Java toolchains and the ones currently in use.

#### Example

Here's an example of a `gradle.properties` file with both properties configured:

```properties
# Specify paths to JDK installations
org.gradle.java.installations.paths=/usr/lib/jvm/java-11-openjdk,/usr/lib/jvm/java-17-openjdk
# Enable auto-detection of installed JDKs
org.gradle.java.installations.auto-detect=true
# Other properties
org.gradle.daemon=true
org.gradle.parallel=true
```

#### Additional Resources

- [Gradle Properties Documentation](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties)
- [Java Toolchains in Gradle](https://docs.gradle.org/current/userguide/toolchains.html#sec:java_toolchains)

### Configuring credentials for Sonatype Nexus Repository in `gradle.properties`

This project uses dependency that are hosted in the companies own Sonatype Nexus Repository that is located
at <https://tools.pdsvision.com/nexus/>.
Access to artefacts from the repository required authentication.
Therefore, the following properties need to be configured in the `gradle.properties` file.

```properties
# Sonatype Nexus Repository - PDSVISION
nexusUrl=https://tools.pdsvision.com/nexus
nexusUser=john.smith
nexusPassword=super-safe-password
```

## Contribution Guidelines

We welcome contributions! To ensure a smooth and efficient process, please follow these guidelines:

1. **Feature Branches**: Create a new branch for each feature or bug fix.
   Use descriptive names for your branches, such as `feature/123_add-login` or `bugfix/123_fix-crash`.

2. **Merge Requests**: Once your feature or fix is ready, open a merge request (MR).
   Ensure your MR includes a clear description of the changes and references any related issues.

3. **Code Review**: All merge requests will undergo a code review by the project maintainer.
   Please be patient as this process can take some time.

4. **Maintainer**: The project maintainer is responsible for merging your changes into the main branch.
   If you have any questions or need assistance, feel free to reach out to the maintainer.

Thank you for your contributions and for helping to improve this project!

## Maintainer

This project is actively maintained by:

- [Daniel Martinsson](mailto:daniel.martinsson@pdsvision.com)

If you have any questions, suggestions, or issues, please feel free to reach out.

Please refer to our [Contributing Guidelines](#contribution-guidelines) for more information
on how to get involved.

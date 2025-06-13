rootProject.name = "pds-change-history"

// Defines any module to consider for this project.
// Changes here probably also require an update to
// the dependencies of the root project
// in the file "build.gradle.kts".
include(":pds-change-history-core")

plugins {
    // Plugin for automatic provisioning of JDKs.
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}

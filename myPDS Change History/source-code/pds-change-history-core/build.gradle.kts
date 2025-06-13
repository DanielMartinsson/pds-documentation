group = rootProject.group
version = rootProject.version

// See https://docs.gradle.org/current/userguide/version_catalogs.html for
// details about usage of the version catalogue for dependencies.
dependencies {
    annotationProcessor(libs.lombok)

    compileOnly(libs.bundles.windchill)
    compileOnly(libs.lombok)

    implementation(libs.velocity)
    implementation(libs.commons.text)

    testAnnotationProcessor(libs.lombok)

    testCompileOnly(libs.bundles.testing)
    testCompileOnly(libs.bundles.windchill)
    testCompileOnly(libs.lombok)

}

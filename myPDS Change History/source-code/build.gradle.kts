import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension

buildscript {
    // private repository for custom plugin with deployment tasks
    repositories {
        mavenCentral()
        maven {
            url = uri("${providers.gradleProperty("nexusUrl").get()}/content/repositories/snapshots")
            credentials {
                username = providers.gradleProperty("nexusUser").get()
                password = providers.gradleProperty("nexusPassword").get()
            }
        }
    }
    dependencies {
        classpath("ext.pdsvision:pds-deployment-gradle-plugin:1.0.0-SNAPSHOT")
    }
}

plugins {
    `java-library-distribution`
    `maven-publish`

    idea

    id("org.sonarqube") version "6.2.0.5505"
    id("com.netflix.nebula.info") version "14.0.0"
    id("org.cyclonedx.bom") version "2.3.1"
    id("org.owasp.dependencycheck") version "12.1.3"
    id("com.github.jk1.dependency-license-report") version "2.9"
    id("com.github.ben-manes.versions") version "0.52.0"
}

apply<ext.pdsvision.gradle.deployment.DeploymentPlugin>()

group = "ext.pdsvision"
version = "1.0.0-SNAPSHOT"

// The configuration is defined in the deployment plugin,
// but must be referenced explicitly to be usable for
// dependency declaration.
val pdsvisionDeployment: Configuration = configurations["pdsvisionDeployment"]

dependencies {
    // The subprojects to include in the root project and therefore also in the deployment package.
    // When renaming module directories, these references must also be updated.
    // When changing anything about modules, probably an update in the file settings.gradle.kts
    // is also necessary.
    implementation(project(":pds-change-history-core"))

    // Application to integrate into the deployment package.
    // Declaration uses a custom configuration type to help identification
    // during the build process.
    pdsvisionDeployment(group = "ext.pdsvision", name = "pds-deployment", version = "1.0.0-SNAPSHOT", ext = "zip")
}

configure<DependencyCheckExtension> {
    // Configures the plugin for the dependency check.
    // To improve index update speed, an API key for the NIST NVD API can
    // be declared in the environment variable "nist_nvd_api_key".
    // Not necessary for local development but will be used by the build server.
    nvd.apiKey = providers.gradleProperty("nist_nvd_api_key").getOrElse("")
    scanConfigurations = listOf("runtimeClasspath", "testRuntimeClasspath")
    formats = mutableListOf("ALL")
}

allprojects {
    apply<JavaLibraryPlugin>()
    apply<MavenPublishPlugin>()
    apply<IdeaPlugin>()

    apply<ext.pdsvision.gradle.deployment.DeploymentPlugin>()

    java {
        // Defines the target Java version of the project and its modules.
        toolchain {
            // Windchill 12.0 uses Java 11
            // Windchill 12.1 uses Java 11
            // Windchill 13.0 uses Java 17
            // Windchill 13.1 uses Java 21
            languageVersion = JavaLanguageVersion.of(17)
        }
        withJavadocJar()
        withSourcesJar()
    }

    idea {
        // Configures IntelliJ to also download any Javadoc and source JAR files of dependencies.
        module {
            isDownloadSources = true
            isDownloadJavadoc = true
        }
    }

    publishing {
        // Defines the artefacts and target repositories for publishing.
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
        repositories {
            maven {
                afterEvaluate {
                    // https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:deferred_configuration
                    url =
                        if (version.toString().endsWith("SNAPSHOT")) {
                            println("version:$version")
                            uri("${providers.gradleProperty("nexusUrl").get()}/content/repositories/snapshots")
                        } else {
                            println("version:$version")
                            uri("${providers.gradleProperty("nexusUrl").get()}/content/repositories/releases")
                        }
                }
                credentials {
                    username = providers.gradleProperty("nexusUser").get()
                    password = providers.gradleProperty("nexusPassword").get()
                }
            }
        }
    }

    repositories {
        // Defines the repositories to use for retrieving dependencies.
        mavenCentral()
        maven {
            url = uri("${providers.gradleProperty("nexusUrl").get()}/content/repositories/releases")
            credentials {
                username = providers.gradleProperty("nexusUser").get()
                password = providers.gradleProperty("nexusPassword").get()
            }
        }
        maven {
            url = uri("${providers.gradleProperty("nexusUrl").get()}/content/repositories/snapshots")
            credentials {
                username = providers.gradleProperty("nexusUser").get()
                password = providers.gradleProperty("nexusPassword").get()
            }
        }
    }
}

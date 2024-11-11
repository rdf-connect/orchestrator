plugins {
  kotlin("jvm")
  id("co.uzzu.dotenv.gradle")
  id("maven-publish")
  id("org.jetbrains.dokka")
}

/** The target JDK. */
val jdkVersion: String by project

kotlin { jvmToolchain(jdkVersion.toInt()) }

allprojects {
  /** The current version number of the RDF-Connect Orchestrator. */
  val projectVersion: String by project
  version = projectVersion

  /** The group name of the project. */
  val projectGroup: String by project
  group = projectGroup

  // Make the GH Packages repository available.
  repositories {
    mavenCentral()

    maven {
      url = uri("https://maven.pkg.github.com/rdf-connect/orchestrator")
      credentials {
        username = env.fetchOrNull("GITHUB_ACTOR") ?: System.getenv("GITHUB_ACTOR")
        password = env.fetchOrNull("GITHUB_TOKEN") ?: System.getenv("GITHUB_TOKEN")
      }
    }
  }
}

subprojects {
  // Shared plugins.
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "maven-publish")
  apply(plugin = "org.jetbrains.dokka")

  // Configure testing framework.
  tasks.test {
    useJUnitPlatform()

    maxParallelForks = 1

    testLogging {
      events("passed", "skipped", "failed")
      showStandardStreams = true
    }
  }

  publishing {
    repositories {
      maven {
        url = uri("https://maven.pkg.github.com/rdf-connect/orchestrator")
        credentials {
          username = env.fetchOrNull("GITHUB_ACTOR") ?: System.getenv("GITHUB_ACTOR")
          password = env.fetchOrNull("GITHUB_TOKEN") ?: System.getenv("GITHUB_TOKEN")
        }
      }
    }
  }
}

plugins {
  kotlin("jvm") version "2.0.21"
  id("maven-publish")
}

group = "technology.idlab"

version = "0.0.2"

kotlin { jvmToolchain(22) }

repositories { mavenCentral() }

dependencies {
  // Local dependencies
  implementation(project(":rdfc-core"))

  // Kotlin extensions
  implementation(kotlin("reflect"))

  // KTest
  testImplementation(kotlin("test"))
}

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
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/rdf-connect/orchestrator")
      credentials {
        username = env.fetchOrNull("GITHUB_ACTOR") ?: System.getenv("GITHUB_ACTOR")
        password = env.fetchOrNull("GITHUB_TOKEN") ?: System.getenv("GITHUB_TOKEN")
      }
    }
  }

  publications {
    create<MavenPublication>("gpr") {
      from(components["java"])
      groupId = "technology.idlab"
      artifactId = "rdfc-processor"
      version = "0.0.1"
    }
  }
}

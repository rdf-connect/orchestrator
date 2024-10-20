plugins {
  kotlin("jvm") version "2.0.21"
  id("maven-publish")
  id("co.uzzu.dotenv.gradle") version "4.0.0"
}

group = "technology.idlab"

version = "0.0.4"

/** Set the Kotlin JVM version to 22. */
kotlin { jvmToolchain(22) }

/**
 * A list of all the repositories we use in the project. This includes the maven central repository
 * and the GitHub package repository.
 */
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
    register<MavenPublication>("gpr") {
      artifactId = "orchestrator"
      from(components["java"])
    }
  }
}

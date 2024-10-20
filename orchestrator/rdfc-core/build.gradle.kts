plugins {
  kotlin("jvm") version "2.0.21"
  id("maven-publish")
}

group = "technology.idlab"

version = "0.0.1"

kotlin { jvmToolchain(22) }

repositories { mavenCentral() }

dependencies {
  // HTTP dependency
  implementation("io.ktor:ktor-client-core:2.3.10")

  // Kotlin extensions
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

  // RDF dependencies
  implementation("org.apache.jena:apache-jena-libs:5.0.0")
  implementation("org.apache.jena:jena-arq:5.0.0")
  implementation("org.apache.jena:jena-shacl:5.0.0")

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
      artifactId = "rdfc-core"
      version = "0.0.1"
    }
  }
}

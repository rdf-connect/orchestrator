plugins {
  application
  id("com.github.johnrengelman.shadow") version "8.1.1"
  kotlin("jvm") version "1.9.22"
  id("maven-publish")
  id("co.uzzu.dotenv.gradle") version "4.0.0"
}

/** JVM Runner configuration. */
group = "technology.idlab"

version = "0.0.1"

/** Set the Kotlin JVM version to 17. */
kotlin { jvmToolchain(17) }

/**
 * Specify the entrypoint for the application. This is a simple CLI interface wrapper which
 * initializes the parsers and the runner.
 */
application { mainClass.set("technology.idlab.MainKt") }

/**
 * Include all dependencies in a "fat jar". We use the shadowJar plugin to merge all dependencies
 * into a single jar file.
 */
tasks.shadowJar {
  manifest.attributes.apply { put("Main-Class", "technology.idlab.MainKt") }
  mergeServiceFiles()
}

/** Use JUnit for testing. */
tasks.test { useJUnitPlatform() }

/**
 * A list of all the repositories we use in the project. This includes the maven central repository
 * and the GitHub package repository.
 */
repositories {
  mavenCentral()

  maven {
    url = uri("https://maven.pkg.github.com/rdf-connect/jvm-runner")
    credentials {
      username = env.GITHUB_ACTOR.value
      password = env.GITHUB_TOKEN.value
    }
  }
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

  // Reflections
  implementation("org.reflections:reflections:0.10.2")

  // Guava
  implementation("com.google.guava:guava:33.2.0-jre")

  // K2JVM Compiler.
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
  implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.22")
  implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.9.22")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")

  // HTTP dependencies.
  implementation("io.ktor:ktor-client-core:2.3.10")
  implementation("io.ktor:ktor-client-cio:2.3.10")
  implementation("io.ktor:ktor-server-core:2.3.10")
  implementation("io.ktor:ktor-server-netty:2.3.10")
  testImplementation("io.ktor:ktor-client-mock:2.3.10")

  // RDF dependencies.
  implementation("org.apache.jena:apache-jena-libs:5.0.0")
  implementation("org.apache.jena:jena-arq:5.0.0")

  // Initialize testing.
  testImplementation("org.jetbrains.kotlin:kotlin-test")
}

publishing {
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/rdf-connect/jvm-runner")
      credentials {
        username = env.GITHUB_ACTOR.value
        password = env.GITHUB_TOKEN.value
      }
    }
  }

  publications {
    register<MavenPublication>("gpr") {
      artifactId = "jvm-runner"
      from(components["java"])
    }
  }
}

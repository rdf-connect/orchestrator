plugins {
  application
  id("com.github.johnrengelman.shadow") version "8.1.1"
  kotlin("jvm") version "1.9.22"
  id("maven-publish")
}

group = "technology.idlab"

version = "0.0.1"

application { mainClass.set("technology.idlab.MainKt") }

tasks.shadowJar {
  manifest.attributes.apply { put("Main-Class", "technology.idlab.MainKt") }
  mergeServiceFiles()
}

repositories { mavenCentral() }

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

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

  // RDF dependencies.
  implementation("org.apache.jena:apache-jena-libs:5.0.0")
  implementation("org.apache.jena:jena-arq:5.0.0")

  // Initialize testing.
  testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(17) }

publishing {
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/rdf-connect/jvm-runner")
      credentials {
        username = System.getenv("GITHUB_ACTOR")
        password = System.getenv("GITHUB_TOKEN")
      }
    }
  }
}

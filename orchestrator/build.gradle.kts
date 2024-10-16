plugins {
  application
  id("com.github.johnrengelman.shadow") version "8.1.1"
  kotlin("jvm") version "2.0.0"
  id("maven-publish")
  id("co.uzzu.dotenv.gradle") version "4.0.0"
  id("com.google.protobuf") version "0.9.4"
  id("org.jetbrains.dokka") version "1.9.20"
}

/** Orchestrator configuration. */
group = "technology.idlab"

version = "0.0.4"

/** Set the Kotlin JVM version to 22. */
kotlin { jvmToolchain(22) }

/**
 * Specify the entrypoint for the application. This is a simple CLI interface wrapper which
 * initializes the parsers and the orchestrator.
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
tasks.test {
  useJUnitPlatform()

  // Do not allow parallel forks.
  maxParallelForks = 1

  // Propagate output to the console.
  testLogging { showStandardStreams = true }
}

/** We define these explicitly due to the reliance on Protobuf and gRPC. */
sourceSets { main { proto { srcDir("../proto") } } }

/*
 * Check this document for more info on the section below:
 * https://github.com/grpc/grpc-kotlin/tree/master/compiler
 */
protobuf {
  protoc { artifact = "com.google.protobuf:protoc:4.27.1" }
  plugins {
    create("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:1.64.0" }
    create("grpckt") { artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar" }
  }
  generateProtoTasks {
    all().forEach {
      it.plugins {
        create("grpc")
        create("grpckt")
      }
      it.builtins { create("kotlin") }
    }
  }
}

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

  // TOML parser.
  implementation("org.tomlj:tomlj:1.1.1")

  // Arrow functional library.
  implementation("io.arrow-kt:arrow-core:1.2.4")
  implementation("io.arrow-kt:arrow-fx-coroutines:1.2.4")

  // Koin dependency injection.
  implementation(project.dependencies.platform("io.insert-koin:koin-bom:4.0.0"))
  implementation("io.insert-koin:koin-core")
  testImplementation("io.insert-koin:koin-test:4.0.0")
  testImplementation("io.insert-koin:koin-test-junit5:4.0.0")

  // gRPC
  implementation("io.grpc:grpc-netty:1.64.0")
  implementation("io.grpc:grpc-protobuf:1.64.0")
  implementation("io.grpc:grpc-stub:1.64.0")
  implementation("io.grpc:grpc-kotlin-stub:1.4.1")
  implementation("com.google.protobuf:protobuf-kotlin:4.27.1")

  // HTTP dependencies.
  implementation("io.ktor:ktor-client-core:2.3.10")
  implementation("io.ktor:ktor-client-cio:2.3.10")
  implementation("io.ktor:ktor-server-core:2.3.10")
  implementation("io.ktor:ktor-server-netty:2.3.10")
  testImplementation("io.ktor:ktor-client-mock:2.3.10")

  // RDF dependencies.
  implementation("org.apache.jena:apache-jena-libs:5.0.0")
  implementation("org.apache.jena:jena-arq:5.0.0")
  implementation("org.apache.jena:jena-shacl:5.0.0")

  // Hide SLF4J warnings.
  implementation("org.slf4j:slf4j-nop:2.0.7")

  // Initialize testing.
  testImplementation("org.jetbrains.kotlin:kotlin-test")
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

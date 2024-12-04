plugins { id("com.google.protobuf") }

/** The target JDK. */
val jdkVersion: String by project

kotlin { jvmToolchain(jdkVersion.toInt()) }

dependencies {
  // Kotlin extensions.
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
  implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")

  // Local dependencies
  implementation(project(":rdfc-core"))
  implementation(project(":rdfc-processor"))
  implementation(project(":rdfc-intermediate"))

  // gRPC
  implementation("io.grpc:grpc-netty:1.64.0")
  implementation("io.grpc:grpc-protobuf:1.64.0")
  implementation("io.grpc:grpc-stub:1.64.0")
  implementation("io.grpc:grpc-kotlin-stub:1.4.1")
  implementation("com.google.protobuf:protobuf-kotlin:4.27.1")

  // HTTP dependencies
  implementation("io.ktor:ktor-client-core:3.0.2")
  implementation("io.ktor:ktor-client-cio:2.3.10")
  implementation("io.ktor:ktor-server-core:2.3.10")
  implementation("io.ktor:ktor-server-netty:2.3.10")
  testImplementation("io.ktor:ktor-client-mock:2.3.10")

  // RDF dependencies
  implementation("org.apache.jena:apache-jena-libs:5.0.0")
  implementation("org.apache.jena:jena-arq:5.0.0")
  implementation("org.apache.jena:jena-shacl:5.0.0")

  // Hide SLF4J warnings
  implementation("org.slf4j:slf4j-nop:2.0.7")

  // Git resolver
  implementation("org.eclipse.jgit:org.eclipse.jgit:7.0.0.202409031743-r")

  // KTest
  testImplementation("org.jetbrains.kotlin:kotlin-test")
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

publishing {
  publications {
    create<MavenPublication>("gpr") {
      from(components["java"])
      groupId = "technology.idlab"
      artifactId = "rdfc-orchestrator"
      version = project.ext["projectVersion"] as String
    }
  }
}

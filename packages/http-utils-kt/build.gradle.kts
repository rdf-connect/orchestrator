plugins { kotlin("jvm") version "2.0.21" }

group = "technology.idlab"

version = "1.0-SNAPSHOT"

kotlin { jvmToolchain(22) }

repositories { mavenCentral() }

dependencies {
  // RDFC SDK
  implementation(files("rdfc-processor.jar"))

  // Kotlin extensions.
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

  // HTTP dependencies
  implementation("io.ktor:ktor-client-core:2.3.10")
  implementation("io.ktor:ktor-client-cio:2.3.10")
  implementation("io.ktor:ktor-server-core:2.3.10")
  implementation("io.ktor:ktor-server-netty:2.3.10")
  testImplementation("io.ktor:ktor-client-mock:2.3.10")

  // RDF dependencies.
  implementation("org.apache.jena:apache-jena-libs:5.0.0")
  implementation("org.apache.jena:jena-arq:5.0.0")
  implementation("org.apache.jena:jena-shacl:5.0.0")

  // Initialize testing.
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

tasks.test {
  useJUnitPlatform()

  testLogging { showStandardStreams = true }
}

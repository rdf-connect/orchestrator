plugins { kotlin("jvm") version "2.0.21" }

group = "technology.idlab"

version = "1.0-SNAPSHOT"

kotlin { jvmToolchain(22) }

repositories { mavenCentral() }

dependencies {
  // Kotlin extensions
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

  // RDFC SDK
  implementation(files("rdfc-core.jar"))
  implementation(files("rdfc-processor.jar"))

  // RDF dependencies.
  implementation("org.apache.jena:apache-jena-libs:5.0.0")
  implementation("org.apache.jena:jena-arq:5.0.0")
  implementation("org.apache.jena:jena-shacl:5.0.0")

  // KTest
  testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
  useJUnitPlatform()

  testLogging { showStandardStreams = true }
}

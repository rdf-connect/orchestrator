plugins { kotlin("jvm") version "2.0.21" }

group = "technology.idlab"

version = "0.0.4"

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

// Configure testing.
tasks.test { useJUnitPlatform() }

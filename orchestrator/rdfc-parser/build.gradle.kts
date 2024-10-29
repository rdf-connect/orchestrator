plugins { kotlin("jvm") version "2.0.21" }

group = "technology.idlab"

version = "0.0.1"

kotlin { jvmToolchain(22) }

repositories { mavenCentral() }

dependencies {
  // Local dependencies
  implementation(project(":rdfc-core"))

  // RDF dependencies
  implementation("org.apache.jena:apache-jena-libs:5.0.0")
  implementation("org.apache.jena:jena-arq:5.0.0")
  implementation("org.apache.jena:jena-shacl:5.0.0")

  // KTest
  testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

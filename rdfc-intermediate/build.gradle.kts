plugins { kotlin("jvm") version "2.0.21" }

group = "technology.idlab"

version = "0.0.2"

kotlin { jvmToolchain(22) }

repositories { mavenCentral() }

dependencies {
  // Local dependencies
  implementation(project(":rdfc-core"))

  // URL encoding
  implementation("io.ktor:ktor-client-core:2.3.10")

  // KTest
  testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

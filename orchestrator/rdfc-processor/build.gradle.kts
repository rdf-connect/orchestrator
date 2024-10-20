plugins { kotlin("jvm") version "2.0.21" }

group = "technology.idlab"

version = "0.0.4"

kotlin { jvmToolchain(22) }

repositories { mavenCentral() }

dependencies {
  // Local dependencies
  implementation(project(":rdfc-core"))

  // Kotlin extensions
  implementation(kotlin("reflect"))

  // KTest
  testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

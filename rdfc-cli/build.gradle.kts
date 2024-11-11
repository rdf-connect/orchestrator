plugins {
  application
  kotlin("jvm") version "2.0.21"
  id("com.gradleup.shadow") version "8.3.3"
}

group = "technology.idlab"

version = "0.0.2"

/** Specify the entrypoint for the application. */
application { mainClass.set("technology.idlab.rdfc.cli.MainKt") }

kotlin { jvmToolchain(22) }

repositories { mavenCentral() }

dependencies {
  // Local dependencies
  implementation(project(":rdfc-core"))
  implementation(project(":rdfc-orchestrator"))
  implementation(project(":rdfc-parser"))
  implementation(project(":rdfc-intermediate"))

  // Kotlin extensions.
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

  // KTest.
  testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
  // Name of the resulting archive.
  archiveFileName.set("rdfc.jar")

  // Make sure to include dependencies from other modules.
  configurations = listOf(project.configurations.runtimeClasspath.get())

  // Specify merge strategies if needed to handle duplicate files.
  mergeServiceFiles()
}
plugins {
  application
  id("com.gradleup.shadow")
}

/** Specify the entrypoint for the application. */
application { mainClass.set("technology.idlab.rdfc.cli.MainKt") }

/** The target JDK. */
val jdkVersion: String by project
kotlin { jvmToolchain(jdkVersion.toInt()) }

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

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
  // Name of the resulting archive.
  archiveFileName.set("rdfc.jar")

  // Make sure to include dependencies from other modules.
  configurations = listOf(project.configurations.runtimeClasspath.get())

  // Specify merge strategies if needed to handle duplicate files.
  mergeServiceFiles()
}

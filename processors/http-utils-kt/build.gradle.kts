plugins {
  `java-library`
  kotlin("jvm") version "2.0.0"
}

group = "technology.idlab.http-utils-kt"

version = "1.0-SNAPSHOT"

/** Set the Kotlin JVM version to 17. */
kotlin { jvmToolchain(17) }

repositories { mavenCentral() }

dependencies {
  implementation("org.reflections:reflections:0.10.2")

  // Processor class and other utilities.
  implementation(files("../../build/libs/technology.idlab.jvm-runner-0.0.4-all.jar"))

  // RDF dependencies.
  implementation("org.apache.jena:apache-jena-libs:5.0.0")
  implementation("org.apache.jena:jena-arq:5.0.0")
  implementation("org.apache.jena:jena-shacl:5.0.0")

  // Initialize testing.
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
  testImplementation("org.jetbrains.kotlin:kotlin-test")
}

/** Use JUnit for testing. */
tasks.test {
  useJUnitPlatform()

  testLogging { showStandardStreams = true }
}

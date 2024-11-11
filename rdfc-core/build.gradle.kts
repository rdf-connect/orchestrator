/** The target JDK. */
val jdkVersion: String by project

kotlin { jvmToolchain(jdkVersion.toInt()) }

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

publishing {
  publications {
    create<MavenPublication>("gpr") {
      from(components["java"])
      groupId = "technology.idlab"
      artifactId = "rdfc-core"
      version = project.ext["projectVersion"] as String
    }
  }
}

/** The target JDK. */
val jdkVersion: String by project

kotlin { jvmToolchain(jdkVersion.toInt()) }

dependencies {
  // Local dependencies
  implementation(project(":rdfc-core"))
  implementation(project(":rdfc-intermediate"))

  // RDF dependencies
  implementation("org.apache.jena:apache-jena-libs:5.0.0")
  implementation("org.apache.jena:jena-arq:5.0.0")
  implementation("org.apache.jena:jena-shacl:5.0.0")

  // URL encoding
  implementation("io.ktor:ktor-client-core:3.0.1")

  // KTest
  testImplementation(kotlin("test"))
}

publishing {
  publications {
    create<MavenPublication>("gpr") {
      from(components["java"])
      groupId = "technology.idlab"
      artifactId = "rdfc-parser"
      version = project.ext["projectVersion"] as String
    }
  }
}

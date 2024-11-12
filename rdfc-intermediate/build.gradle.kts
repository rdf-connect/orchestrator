/** The target JDK. */
val jdkVersion: String by project

kotlin { jvmToolchain(jdkVersion.toInt()) }

dependencies {
  // Local dependencies
  implementation(project(":rdfc-core"))

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
      artifactId = "rdfc-intermediate"
      version = project.ext["projectVersion"] as String
    }
  }
}

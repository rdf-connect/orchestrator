/** The target JDK. */
val jdkVersion: String by project

kotlin { jvmToolchain(jdkVersion.toInt()) }

dependencies {
  // Local dependencies
  implementation(project(":rdfc-core"))

  // Kotlin extensions
  implementation(kotlin("reflect"))

  // KTest
  testImplementation(kotlin("test"))
}

publishing {
  publications {
    create<MavenPublication>("gpr") {
      from(components["java"])
      groupId = "technology.idlab"
      artifactId = "rdfc-processor"
      version = project.ext["projectVersion"] as String
    }
  }
}

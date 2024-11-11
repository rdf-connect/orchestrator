/** The target JDK. */
val jdkVersion: String by project
kotlin { jvmToolchain(jdkVersion.toInt()) }

dependencies {
  // Local dependencies
  implementation(project(":rdfc-core"))

  // URL encoding
  implementation("io.ktor:ktor-client-core:2.3.10")

  // KTest
  testImplementation(kotlin("test"))
}

rootProject.name = "technology.idlab.rdfc"

include("rdfc-core")

include("rdfc-processor")

include("rdfc-orchestrator")

include("rdfc-cli")

include("rdfc-parser")

include("rdfc-intermediate")

pluginManagement {
  val kotlinVersion: String by settings
  val dotEnvVersion: String by settings
  val dokkaVersion: String by settings
  val shadowVersion: String by settings
  val protobufVersion: String by settings

  plugins {
    kotlin("jvm") version kotlinVersion
    id("co.uzzu.dotenv.gradle") version dotEnvVersion
    id("org.jetbrains.dokka") version dokkaVersion
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
    id("com.gradleup.shadow") version shadowVersion
    id("com.google.protobuf") version protobufVersion
  }
}

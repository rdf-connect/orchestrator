plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0" }

rootProject.name = "technology.idlab.jvm-runner"

include("rdfc-core")

include("rdfc-processor")

include("rdfc-orchestrator")

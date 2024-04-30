plugins {
    kotlin("jvm") version "1.9.22"
}

group = "technology.idlab"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // HTTP dependencies.
    implementation("io.ktor:ktor-client-core:2.3.10")
    implementation("io.ktor:ktor-client-cio:2.3.10")
    implementation("io.ktor:ktor-server-core:2.3.10")
    implementation("io.ktor:ktor-server-netty:2.3.10")

    // RDF dependencies.
    implementation("org.apache.jena:apache-jena-libs:5.0.0")
    implementation("org.apache.jena:jena-arq:5.0.0")

    // Add reactive support.
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")

    // Initialize testing.
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

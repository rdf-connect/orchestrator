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

    // RDF dependencies.
    implementation("org.apache.jena:apache-jena-libs:5.0.0")
    implementation("org.apache.jena:jena-arq:5.0.0")

    // Initialize testing.
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

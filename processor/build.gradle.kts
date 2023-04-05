plugins {
    id("java")
    kotlin("jvm")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    // KSP and Kotlin Poet
    implementation("com.google.devtools.ksp:symbol-processing-api:1.8.10-1.0.9")
    implementation("com.squareup:kotlinpoet:1.10.1")
    implementation("com.squareup:kotlinpoet-ksp:1.10.1")
    implementation(project(":annotations"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
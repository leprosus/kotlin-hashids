import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm").version("1.3.41")
}

version = "1.0.4"
group = "org.hashids"
description = "Kotlin implementation of Hashids https://hashids.org"

repositories {
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.5.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.1")
    runtime("org.junit.jupiter:junit-jupiter-engine:5.5.1")
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Jar> {
    manifest {
        attributes(mapOf(
                "Implementation-Version" to project.version,
                "Implementation-Title" to project.description
        ))
    }
}

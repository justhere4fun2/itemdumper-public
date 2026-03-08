plugins {
    kotlin("jvm") version "1.9.23"
    application
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.displee:rs-cache-library:8.0.1")
}

application {
    mainClass.set("MainKt")
}

tasks.register<JavaExec>("item-dump") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("MainKt")
    if (project.hasProperty("args")) {
        args = project.property("args").toString().split(" ")
    }
}
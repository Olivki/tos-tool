plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("com.github.johnrengelman.shadow") version "8.1.0"
    application
}

repositories {
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation(project(":tos-common"))
    implementation(project(":tos-ies"))
    implementation(project(":tos-ipf"))
    implementation(project(":tos-xac"))

    implementation(libs.kotlinx.serialization.json)
    implementation("com.ensarsarajcic.kotlinx:serialization-msgpack:0.5.4")

    implementation("net.ormr.krautils:krautils-core:0.2.0")

    implementation(libs.clikt)

    implementation(libs.jdom2)
    implementation(libs.jaxen)

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("net.ormr.tos.cli.MainKt")
}

distributions {
    this.shadow {
        distributionBaseName.set("tos")
    }
}

tasks {
    shadowJar {
        archiveFileName.set("tos.jar")
    }

    shadowDistZip {
        archiveFileName.set("tos.zip")
    }

    test {
        useJUnitPlatform()
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    processResources {
        expand("projectVersion" to project.version)
    }
}
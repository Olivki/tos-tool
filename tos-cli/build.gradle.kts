plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
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

    implementation("org.apache.commons:commons-csv:1.10.0")

    implementation(libs.kotlinx.serialization.json)
    implementation("com.ensarsarajcic.kotlinx:serialization-msgpack:0.5.4")

    implementation("net.ormr.krautils:krautils-core:0.2.0")
    implementation("net.ormr.krautils:krautils-compress:0.0.1")

    implementation(libs.clikt)

    implementation(libs.jdom2)
    implementation(libs.jaxen)

    implementation(libs.kxml2)

    implementation(libs.bundles.slf4j)

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("net.ormr.tos.cli.MainKt")
}

tasks {
    test {
        useJUnitPlatform()
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}
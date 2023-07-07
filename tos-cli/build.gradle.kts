plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":tos-ies"))
    implementation(project(":tos-ipf"))
    implementation(project(":tos-xac"))

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
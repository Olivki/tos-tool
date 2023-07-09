plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "net.ormr.tos.ipf"
version = "0.0.1"

dependencies {
    implementation(project(":tos-common"))
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
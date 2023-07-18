plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "net.ormr.tos"
version = "0.0.1"

dependencies {
    implementation(libs.guava)
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
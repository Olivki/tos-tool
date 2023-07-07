plugins {
    alias(libs.plugins.kotlin.jvm)
}

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
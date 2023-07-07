plugins {
    alias(libs.plugins.kotlin.jvm)
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
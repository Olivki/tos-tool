plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("org.javamodularity.moduleplugin") version "1.8.12"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.runtime") version "1.12.7"
    application
}

group = "net.ormr.ieseditor"
version = "0.0.1"

dependencies {
    implementation(project(":tos-ies"))
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.javafx)

    implementation("net.ormr.krautils:krautils-core:0.2.0")

    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-feather-pack:12.3.1")

    implementation("no.tornado:tornadofx:1.7.20")
    implementation("io.github.mkpaz:atlantafx-base:2.0.1")
}

application {
    mainClass.set("net.ormr.iesviewer.MainKt")
}

javafx {
    version = "17.0.2"
    modules = mutableListOf("javafx.controls", "javafx.graphics")
}

runtime {
    options.addAll("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
    launcher {
        noConsole = true
    }
    jpackage {
        imageName = "ies-viewer"
        /*imageOptions = listOf(
            "--icon", projectDir.resolve("icon.ico").absolutePath,
        )*/
    }
}
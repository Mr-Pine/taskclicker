import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "de.mr-pine"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    /*maven {
        name = "sonatype-snapshots"
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        isAllowInsecureProtocol = false
    }*/
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation("me.bechberger:bpf:0.1.1-scx-enabled-SNAPSHOT")
    annotationProcessor("me.bechberger:bpf:0.1.1-scx-enabled-SNAPSHOT")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xplugin:BPFCompilerPlugin")
    options.compilerArgs.addAll(listOf("-processor","me.bechberger.ebpf.bpf.processor.Processor"))
}

compose.desktop {
    application {
        mainClass = "de.mr_pine.taskclicker.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "taskclicker"
            packageVersion = "1.0.0"
        }
    }
}

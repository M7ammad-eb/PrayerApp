plugins { kotlin("multiplatform") }
kotlin {
    jvm { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) } }
    if (System.getProperty("os.name") == "Mac OS X") {
        listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach {
            it.binaries.framework { baseName = "SalatiCore"; isStatic = true }
        }
    }
    sourceSets {
        commonMain.dependencies { implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1") }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}


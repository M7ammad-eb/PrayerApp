plugins { kotlin("multiplatform") }
kotlin {
    jvm { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) } }
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach {
        it.binaries.framework { baseName = "SalatiCore"; isStatic = true }
    }
    sourceSets {
        commonMain.dependencies { implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1") }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}


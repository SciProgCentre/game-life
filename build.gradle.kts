import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    kotlin("multiplatform") version "1.6.10"
    application
    kotlin("plugin.serialization") version "1.6.10"
}

val akkaVersion = "2.6.19"
val akkaScalaBinaryVersion = "2.13"
val serializationVersion = "1.3.2"
val ktorVersion = "1.6.7"
val reactVersion = "17.0.2-pre.299-kotlin-1.6.10"
val kmongoVersion = "4.5.1"
val coroutinesVersion = "1.6.1"

group = "space.kscience.simba"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("space.kscience.simba.ServerKt")
}

kotlin {
    jvm {
        withJava()
    }

    js {
        browser {
            binaries.executable()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("com.typesafe.akka:akka-actor-typed_$akkaScalaBinaryVersion:$akkaVersion")
                implementation("com.typesafe.akka:akka-slf4j_$akkaScalaBinaryVersion:$akkaVersion")

                implementation("org.slf4j:slf4j-api:1.7.36")
                implementation("org.slf4j:slf4j-simple:1.7.36")

                implementation("io.ktor:ktor-serialization:$ktorVersion")
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")

                implementation("org.litote.kmongo:kmongo-coroutine:$kmongoVersion")
                implementation("org.litote.kmongo:kmongo-coroutine-serialization:$kmongoVersion")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            }
        }

        val jsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation("io.ktor:ktor-client-json:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization:$ktorVersion")

                implementation("org.jetbrains.kotlin-wrappers:kotlin-react:$reactVersion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:$reactVersion")
            }
        }
    }
}

// include JS artifacts in any JAR we generate
tasks.getByName<Jar>("jvmJar") {
    val taskName = if (project.hasProperty("isProduction") || project.gradle.startParameter.taskNames.contains("installDist")) {
        "jsBrowserProductionWebpack"
    } else {
        "jsBrowserDevelopmentWebpack"
    }
    val webpackTask = tasks.getByName<KotlinWebpack>(taskName)
    dependsOn(webpackTask) // make sure JS gets compiled first
    from(File(webpackTask.destinationDirectory, webpackTask.outputFileName)) // bring output file along into the JAR
}

tasks.getByName<JavaExec>("run") {
    classpath(tasks.getByName<Jar>("jvmJar")) // so that the JS artifacts generated by `jvmJar` can be found and served
}

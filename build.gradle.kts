import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    kotlin("multiplatform") version "1.7.10"
    application
    kotlin("plugin.serialization") version "1.7.10"
}

val akkaVersion = "2.7.0"
val akkaScalaBinaryVersion = "2.13"
val akkaManagementVersion = "1.1.4"
val serializationVersion = "1.3.2"
val ktorVersion = "1.6.7"
val reactVersion = "17.0.2-pre.299-kotlin-1.6.10"
val coroutinesVersion = "1.6.1"
val jacksonVersion = "2.14.0"
val slickVersion = "3.4.1"

group = "space.kscience.simba"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
}

application {
    mainClass.set("space.kscience.simba.ServerKt")
}

kotlin {
    jvm {
        withJava()
    }

    js(IR) {
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
                implementation("com.typesafe.akka:akka-stream-typed_$akkaScalaBinaryVersion:$akkaVersion")
                implementation("com.typesafe.akka:akka-slf4j_$akkaScalaBinaryVersion:$akkaVersion")
                implementation("com.lightbend.akka.management:akka-management-cluster-bootstrap_$akkaScalaBinaryVersion:$akkaManagementVersion")
                implementation("com.typesafe.akka:akka-bom_$akkaScalaBinaryVersion:$akkaVersion")
                implementation("com.typesafe.akka:akka-cluster-sharding-typed_$akkaScalaBinaryVersion:$akkaVersion")
                implementation("com.typesafe.akka:akka-serialization-jackson_$akkaScalaBinaryVersion:$akkaVersion")
                implementation("com.typesafe.akka:akka-persistence-typed_$akkaScalaBinaryVersion:$akkaVersion")

                // Next 5 dependencies must present to be able to overwrite version
                implementation("com.typesafe.akka:akka-discovery_$akkaScalaBinaryVersion:$akkaVersion")
                implementation("com.typesafe.akka:akka-coordination_$akkaScalaBinaryVersion:$akkaVersion")
                implementation("com.typesafe.akka:akka-remote_$akkaScalaBinaryVersion:$akkaVersion")
                implementation("com.typesafe.akka:akka-cluster_$akkaScalaBinaryVersion:$akkaVersion")
                implementation("com.typesafe.akka:akka-pki_$akkaScalaBinaryVersion:$akkaVersion")
                // END

                // Next dependency must present because of error "ClusterReceptionist could not be loaded dynamically. Make sure you have 'akka-cluster-typed' in the classpath."
                implementation("com.typesafe.akka:akka-cluster-typed_$akkaScalaBinaryVersion:$akkaVersion")

                implementation("org.slf4j:slf4j-api:1.7.36")
                implementation("org.slf4j:slf4j-simple:1.7.36")
                implementation("ch.qos.logback:logback-classic:1.4.4") // needs JDK 11+

                implementation("io.ktor:ktor-serialization:$ktorVersion")
//                implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
//                implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")


                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

                implementation("com.lightbend.akka:akka-persistence-jdbc_$akkaScalaBinaryVersion:5.2.0")
                implementation("com.typesafe.akka:akka-persistence-query_$akkaScalaBinaryVersion:$akkaVersion")
                implementation("com.typesafe.slick:slick_$akkaScalaBinaryVersion:$slickVersion")
                implementation("com.typesafe.slick:slick-hikaricp_$akkaScalaBinaryVersion:$slickVersion")
                implementation("org.postgresql:postgresql:42.5.1")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.5")

                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation("io.ktor:ktor-client-json:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization:$ktorVersion")

                implementation("org.jetbrains.kotlin-wrappers:kotlin-react:$reactVersion")
                implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:$reactVersion")

                implementation("space.kscience:plotlykt-core-js:0.5.0")
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
    if (project.hasProperty("isProduction")) {
        // Must disable debug mode in coroutines because of performance issues
        systemProperty("kotlinx.coroutines.debug", "off")
    }
    // For some reason gradle doesn't propagate system properties (-D) to VM, so we must do it manually
    System.getProperty("config.resource")?.let { systemProperty("config.resource", it) }
    classpath(tasks.getByName<Jar>("jvmJar")) // so that the JS artifacts generated by `jvmJar` can be found and served
}

tasks.getByName<Test>("jvmTest") {
    System.getProperty("config.resource")?.let { systemProperty("config.resource", it) }
}

tasks.test {
    useJUnitPlatform()
}

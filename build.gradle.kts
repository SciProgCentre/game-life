plugins {
    kotlin("multiplatform") version "1.6.10"
    application
}

val akkaVersion = "2.6.19"
val akkaScalaBinaryVersion = "2.13"
group = "space.kscience.simba"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
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
                implementation(kotlin("stdlib"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("com.typesafe.akka:akka-actor-typed_$akkaScalaBinaryVersion:$akkaVersion")
                implementation("com.typesafe.akka:akka-slf4j_$akkaScalaBinaryVersion:$akkaVersion")

                implementation("org.slf4j:slf4j-api:1.7.36")
                implementation("org.slf4j:slf4j-simple:1.7.36")
            }
        }

        val jsMain by getting {
            dependencies {

            }
        }
    }
}

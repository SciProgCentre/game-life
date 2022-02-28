plugins {
    kotlin("multiplatform") version "1.6.10"
    application
}

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
                dependencies {
                    implementation(kotlin("stdlib"))
                }
            }
        }

        val jvmMain by getting {
            dependencies {

            }
        }

        val jsMain by getting {
            dependencies {

            }
        }
    }
}

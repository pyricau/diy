plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":diy-lib"))
    implementation(project(":diy-processor"))
    ksp(project(":diy-processor"))
}

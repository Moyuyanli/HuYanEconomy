plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    compileOnly(project(":economy-action"))
    compileOnly("com.google.devtools.ksp:symbol-processing-api:1.9.20-1.0.14")

    testImplementation(kotlin("test"))
}

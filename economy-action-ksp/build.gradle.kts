plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    compileOnly(project(":economy-action"))
    compileOnly("com.google.devtools.ksp:symbol-processing-api:1.9.20-1.0.14")

    testImplementation(project(":economy-action"))
    testImplementation(kotlin("test"))
    testImplementation("com.google.devtools.ksp:symbol-processing-api:1.9.20-1.0.14")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:1.5.0")
}

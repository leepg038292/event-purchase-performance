plugins {
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.6") }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":cart:model"))
    implementation(project(":cart:infrastructure"))
    implementation(project(":user:infrastructure"))      // UserValidationPort
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
}

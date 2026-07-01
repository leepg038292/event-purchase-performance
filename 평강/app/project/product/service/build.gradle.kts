plugins {
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.6") }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":product:model"))
    implementation(project(":product:exception"))
    implementation(project(":product:infrastructure"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
}

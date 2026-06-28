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
    implementation(project(":product:service"))
    implementation("org.springframework.boot:spring-boot-starter-web")
}

plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("io.spring.dependency-management")
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
}

dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.6") }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":cart:model"))
    implementation(project(":cart:infrastructure"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}

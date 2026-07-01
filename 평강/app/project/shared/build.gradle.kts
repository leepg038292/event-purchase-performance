plugins {
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.6") }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
}

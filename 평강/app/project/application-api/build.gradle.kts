plugins {
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":shared"))
    // product
    runtimeOnly(project(":product:model"))
    runtimeOnly(project(":product:infrastructure"))
    implementation(project(":product:api"))
    implementation(project(":product:repository-jpa"))
    implementation(project(":product:schema"))
    // cart
    runtimeOnly(project(":cart:model"))
    runtimeOnly(project(":cart:infrastructure"))
    implementation(project(":cart:api"))
    implementation(project(":cart:repository-jpa"))
    implementation(project(":cart:schema"))
    // user
    runtimeOnly(project(":user:model"))
    runtimeOnly(project(":user:infrastructure"))
    implementation(project(":user:repository-jpa"))
    implementation(project(":user:schema"))
    // spring boot
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
}

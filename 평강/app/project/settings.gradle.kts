pluginManagement {
    plugins {
        kotlin("jvm") version "2.2.21"
        kotlin("plugin.spring") version "2.2.21"
        kotlin("plugin.jpa") version "2.2.21"
        id("org.springframework.boot") version "4.0.6"
        id("io.spring.dependency-management") version "1.1.7"
    }
}

rootProject.name = "project"

include(
    "shared",
    "product:model",
    "product:infrastructure",
    "product:service",
    "product:repository-jpa",
    "product:api",
    "product:schema",
    "cart:model",
    "cart:infrastructure",
    "cart:service",
    "cart:repository-jpa",
    "cart:api",
    "cart:schema",
    "user:model",
    "user:infrastructure",
    "user:repository-jpa",
    "user:schema",
    "application-api"
)


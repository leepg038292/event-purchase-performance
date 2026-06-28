import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") apply false
    kotlin("plugin.spring") apply false
    kotlin("plugin.jpa") apply false
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
}

allprojects {
    // 중첩 모듈은 도메인별 group으로 구분해 GAV 충돌 방지
    // ex) cart:infrastructure → com.eventpurchase.cart:infrastructure
    val domain = project.path.removePrefix(":").split(":").firstOrNull()
    group = if (project.path.contains(":")) "com.eventpurchase.$domain" else "com.eventpurchase"
    version = "0.0.1-SNAPSHOT"
    repositories { mavenCentral() }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    // 같은 이름을 가진 모듈(model, infrastructure 등)이 여러 도메인에 존재할 때
    // Gradle이 GAV(group:name:version)를 동일하게 보고 하나로 치환하는 문제 방지.
    // 프로젝트 경로 기반으로 아티팩트 이름을 고유하게 설정한다.
    configure<BasePluginExtension> {
        archivesName.set(project.path.removePrefix(":").replace(":", "-"))
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<JavaCompile> {
        options.release.set(21)
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

}

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("org.springframework.boot") version "4.1.1"
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.allopen") version "2.3.21"
}

group = "com.br.semesperanca"
version = "0.0.1-SNAPSHOT"
description = "backend-leads"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// NUNCA usar kotlin("plugin.jpa") ou kotlin("plugin.spring"): ambos habilitam
// tambem o compilador noarg, que mudaria a estrategia de persistencia do
// Hibernate (Activity.lead nao tem valor default e depende do construtor
// canonico do Hibernate). O preset "spring" tambem nao cobre @Service,
// @Repository, @RestController, @ControllerAdvice, @Configuration e
// @ConfigurationProperties — por isso as anotacoes sao declaradas aqui.
allOpen {
    annotation("org.springframework.stereotype.Component")
    annotation("org.springframework.stereotype.Service")
    annotation("org.springframework.stereotype.Repository")
    annotation("org.springframework.web.bind.annotation.RestController")
    annotation("org.springframework.web.bind.annotation.ControllerAdvice")
    annotation("org.springframework.context.annotation.Configuration")
    annotation("org.springframework.boot.context.properties.ConfigurationProperties")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            // Spring Framework 7 e anotado com JSpecify e o padrao do Kotlin e
            // reportar divergencias de nullability como ERRO.
            "-Xjspecify-annotations=warn",
        )
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // BOM nativo do Gradle em vez de io.spring.dependency-management, cuja
    // 1.1.7 (dez/2024) e anterior ao Gradle 9.
    listOf(
        "implementation",
        "testImplementation",
        "runtimeOnly",
        "developmentOnly",
    ).forEach { configuration ->
        add(configuration, platform(SpringBootPlugin.BOM_COORDINATES))
    }

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    // No Boot 4 a auto-config de RestClient.Builder saiu do starter web
    // (modularizacao): sem este starter o InsightClient nao injeta.
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    // flyway-database-postgresql e "optional" no starter, precisa ser explicito
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")

    implementation("io.jsonwebtoken:jjwt-api:0.12.7")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.7")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.7")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// O plugin do Spring Boot nao configura a task Test: sem esta linha
// ./gradlew test passa com 0 testes.
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

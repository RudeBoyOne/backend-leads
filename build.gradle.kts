import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("org.springframework.boot") version "4.1.1"
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.allopen") version "2.3.21"
    jacoco
}

val jacocoToolVersion = "0.8.14"

group = "com.br.semesperanca"
version = "0.0.1-SNAPSHOT"
description = "backend-leads"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
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
            "-Xjspecify-annotations=warn",
        )
    }
}

repositories {
    mavenCentral()
}

dependencies {
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
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("com.hivemq:hivemq-mqtt-client:1.4.0")
    implementation(platform("com.hivemq:hivemq-mqtt-client-websocket:1.4.0"))

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
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-restclient-test")
    testImplementation("org.springframework.boot:spring-boot-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

extensions.configure<JacocoPluginExtension> {
    toolVersion = jacocoToolVersion
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required = true
        xml.required = true
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
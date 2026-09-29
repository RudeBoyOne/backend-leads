import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("org.springframework.boot") version "4.1.1"
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.allopen") version "2.3.21"
    jacoco
}

// 0.8.14 e a primeira release com suporte oficial a classe files do Java 25
// (class file major version 69) — o projeto compila para JVM 25.
val jacocoToolVersion = "0.8.14"

group = "com.br.semesperanca"
version = "0.0.1-SNAPSHOT"
description = "backend-leads"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// all-open PURO: abre classe e membros anotados, sem gerar construtor vazio.
// NUNCA usar kotlin("plugin.jpa") aqui — ele tambem habilita o compilador noarg,
// que mudaria a estrategia de persistencia do Hibernate: as entities ficariam com
// construtor no-arg e o Hibernate 7 passaria a instanciar por ele, em vez do
// construtor canonico (Activity.lead nao tem valor default).
//
// As stereotypes do Spring sao declaradas uma a uma em vez do preset
// kotlin("plugin.spring") para manter o build independente do plugin do Spring
// Boot; a lista e equivalente ao preset, acrescida de @ConfigurationProperties.
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
    // No Boot 4 a infraestrutura de teste tambem foi modularizada:
    // spring-boot-starter-test NAO traz mais @WebMvcTest/@DataJpaTest.
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    // @RestClientTest + MockRestServiceServer (substitui WireMock no InsightClient)
    testImplementation("org.springframework.boot:spring-boot-restclient-test")
    // Infraestrutura de seguranca nos slices @WebMvcTest
    testImplementation("org.springframework.boot:spring-boot-security-test")
    // @ServiceConnection, para o container alimentar o DataSource sozinho
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    // Testcontainers 2.x: artefatos com prefixo "testcontainers-" e pacotes proprios
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    // DSL Kotlin para Mockito: any(), eq(), verify {} com ArgumentCaptor tipado,
    // sem as raw do Java (any(Class), eq(Nullable) sao bags de nullability).
    // O Mockito 5 ja usa mock maker inline, entao classes final do Kotlin sao mockaveis.
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}

// O plugin do Spring Boot nao configura a task Test: sem esta linha
// ./gradlew test passa com 0 testes.
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // O JaCoCo instrumenta via java agent; no JDK 21+ isso exige a flag explicita
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

// Meta enforcada: 90% de linha cobertas. Build falha abaixo disso.
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

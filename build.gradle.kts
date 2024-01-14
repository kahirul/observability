import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

group = "co.paikama"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    runtimeOnly("io.micrometer:micrometer-registry-otlp:1.12.2")
    implementation("io.micrometer:micrometer-tracing-bridge-otel:1.2.2")
    implementation("io.opentelemetry:opentelemetry-api:1.34.1")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.34.1") {
        exclude("io.opentelemetry", module = "opentelemetry-exporter-sender-okhttp")
    }
    implementation("io.opentelemetry:opentelemetry-exporter-sender-jdk:1.34.1-alpha")
    implementation("io.opentelemetry:opentelemetry-exporter-common:1.34.1")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.0.0-alpha")
    implementation("io.opentelemetry:opentelemetry-sdk:1.34.1")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-semconv:1.32.0-alpha")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp-common:1.34.1")
    implementation("io.opentelemetry:opentelemetry-exporter-sender-grpc-managed-channel:1.34.1")
    implementation("io.grpc:grpc-netty:1.61.0")
    implementation("io.opentelemetry:opentelemetry-sdk-logs:1.34.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks {
    jar.configure {
        enabled = false
    }

    build {
        dependsOn("ktlintFormat")
    }

    withType<Test> {
        useJUnitPlatform()
    }
}

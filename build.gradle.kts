import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.1.5"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    kotlin("plugin.noarg") version "1.8.22"
}

group = "com.teamsparta"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")
    implementation("org.springframework.cloud:spring-cloud-starter-aws:2.0.1.RELEASE")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.postgresql:postgresql:42.7.3")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.google.code.gson:gson:2.8.8")
    implementation("org.jsoup:jsoup:1.19.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.json:json:20210307")

    implementation("org.seleniumhq.selenium:selenium-java:4.18.1")
    implementation("io.github.bonigarcia:webdrivermanager:5.6.3")
    implementation("com.aventrix.jnanoid:jnanoid:2.0.0")

    // ✅ 테스트 전용 h2만 명시적으로 추가
    testImplementation("com.h2database:h2")

    // ✅ starter-test에서 h2 제외
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "com.h2database", module = "h2")
    }

    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.3")
}

noArg {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

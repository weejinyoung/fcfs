plugins {
	id("org.springframework.boot") version "3.3.2"
	id("io.spring.dependency-management") version "1.1.6"
	kotlin("jvm") version "1.9.24"
	kotlin("plugin.spring") version "1.9.24"
}

group = "taskforce"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

val redissonVersion = "3.33.0"
val kotlinLoggingVersion = "7.0.0"
val kotestRunnerVersion = "5.9.0"
val kotestExtensionsVersion = "1.3.0"
val mockkVersion = "4.0.2"

dependencies {

	// Kotlin
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// Web
	implementation("org.springframework.boot:spring-boot-starter-web")

	// Inmemory DB Access
	implementation("org.redisson:redisson-spring-boot-starter:${redissonVersion}")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")

	// Logging
	implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")

	// Metrics monitoring
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.micrometer:micrometer-registry-prometheus")

	// Dev Tools
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("io.kotest:kotest-runner-junit5:$kotestRunnerVersion")
	testImplementation("io.kotest.extensions:kotest-extensions-spring:$kotestExtensionsVersion")
	testImplementation("com.ninja-squad:springmockk:$mockkVersion")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

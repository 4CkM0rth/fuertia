plugins {
	java
	id("org.springframework.boot") version "4.1.2"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "app.fuertia"
// Se publica en GET /api/v1/sistema/estado (vía build-info). Súbela con cada versión.
version = "0.2.0"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

// Versiones que no maneja el BOM de Spring Boot.
val versionArchUnit = "1.5.1"
val versionSpringdoc = "3.2.0" // la línea 3.x de springdoc es la compatible con Spring Boot 4
// Argon2 de Spring Security usa la implementación de Bouncy Castle.
val versionBouncyCastle = "1.86"
// Servidor SMTP en memoria para las pruebas de integración del correo.
val versionGreenMail = "2.1.15"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	// JWT: se usan las librerías y no el starter, porque las llaves, el decodificador y las
	// validaciones se arman a mano (IdentidadConfig) y no queremos la autoconfiguración por propiedades.
	implementation("org.springframework.security:spring-security-oauth2-resource-server")
	implementation("org.springframework.security:spring-security-oauth2-jose")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$versionSpringdoc")
	implementation("org.bouncycastle:bcprov-jdk18on:$versionBouncyCastle")
	runtimeOnly("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testImplementation("com.tngtech.archunit:archunit-junit5:$versionArchUnit")
	testImplementation("com.icegreen:greenmail:$versionGreenMail")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Genera META-INF/build-info.properties con la versión, para que el backend sepa cuál es la suya.
springBoot {
	buildInfo()
}

tasks.withType<JavaCompile> {
	// Los comentarios y mensajes van en español con tildes: forzamos UTF-8 en cualquier sistema operativo.
	options.encoding = "UTF-8"
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// ./gradlew test corre TODAS las pruebas (unitarias, ArchUnit e integración).
// ./gradlew pruebasRapidas corre solo las que no necesitan Docker: excluye el tag "integracion".
tasks.register<Test>("pruebasRapidas") {
	description = "Corre las pruebas unitarias y de ArquitecturaTest, sin las de integración (no necesita Docker)."
	group = "verification"
	testClassesDirs = sourceSets.test.get().output.classesDirs
	classpath = sourceSets.test.get().runtimeClasspath
	useJUnitPlatform {
		excludeTags("integracion")
	}
}

// Solo las de integración (Testcontainers). Necesita Docker corriendo.
tasks.register<Test>("pruebasDeIntegracion") {
	description = "Corre solo las pruebas de integración con Testcontainers (necesita Docker)."
	group = "verification"
	testClassesDirs = sourceSets.test.get().output.classesDirs
	classpath = sourceSets.test.get().runtimeClasspath
	useJUnitPlatform {
		includeTags("integracion")
	}
}

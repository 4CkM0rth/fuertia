// El equipo tiene Java 17 instalado, pero el proyecto compila con Java 21.
// Este plugin deja que Gradle descargue el JDK 21 automáticamente (desde foojay.io)
// si no lo encuentra en la máquina, así nadie tiene que instalarlo a mano.
plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "fuertia-backend"

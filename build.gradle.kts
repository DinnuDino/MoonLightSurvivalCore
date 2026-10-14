plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.7.1" apply false
    id("com.gradleup.shadow") version "8.3.0"
}

group = "com.moonlight.vanilla"
version = "1.0.0-SNAPSHOT"
description = "MoonlightVanillaSurvivalCore - Clean-Room Paper 26.2 Integration Plugin"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    // GeyserMC & Floodgate API repository for Bedrock crossplay
    maven("https://repo.opencollab.dev/main/")
    // ViaVersion API repository for 1.18.1 - 26.2 multi-version pipeline
    maven("https://repo.viaversion.com/")
}

dependencies {
    // Target Paper 26.2 / 1.21.4+ Core API
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")

    // HikariCP High-Performance Connection Pool (Shaded)
    implementation("com.zaxxer:HikariCP:5.1.0")

    // SQLite JDBC Driver (Bundled fallback)
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")

    // Floodgate 2.0 API (Bedrock crossplay detection & form GUIs without Java account)
    compileOnly("org.geysermc.floodgate:api:2.2.2-SNAPSHOT")
    compileOnly("org.geysermc.cumulus:cumulus:1.1.2")

    // ViaVersion & ViaBackwards Shaded Dependencies (Embedded Multi-Version Compatibility)
    implementation("com.viaversion:viaversion:5.0.0")
    implementation("com.viaversion:viabackwards:5.0.0")

    // Kyori Adventure MiniMessage (Included with Paper, but specified for dev tooling)
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")

    // Annotations for null safety
    compileOnly("org.jetbrains:annotations:24.1.0")

    // JUnit 5 Test Framework
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveClassifier.set("")
        // Relocate HikariCP to avoid runtime classpath conflicts
        relocate("com.zaxxer.hikari", "com.moonlight.vanilla.libs.hikari")
        relocate("org.sqlite", "com.moonlight.vanilla.libs.sqlite")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}

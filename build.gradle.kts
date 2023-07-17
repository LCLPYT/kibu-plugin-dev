import java.util.*

plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    id("gradle-build-utils").version("1.5.3")
}

repositories {
    mavenCentral()
    gradlePluginPortal()

    maven {
        url = uri("https://maven.fabricmc.net/")
    }

    maven {
        url = uri("https://repo.lclpnet.work/repository/internal")
    }
}

dependencies {
    implementation("com.github.johnrengelman.shadow:com.github.johnrengelman.shadow.gradle.plugin:${project.properties["shadow_version"]}")
    implementation("fabric-loom:fabric-loom.gradle.plugin:${project.properties["loom_version"]}")
    implementation("work.lclpnet:plugins4j:${project.properties["plugins4j_version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

val mavenGroup: String by project
val mavenArchivesName: String by project
val props: Properties = buildUtils.loadProperties("publish.properties")

base {
    group = mavenGroup
    archivesName.set(mavenArchivesName)

    version = buildUtils.gitVersion()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withSourcesJar()
}

gradlePlugin {
    plugins {
        create("kibuPluginDev") {
            id = "kibu-plugin-dev"
            implementationClass = "work.lclpnet.kibupd.KibuGradlePlugin"
        }
    }
}

publishing {
    buildUtils.setupPublishRepository(repositories, props)
}
import java.util.*

plugins {
    id("java")
    id("java-gradle-plugin")
    id("maven-publish")
    id("gradle-build-utils").version("1.2.0")
}

repositories {
    mavenCentral()
    gradlePluginPortal()

    maven {
        url = uri("https://maven.fabricmc.net/")
    }
}

dependencies {
    implementation("com.github.johnrengelman.shadow:com.github.johnrengelman.shadow.gradle.plugin:${project.properties["shadow_version"]}")
    implementation("fabric-loom:fabric-loom.gradle.plugin:${project.properties["loom_version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

val loadProperties: groovy.lang.Closure<Properties> by extra
val gitVersion: groovy.lang.Closure<String> by extra
val mavenGroup: String by project
val mavenArchivesName: String by project
val props = loadProperties("publish.properties")

base {
    group = mavenGroup
    archivesName.set(mavenArchivesName)
    version = gitVersion.call()
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
    repositories {
        maven {
            val env = System.getenv()
            if (listOf("DEPLOY_URL", "DEPLOY_USER", "DEPLOY_PASSWORD").all(env::containsKey)) {
                credentials {
                    username = env["DEPLOY_USER"]
                    password = env["DEPLOY_PASSWORD"]
                }
                url = uri(env["DEPLOY_URL"]!!)
            }
            else if (listOf("mavenHost", "mavenUser", "mavenPassword").all(props::containsKey)) {
                credentials {
                    username = props.getProperty("mavenUser")
                    password = props.getProperty("mavenPassword")
                }
                url = uri(props.getProperty("mavenHost")!!)
            } else {
                url = uri("file:///${project.projectDir}/repo")
            }
        }
    }
}
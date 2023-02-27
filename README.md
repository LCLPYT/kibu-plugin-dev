# kibu-plugin-dev
Gradle plugin for easier development of kibu plugins for Minecraft.

## Installation
In your `settings.gradle`, add the following repository at the very top:
```groovy
pluginManagement {
    repositories {
        maven {
            name = 'LCLPNetwork Maven'
            url = 'https://repo.lclpnet.work/repository/internal'
        }
        gradlePluginPortal()
    }
}
```

Then, you can apply the plugin in the plugins block in your `build.gradle`:
```groovy
plugins {
    id 'kibu-plugin-dev' version '0.1.0'
}
```

You can check the latest version [here](https://repo.lclpnet.work/#artifact/work.lclpnet.gradle/kibu-plugin-dev).

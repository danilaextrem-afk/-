pluginManagement {
    repositories {
        exclusiveContent {
            forRepository { mavenCentral() }
            filter { includeGroup("com.google.devtools.ksp") }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "RealtorAI"
include(":app")

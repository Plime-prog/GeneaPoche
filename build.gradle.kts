// KGP force avant le bloc plugins (recette AGP 9 + Kotlin 2.4.21 validee sur annuaire-ecoles)
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.4.21")
    }
}

plugins {
    id("com.android.application") version "9.2.0" apply false
}

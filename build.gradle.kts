buildscript {
  configurations.all {
    resolutionStrategy {
      force("org.jetbrains.kotlin:kotlin-reflect:2.3.20")
      force("org.jetbrains.kotlin:kotlin-stdlib:2.2.10")
    }
  }
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  // alias(libs.plugins.roborazzi) apply false
  // alias(libs.plugins.secrets) apply false
  // alias(libs.plugins.google.services) apply false
}

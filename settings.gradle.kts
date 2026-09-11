rootProject.name = "VeganApplication"

// Keep both applications independently buildable while exposing one IDE workspace.
includeBuild("android")
includeBuild("backend")

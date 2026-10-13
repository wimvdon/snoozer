package io.github.impca201.snoozer.data

enum class ThemeMode { System, Light, Dark }

data class Appearance(
    val themeMode: ThemeMode = ThemeMode.System,
    val dynamicColor: Boolean = false,
)

# По умолчанию минификация выключена (isMinifyEnabled = false),
# правила ниже — на случай включения R8 в релизной сборке.
-keep class com.chinesegames.app.data.** { *; }
-dontwarn org.jetbrains.annotations.**

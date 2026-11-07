# Ktor's logging refers to the slf4j binding, the app has none and slf4j falls back to its no-op logger
-dontwarn org.slf4j.impl.StaticLoggerBinder

# File names and line numbers for the Crashlytics stack traces, the uploaded mapping file restores the names
-keepattributes SourceFile,LineNumberTable

# networkErrorType() sends the class name of an unexpected network error to the analytics
-keepnames class * extends java.lang.Throwable

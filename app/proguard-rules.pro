# R8/ProGuard rules for release builds.
#
# Firebase Messaging and AndroidX ship their own consumer rules, and AGP keeps
# manifest-declared components (MainActivity, FCMService) automatically, so little
# is needed here. Keep line numbers for readable crash traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

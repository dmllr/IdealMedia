#default
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

#gms
-keep class * extends java.util.ListResourceBundle { protected Object[][] getContents(); }
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable { public static final *** NULL; }
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepnames class * implements android.os.Parcelable { public static final ** CREATOR; }
-keepclassmembernames class * { @com.google.android.gms.common.annotation.KeepName *; }

#app
-keep class com.un4seen.bass.** { *; }
-keep class com.armedarms.** { *; }
-keep class android.animation.** { *; }

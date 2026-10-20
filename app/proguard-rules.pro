# Release shrinking rules.
#
# The domain layer has no reflection, no serialization framework and no
# dependency on class names, so almost nothing needs keeping. What is listed
# here is limited to genuine entry points.

# Keep the Application subclass: it is named in the manifest.
-keep class com.tanesco.metercompanion.app.MeterCompanionApplication { *; }

# Keep enum valueOf/valueOf, used when reading persisted enum names.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Line numbers make crash reports from the field usable, while still allowing
# the method bodies themselves to be optimised.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

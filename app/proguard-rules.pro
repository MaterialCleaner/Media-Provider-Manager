-repackageclasses "me.gm.cleaner.plugin"
-allowaccessmodification
-overloadaggressively

-keep class me.gm.cleaner.plugin.xposed.XposedInit

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
	public static void check*(...);
	public static void throw*(...);
}

-assumenosideeffects class me.gm.cleaner.plugin.util.LogUtils {
	public static void e(...);
}

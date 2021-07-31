-repackageclasses "me.gm.cleaner.plugin"

-overloadaggressively

-keep class me.gm.cleaner.plugin.xposed.XposedInit

-keepclasseswithmembers class me.gm.cleaner.plugin.BinderReceiver {
    private static android.os.IBinder binder;
}

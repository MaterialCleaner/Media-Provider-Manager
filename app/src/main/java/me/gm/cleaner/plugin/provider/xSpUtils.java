package me.gm.cleaner.plugin.provider;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;

public class xSpUtils {
    public static String getSharedStringPrefs(Context context, String name) {

        Uri uri = Uri.parse("content://" + SharedPrefsProvider.AUTHORITY + "/string/" + name);
        try {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String prefValue = cursor.getString(0);
                cursor.close();
                return prefValue;
            } else {
                XposedBridge.log("[ContentResolver]===>" + "[" + name + "] Cursor fail: " + cursor);
            }
        } catch (Throwable t) {
            XposedBridge.log("[ContentResolver]===>" + t);
        }
        return null;
    }

    public static SharedPreferences getxSp(Context ctx) {
        return ctx.createDeviceProtectedStorageContext()
                .getSharedPreferences("xSp", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);

    }

    public static String backupSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = getxSp(context);
        Map<String, ?> allEntries = sharedPreferences.getAll();
        JSONObject json = new JSONObject();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            try {
                json.put(entry.getKey(), entry.getValue());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return json.toString(); // 可保存为文件
    }

    public static boolean restoreSharedPreferences(Context context, String jsonString) {
        SharedPreferences sharedPreferences = getxSp(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        try {
            JSONObject json = new JSONObject(jsonString);
            Iterator<String> keys = json.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                Object value = json.get(key);

                if (value instanceof Integer) {
                    editor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    editor.putLong(key, (Long) value);
                } else if (value instanceof Float) {
                    editor.putFloat(key, (Float) value);
                } else if (value instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) value);
                } else if (value instanceof String) {
                    editor.putString(key, (String) value);
                } else {
                    editor.putString(key, value.toString());
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        editor.commit();
        return true;
    }


}

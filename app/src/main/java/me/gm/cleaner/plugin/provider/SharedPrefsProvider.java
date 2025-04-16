package me.gm.cleaner.plugin.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SharedPrefsProvider extends ContentProvider {

    public static final String AUTHORITY = "me.gm.cleaner.plugin.provider.sharedprefs";
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    SharedPreferences prefs;

    static {
        uriMatcher.addURI(AUTHORITY, "string/*/", 0);
        uriMatcher.addURI(AUTHORITY, "string/*/*", 1);
        uriMatcher.addURI(AUTHORITY, "integer/*/*", 2);
        uriMatcher.addURI(AUTHORITY, "boolean/*/*", 3);
        uriMatcher.addURI(AUTHORITY, "stringset/*", 4);
        uriMatcher.addURI(AUTHORITY, "test/*", 5);
        uriMatcher.addURI(AUTHORITY, "shortcut_icon/*", 6);
    }

    @Override
    public boolean onCreate() {
        try {
            prefs = xSpUtils.getxSp(Objects.requireNonNull(getContext()));
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        List<String> parts = uri.getPathSegments();
        MatrixCursor cursor = new MatrixCursor(new String[]{"data"});
        Log.e("[EEEE]", "uri=" + uri + "parts0=" + parts.get(0) + "parts1=" + parts.get(1));
        int m = uriMatcher.match(uri);
        Log.e("[EEEE]", "uriMAtch=" + m);
        switch (m) {
            case 0 -> {
                Log.e("[EEEE]", "data=" + prefs.getString(parts.get(1), null));
                cursor.newRow().add("data", prefs.getString(parts.get(1), null));
                return cursor;
            }
            case 1 -> {
                Log.e("[EEEE]", "data=" + prefs.getString(parts.get(1), parts.get(2)));
                cursor.newRow().add("data", prefs.getString(parts.get(1), parts.get(2)));
                return cursor;
            }
            case 2 -> {
                cursor.newRow().add("data", prefs.getInt(parts.get(1), Integer.parseInt(parts.get(2))));
                return cursor;
            }
            case 3 -> {
                cursor.newRow().add("data", prefs.getBoolean(parts.get(1), Integer.parseInt(parts.get(2)) == 1) ? 1 : 0);
                return cursor;
            }
            case 4 -> {
                Set<String> strings = prefs.getStringSet(parts.get(1), new LinkedHashSet<>());
                for (String str : strings) cursor.newRow().add("data", str);
                return cursor;
            }
        }
        return null;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

}

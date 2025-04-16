package me.gm.cleaner.plugin.provider;

import android.content.ClipboardManager;
import android.content.Context;

public class ClipBoardUtils {

    public static String getClipBoardDataString(Context context) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        return clipboardManager.getText().toString();
    }

    public static boolean setClipBoardDataString(Context context, String data) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.setText(data);
        return true;

    }
}

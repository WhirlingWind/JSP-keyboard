package com.jsp.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;


import java.io.File;

/**
 * Created by blueberry on 2014-11-27.
 */
public class ExternalStorageHandler {
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
    public boolean checkRW(){
        if(isExternalStorageWritable()&isExternalStorageReadable()) return true;
        else return false;
    }
    public File setDir(Context context, String dirName) {
        // Get the directory for the user's public pictures directory.
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
}
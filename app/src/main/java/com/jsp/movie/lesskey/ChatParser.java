package com.jsp.movie.lesskey;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.jsp.util.DatabaseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ChatParser extends AsyncTask<String, Void, Void> {

    public DatabaseHandler dbHandler;

    public ChatParser (DatabaseHandler dbHandler) {

        this.dbHandler = dbHandler;
    }

    public interface OnPostListener {

        public void onPostExcute();
    }

    public OnPostListener l = null;

    @Override
    protected Void doInBackground(String... params) {

        ArrayList<String> arr = new ArrayList<String>();

        char[] b = params[0].toCharArray();

        for (int i = 0; i < b.length; i++) {

            if (!isKorean(b[i]))
                b[i] = '&';
        }

        String[] c = String.copyValueOf(b).split("&");

        for (String s : c)
            dbHandler.onNewWordGenerated(s);

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {

        if (l != null)
            l.onPostExcute();
    }

    @Override
    protected void onProgressUpdate(Void... values) {}

    private boolean isKorean (char c) {

        if (c >= 0x1100 && c <= 0x11FF) return true;
        if (c >= 0x3130 && c <= 0x318F) return true;
        if (c >= 0xA960 && c <= 0xA97F) return true;
        if (c >= 0xAC00 && c <= 0xD7AF) return true;
        if (c >= 0xD7B0 && c <= 0xD7FF) return true;

        return false;
    }

}

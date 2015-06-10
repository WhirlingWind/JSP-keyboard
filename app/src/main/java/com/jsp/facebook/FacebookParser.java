package com.jsp.facebook;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.jsp.util.DatabaseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class FacebookParser extends AsyncTask<JSONObject, Void, ArrayList<String>> {

    Context context;

    private DatabaseHandler dbHandler;

    public FacebookParser (Context cxt, DatabaseHandler dbHandler) {

        context = cxt;
        this.dbHandler = dbHandler;
    }

	public interface OnPostListener {

		public void onPostExecute();
	}

	public OnPostListener l = null;

	@Override
	protected ArrayList<String> doInBackground(JSONObject... params) {

        JSONObject jsonObject = params[0];
        JSONArray list;

        try {

            list = jsonObject.getJSONArray("data");
        }
        catch(JSONException e) { list = null; };

        String a = "";

        if(list != null) {

            for(int i=0;i<list.length();i++) {

                JSONObject elem;
                try {
                    elem = list.getJSONObject(i);
                } catch (JSONException e) {elem = null;};

                if (elem != null) {

                    JSONObject story_tags;

                    try {
                        story_tags = elem.getJSONObject("story_tags");
                    } catch (JSONException e) {story_tags = null;};

                    if (story_tags != null) {

                        try {
                            a += story_tags.getString("description");
                        } catch (JSONException e) {};
                    }

                    try {
                        a = a + elem.getString("message");
                    } catch (JSONException e) {};
                    a += '&';
                }
            }
        }

        char[] b = a.toCharArray();

        for (int i = 0; i < b.length; i++) {

            if (!isKorean (b[i]))
                b[i] = '&';
        }

        String[] c = String.copyValueOf(b).split("&");

        ArrayList<String> arr = new ArrayList<String> ();

        for (String s : c) {

            if (s.length() > 0)
                dbHandler.onNewWordGenerated(s);
        }

		return arr;
	}
	@Override
	protected void onPostExecute(ArrayList<String> result) {

		Toast.makeText(context, "FB Download Complete.", Toast.LENGTH_SHORT).show();

		if (l != null)
			l.onPostExecute();
	}
	@Override
	protected void onProgressUpdate(Void... values) {}

    private boolean isKorean (char c) {

        if (c >= 0x1100 && c <= 0x11FF) return true;
        //if (c >= 0x3130 && c <= 0x318F) return true;
        if (c >= 0xA960 && c <= 0xA97F) return true;
        if (c >= 0xAC00 && c <= 0xD7AF) return true;
        if (c >= 0xD7B0 && c <= 0xD7FF) return true;

        return false;
    }

}

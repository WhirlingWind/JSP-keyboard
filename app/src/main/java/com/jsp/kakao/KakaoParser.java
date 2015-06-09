package com.jsp.kakao;

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
import java.util.Collection;

public class KakaoParser extends AsyncTask<File, Void, ArrayList<String>> {

    Context context;

    private DatabaseHandler dbHandler;

    public KakaoParser(Context cxt, DatabaseHandler dbHandler) {

        context = cxt;
        this.dbHandler = dbHandler;
    }

	public interface OnPostListener {

		public void onPostExcute();
	}

	public OnPostListener l = null;

	@Override
	protected ArrayList<String> doInBackground(File... params) {

        ArrayList<String> arr = new ArrayList<String>();

        try {

            ArrayList<String> c = new ArrayList<String>();

            BufferedReader br = new BufferedReader(new FileReader(params[0]), 1024);

            br.readLine();
            br.readLine();
            br.readLine();
            br.readLine();

            while (true) {

                boolean read;

                String str = br.readLine();

                if (str == null) break;

                int idx1 = str.indexOf(":");

                if (idx1 == -1)
                    read = true;
                else {

                    int idx2 = str.indexOf(" : ", idx1 + 1);

                    if (idx2 == -1)
                        read = false;
                    else {
                        str = str.substring(idx2 + 3);
                        read = true;
                    }
                }

                if (!read) continue;

                char[] b = str.toCharArray();

                for (int i = 0; i < b.length; i++) {

                    if (!isKorean(b[i]))
                        b[i] = '&';
                }

                for (String s : String.copyValueOf(b).split("&"))
                    c.add(s);
            }

            Log.d ("db", "len:" + c.size());

            for (String s : c) {

                if (s.length() > 0)
                    dbHandler.onNewWordGenerated (s);
            }

            br.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

		return arr;
	}
	@Override
	protected void onPostExecute(ArrayList<String> result) {

		Toast.makeText(context, "KT Download Complete.", Toast.LENGTH_SHORT).show();

		if (l != null)
			l.onPostExcute();
	}
	@Override
	protected void onProgressUpdate(Void... values) {}

    private final static char choTable[] = {
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
    };

    char getChosung (char c) {

        int idx = (c - 0xAC00)/28/21;

        if (idx < 0 || idx >= choTable.length)
            return '.';

        return choTable[idx];
    }

    private boolean isKorean (char c) {

        if (c >= 0x1100 && c <= 0x11FF) return true;
        //if (c >= 0x3130 && c <= 0x318F) return true;
        if (c >= 0xA960 && c <= 0xA97F) return true;
        if (c >= 0xAC00 && c <= 0xD7AF) return true;
        if (c >= 0xD7B0 && c <= 0xD7FF) return true;

        return false;
    }

}

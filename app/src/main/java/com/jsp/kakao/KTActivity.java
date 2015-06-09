package com.jsp.kakao;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.jsp.util.DatabaseHandler;
import com.jsp.util.DatabaseHelper;
import com.jsp.util.ExternalStorageHandler;
import com.jsp.movie.lesskey.R;

import java.io.File;


public class KTActivity extends Activity implements AdapterView.OnItemClickListener, KakaoParser.OnPostListener {

    private ListView list;
    private LinearLayout result;

    private String kakaotalkDir = "/KakaoTalk/Chats/";

    private String fileName = "/KakaoTalkChats.txt";

    private File dir;

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private DatabaseHandler dbHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("카카오톡에서 수집");

        setContentView(R.layout.kt_main);

        list = (ListView) findViewById(R.id.list);
        result = (LinearLayout) findViewById(R.id.result);

        progress = new ProgressDialog(this);

        dbHelper = new DatabaseHelper(this);
        try {
            dbHelper.createDataBase();
        } catch (Exception e) {

            e.printStackTrace();
        }
        db = dbHelper.openDataBase();
        dbHandler = new DatabaseHandler(db);
    }

    ProgressDialog progress;

    @Override
    public void onResume () {
        super.onResume();

        ExternalStorageHandler e = new ExternalStorageHandler();

        dir = e.setDir(this, Environment.getExternalStorageDirectory().getAbsolutePath() + kakaotalkDir);

        ListAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dir.list());

        list.setAdapter(adapter);
        list.setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        KakaoParser parser = new KakaoParser(this, dbHandler);

        progress.setMessage("Parsing Data :)");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();

        parser.l = this;
        parser.execute(new File (dir.listFiles()[position].getAbsolutePath() + fileName));
    }

    @Override
    public void onPostExcute() {

        progress.cancel();
    }
}

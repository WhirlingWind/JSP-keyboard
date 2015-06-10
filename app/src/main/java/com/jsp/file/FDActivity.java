package com.jsp.file;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.jsp.movie.lesskey.R;
import com.jsp.util.DatabaseHandler;
import com.jsp.util.DatabaseHelper;
import com.jsp.util.ExternalStorageHandler;

import java.io.File;


public class FDActivity extends Activity implements View.OnClickListener, FileDownloader.OnPostListener, FileParser.OnPostListener {

    private String url = "http://203.252.54.4:8080/Files/123.txt";

    private Button download;

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private DatabaseHandler dbHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("다운로드");

        setContentView(R.layout.fd_main);

        download = (Button) findViewById(R.id.downbtn);
        download.setOnClickListener(this);

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
    public void onClick(View v) {

        FileDownloader downloader = new FileDownloader(this, this, progress);

        progress.setMessage("Download Database :)");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();

        downloader.execute(url);
    }

    @Override
    public void onPostExecute() {

        progress.cancel();
    }

    @Override
    public void onPostExecute(File rtn, boolean result) {

        if (result) {
            FileParser parser = new FileParser(this, dbHandler);

            progress.setMessage("Parsing Data :)");

            parser.l = this;
            parser.execute(rtn);
        }
        else {
            progress.cancel();
            Toast.makeText(this, "Cannot Connect to Server.", Toast.LENGTH_LONG).show();
        }
    }
}

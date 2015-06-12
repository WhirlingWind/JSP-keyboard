package com.jsp.server;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.jsp.movie.lesskey.R;
import com.jsp.util.DatabaseHandler;
import com.jsp.util.DatabaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Han on 2015-06-09.
 */
public class ServerCon extends FragmentActivity implements View.OnClickListener{

    String File_Name;
    String File_extend = "txt";

    String fileURL ;
    String Save_Path="";
    String Save_folder = "/less_key";

    DownloadThread dThread;


    Button uploadBtn;
    Button downServerBtn;
    Button downUserBtn;
    //ProgressBar progressBar;
    private ProgressDialog mProgressDlg;

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private DatabaseHandler dbHandler;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.servercon);

        uploadBtn = (Button)findViewById(R.id.uploadbtn);
        downServerBtn = (Button)findViewById(R.id.downserverbtn);
        downUserBtn = (Button)findViewById(R.id.downuserbtn);
        //progressBar = (ProgressBar)findViewById(R.id.ProgressBar);

        uploadBtn.setOnClickListener(this);
        downServerBtn.setOnClickListener(this);
        downUserBtn.setOnClickListener(this);
        //progressBar.setVisibility(ProgressBar.GONE);

        String ext = Environment.getExternalStorageState();
        if (ext.equals(Environment.MEDIA_MOUNTED)) {
            Save_Path = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + Save_folder;
        }

        dbHelper = new DatabaseHelper(this);
        try {
            dbHelper.createDataBase();
        } catch (Exception e) {

            e.printStackTrace();
        }
        db = dbHelper.openDataBase();
        dbHandler = new DatabaseHandler(db);


    }

    public void onClick(View view){
        Intent i;
        if(view.getId() == R.id.uploadbtn) {
            i = new Intent(ServerCon.this,Upload.class);
            startActivity(i);
        }else if(view.getId() == R.id.downserverbtn)
        {
            File_Name = "serverdata"+System.currentTimeMillis() + ".txt";

            fileURL = "http://112.150.48.27:3303/downloadserver";


            File dir = new File(Save_Path);
            // 폴더가 존재하지 않을 경우 폴더를 만듦
            if (!dir.exists()) {
                dir.mkdir();
            }

            // 다운로드 실행
            dThread = new DownloadThread(fileURL ,
                    Save_Path + "/" + File_Name);
            dThread.start();

        }
        else if(view.getId() == R.id.downuserbtn)
        {
            i = new Intent(ServerCon.this,DownloadBackup.class);
            startActivity(i);
        }
    }


    class DownloadThread extends Thread {
        String ServerUrl;
        String LocalPath;

        DownloadThread(String serverPath, String localPath) {
            ServerUrl = serverPath;
            LocalPath = localPath;
        }

        @Override
        public void run() {

            ProgressHandler.sendMessage(Message.obtain(ProgressHandler, -3));

            URL imgurl;
            int Read;
            try {
                imgurl = new URL(ServerUrl);
                HttpURLConnection conn = (HttpURLConnection) imgurl
                        .openConnection();
                int len = conn.getContentLength();
                byte[] tmpByte = new byte[len];
                InputStream is = conn.getInputStream();
                File file = new File(LocalPath);
                FileOutputStream fos = new FileOutputStream(file);


                for (;;) {
                    Read = is.read(tmpByte);
                    if (Read <= 0) {
                        break;
                    }
                    fos.write(tmpByte, 0, Read);
                }
                is.close();
                fos.close();
                conn.disconnect();

            } catch (MalformedURLException e) {
                Log.e("ERROR1", e.getMessage());
            } catch (IOException e) {
                Log.e("ERROR2", e.getMessage());
                e.printStackTrace();
            }

            ProgressHandler.sendMessage(Message.obtain(ProgressHandler,-2));
            runOnUiThread(new Runnable() {
                public void run() {

                    Toast toast = Toast.makeText(ServerCon.this, "파일 다운로드 완료\nDB INSERT 중 입니다...\n잠시만 기다려주십시오",Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();
                }
            });

            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            DownloadFileDBInsert();
        }
    }



    Handler ProgressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            // 파일 다운로드 종료 후 다운받은 파일을 DB 삽입.
            super.handleMessage(msg);
            int var;
            //System.out.println(msg.what);
            switch(var = msg.what) {

                case -1:{
                    //progressBar.incrementProgressBy(1);
                    mProgressDlg.incrementProgressBy(1);
                    break;

                }
                case -2:{
                    //progressBar.setVisibility(ProgressBar.GONE);
                    mProgressDlg.dismiss();

                    break;
                }
                case -3:{
                    mProgressDlg = ProgressDialog.show(ServerCon.this,"", "다운로드 중입니다...",true,false);
                    break;
                }
                default:{
                    /*
                    progressBar.setMax(var);
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                    progressBar.setProgress(0);
                    */
                    mProgressDlg = new ProgressDialog(ServerCon.this);
                    mProgressDlg.setMessage("DB INSERT 중입니다...");
                    mProgressDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    mProgressDlg.setMax(var);
                    mProgressDlg.show();
                    mProgressDlg.setCancelable(false);
                    break;
                }
            }
        }

    };

    private void DownloadFileDBInsert() {
        File file = new File(Save_Path + "/" + File_Name);
        dbHandler.ServerTableInitializer(file, ProgressHandler);
        runOnUiThread(new Runnable() {
            public void run() {

                Toast toast = Toast.makeText(ServerCon.this, "DB INSERT 완료\n감사합니다", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();
            }
        });

        return;
    }



}
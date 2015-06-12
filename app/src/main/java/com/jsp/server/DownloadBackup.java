package com.jsp.server;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.jsp.movie.lesskey.R;
import com.jsp.util.DatabaseHandler;
import com.jsp.util.DatabaseHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Han on 2015-06-09.
 */
public class DownloadBackup extends Activity implements View.OnClickListener {

    EditText editid;
    EditText editpw;
    private String id;
    private String pw;
    StringBuilder output = new StringBuilder();


    private ProgressDialog mProgressDlg;

    /** Called when the activity is first created. */
    String File_Name;
    String File_extend = "txt";

    String fileURL ;
    String idURL;
    String Save_Path="";
    String Save_folder = "/less_key";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private DatabaseHandler dbHandler;

    DownloadThread dThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.downloadbackup);

        editid = (EditText)findViewById(R.id.editid);
        editpw = (EditText)findViewById(R.id.editpw);
        Button btn = (Button) findViewById(R.id.downbtn);
        btn.setOnClickListener(this);


        // 다운로드 경로를 외장메모리 사용자 지정 폴더로 함.
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

    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        if (view.getId() == R.id.downbtn) {

            if(editid.getText().toString().isEmpty() || editpw.getText().toString().isEmpty() ) {
                runOnUiThread(new Runnable() {
                    public void run() {

                        Toast toast = Toast.makeText(DownloadBackup.this, "모든 정보를 적어주십시오",Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                        toast.show();
                    }
                });
                return;
            }

            File_Name = "backup"+System.currentTimeMillis() + ".txt";

            id = editid.getText().toString();
            pw = editpw.getText().toString();



            fileURL = "http://112.150.48.27:3303/download?id=" + id + "&pw=" + pw;
            idURL = "http://112.150.48.27:3303/idpwcheck?id=" + id + "&pw=" + pw;

            File dir = new File(Save_Path);
            // 폴더가 존재하지 않을 경우 폴더를 만듦
            if (!dir.exists()) {
                dir.mkdir();
            }

            // 다운로드
            dThread = new DownloadThread(fileURL ,
                    Save_Path + "/" + File_Name,idURL);
            dThread.start();

        }
    }

    // 다운로드 쓰레드로 돌림..
    class DownloadThread extends Thread {
        String ServerUrl;
        String LocalPath;
        String IDCheckUrl;

        DownloadThread(String serverPath, String localPath, String IDCheckPath) {
            ServerUrl = serverPath;
            LocalPath = localPath;
            IDCheckUrl = IDCheckPath;
        }

        @Override
        public void run() {
            URL imgurl;
            int Read;

            try {
                imgurl = new URL(IDCheckUrl);
                HttpURLConnection conn = (HttpURLConnection) imgurl
                        .openConnection();
                InputStream is = conn.getInputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = null;

                while(true){
                    line = reader.readLine();
                    if(line == null){
                        break;
                    }
                    output.append(line);
                }
                reader.close();

                System.out.println(output.toString() + "this!!!");

                is.close();
                conn.disconnect();
                if(output.toString().equals("SUCCESS"))
                {

                    output.setLength(0);

                }else{
                    runOnUiThread(new Runnable() {
                        public void run() {

                            Toast toast = Toast.makeText(DownloadBackup.this, "해당 아이디가 존재하지 않거나\n패스워드가 틀렸습니다",Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                            toast.show();
                        }
                    });

                    output.setLength(0);
                    return;
                }


            } catch (MalformedURLException e) {
                Log.e("ERROR1", e.getMessage());
            } catch (IOException e) {
                Log.e("ERROR2", e.getMessage());
                e.printStackTrace();
            }


            ProgressHandler.sendMessage(Message.obtain(ProgressHandler, -3));

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

                    Toast toast = Toast.makeText(DownloadBackup.this, "파일 다운로드 완료\nDB에 INSERT 중 입니다...\n잠시만 기다려주십시오",Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();
                }
            });

            downloadFilePrivateInsert();
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
                    mProgressDlg = ProgressDialog.show(DownloadBackup.this, "", "다운로드 중입니다...", true, false);
                    break;
                }
                default:{
                    /*
                    progressBar.setMax(var);
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                    progressBar.setProgress(0);
                    */
                    mProgressDlg = new ProgressDialog(DownloadBackup.this);
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

    private void downloadFilePrivateInsert() {
        File file = new File(Save_Path + "/" + File_Name);

        dbHandler.PrivateTableInitializer(file, ProgressHandler);

        runOnUiThread(new Runnable() {
            public void run() {

                Toast toast = Toast.makeText(DownloadBackup.this, "DB INSERT 완료\n감사합니다", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();
            }
        });

        return;

    }


}
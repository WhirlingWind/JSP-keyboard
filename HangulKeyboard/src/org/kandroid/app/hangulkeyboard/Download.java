package org.kandroid.app.hangulkeyboard;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import android.os.Handler;

public class Download extends Activity implements OnClickListener {
    /** Called when the activity is first created. */
    String File_Name = "�����̸�(Ȯ��������)";
    String File_extend = "Ȯ����";

    String fileURL = "�ٿ�ε� URL"; // URL
    String Save_Path="";
    String Save_folder = "/mydown";

    ProgressBar loadingBar;
    DownloadThread dThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download);

        Button btn = (Button) findViewById(R.id.downbtn);
        btn.setOnClickListener(this);

        loadingBar = (ProgressBar) findViewById(R.id.Loading);

        // �ٿ�ε� ��θ� ����޸� ����� ���� ������ ��.
        String ext = Environment.getExternalStorageState();
        if (ext.equals(Environment.MEDIA_MOUNTED)) {
            Save_Path = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + Save_folder;
        }
    }

    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        if (view.getId() == R.id.downbtn) {
            File dir = new File(Save_Path);
            // ������ �������� ���� ��� ������ ����
            if (!dir.exists()) {
                dir.mkdir();
            }

            // �ٿ�ε� ������ ������ ���ϸ��� �����ϴ��� Ȯ���ؼ�
            // ������ �ٿ�ް� ������ �ش� ���� �����Ŵ.
            if (new File(Save_Path + "/" + File_Name).exists() == false) {
                loadingBar.setVisibility(View.VISIBLE);
                dThread = new DownloadThread(fileURL + "/" + File_Name,
                        Save_Path + "/" + File_Name);
                dThread.start();
            } else {
                showDownloadFile();
            }
        }
    }

    // �ٿ�ε� ������� ����..
    class DownloadThread extends Thread {
        String ServerUrl;
        String LocalPath;

        DownloadThread(String serverPath, String localPath) {
            ServerUrl = serverPath;
            LocalPath = localPath;
        }

        @Override
        public void run() {
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
            mAfterDown.sendEmptyMessage(0);
        }
    }

    Handler mAfterDown = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            loadingBar.setVisibility(View.GONE);
            // ���� �ٿ�ε� ���� �� �ٿ���� ������ �����Ų��.
            showDownloadFile();
        }

    };

    private void showDownloadFile() {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        File file = new File(Save_Path + "/" + File_Name);

        // ���� Ȯ���� ���� mime type ������ �ش�.
        if (File_extend.equals("mp3")) {
            intent.setDataAndType(Uri.fromFile(file), "audio/*");
        } else if (File_extend.equals("mp4")) {
            intent.setDataAndType(Uri.fromFile(file), "vidio/*");
        } else if (File_extend.equals("jpg") || File_extend.equals("jpeg")
                || File_extend.equals("JPG") || File_extend.equals("gif")
                || File_extend.equals("png") || File_extend.equals("bmp")) {
            intent.setDataAndType(Uri.fromFile(file), "image/*");
        } else if (File_extend.equals("txt")) {
            intent.setDataAndType(Uri.fromFile(file), "text/*");
        } else if (File_extend.equals("doc") || File_extend.equals("docx")) {
            intent.setDataAndType(Uri.fromFile(file), "application/msword");
        } else if (File_extend.equals("xls") || File_extend.equals("xlsx")) {
            intent.setDataAndType(Uri.fromFile(file),
                    "application/vnd.ms-excel");
        } else if (File_extend.equals("ppt") || File_extend.equals("pptx")) {
            intent.setDataAndType(Uri.fromFile(file),
                    "application/vnd.ms-powerpoint");
        } else if (File_extend.equals("pdf")) {
            intent.setDataAndType(Uri.fromFile(file), "application/pdf");
        }
        startActivity(intent);
    }

/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/
}
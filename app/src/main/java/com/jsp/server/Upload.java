package com.jsp.server;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jsp.movie.lesskey.R;
import com.jsp.util.DatabaseHandler;
import com.jsp.util.DatabaseHelper;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Han on 2015-06-09.
 */
public class Upload extends FragmentActivity implements View.OnClickListener{
    Button uploadBtn;

    EditText editid;
    EditText editpw;
    EditText editage;
    TextView messageText;
    Button uploadButton;
    int serverResponseCode = 0;
    private ProgressDialog mProgressDlg;

    String upLoadServerUri = null;

    /**********  File Path *************/
    //final String uploadFilePath = "/mnt/sdcard/Download/";
    //final String uploadFileName = "GameCIH.apk";
    StringBuilder output = new StringBuilder();


    //private data path
    String uploadFilePath="";
    String Save_folder = "/less_key";
    String uploadFileName;
    String IDCheckUrl;

    private String id;
    private String pw;
    private int age;


    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;
    private DatabaseHandler dbHandler;

    private ProgressBar mProgressStick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload);

        uploadBtn = (Button)findViewById(R.id.uploadbtn);
        editid = (EditText)findViewById(R.id.editid);
        editpw = (EditText)findViewById(R.id.editpw);
        editage = (EditText)findViewById(R.id.editage);

        uploadBtn.setOnClickListener(this);

        //upLoadServerUri = "http://192.168.219.198:3303/upload?id=han&pw=sang&age=25";

        String ext = Environment.getExternalStorageState();
        if (ext.equals(Environment.MEDIA_MOUNTED)) {
            uploadFilePath = Environment.getExternalStorageDirectory()
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




            if(editid.getText().toString().isEmpty() || editpw.getText().toString().isEmpty() || editage.getText().toString().isEmpty()) {
                runOnUiThread(new Runnable() {
                    public void run() {

                        Toast toast = Toast.makeText(Upload.this, "모든 정보를 적어주십시오",Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                        toast.show();
                    }
                });
                return;
            }

            uploadFileName = "privatedata"+System.currentTimeMillis() + ".txt";

            id = editid.getText().toString();
            pw = editpw.getText().toString();
            age = Integer.parseInt(editage.getText().toString());

            upLoadServerUri = "http://112.150.48.27:3303/upload?id=" + id + "&pw=" + pw + "&age="+ age;
            IDCheckUrl = "http://112.150.48.27:3303/idcheck?id=" + id;

            Thread uploading = new Thread(new Runnable() {
                public void run() {

                    uploadFile(uploadFilePath + "/" + uploadFileName, uploadFileName,IDCheckUrl);
                    System.out.println("uploadcomp");

                }
            });
            uploading.start();
        }
    }

    public int uploadFile(String sourceFileUri, String filename,String idurl) {


        String fileName = sourceFileUri;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);
        URL checkurl;


        try {
            checkurl = new URL(idurl);
            conn = (HttpURLConnection) checkurl.openConnection();
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
                runOnUiThread(new Runnable() {
                    public void run() {

                        Toast toast = Toast.makeText(Upload.this, "같은 아이디가 존재합니다",Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                        toast.show();
                    }
                });

                output.setLength(0);
                return  0;

            }else{


                output.setLength(0);
            }


        } catch (MalformedURLException e) {
            Log.e("ERROR1", e.getMessage());
        } catch (IOException e) {
            Log.e("ERROR2", e.getMessage());
            e.printStackTrace();
        }

        exportPrivateFile(filename);




        if (!sourceFile.isFile()) {

            //dialog.dismiss();

            Log.e("uploadFile", "Source File not exist :"
                    + uploadFilePath + "" + uploadFileName);

            runOnUiThread(new Runnable() {
                public void run() {
                    messageText.setText("Source File not exist :"
                            +uploadFilePath + "" + uploadFileName);
                }
            });

            return 0;

        }
        else
        {




            ProgressHandler.sendMessage(Message.obtain(ProgressHandler, -3));


            try {

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + fileName + "\"" + lineEnd);

                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);



                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {

                //dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("MalformedURLException Exception : check script url.");
                        Toast.makeText(Upload.this, "MalformedURLException",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {

                //dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    public void run() {
                        messageText.setText("Got Exception : see logcat ");
                        Toast.makeText(Upload.this, "Got Exception : see logcat ",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e("UploadException", "Exception : "+ e.getMessage(), e);
            }

            ProgressHandler.sendMessage(Message.obtain(ProgressHandler,-2));

            runOnUiThread(new Runnable() {
                public void run() {

                    Toast toast = Toast.makeText(Upload.this, "업로드 완료",Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                    toast.show();
                }
            });

            return serverResponseCode;

        } // End else block
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
                    mProgressDlg = ProgressDialog.show(Upload.this, "", "업로드 중입니다...", true, false);
                    break;
                }
                case -4:{
                    mProgressDlg = ProgressDialog.show(Upload.this, "", "파일 생성 중입니다...", true, false);
                    break;
                }
                default:{
                    /*
                    progressBar.setMax(var);
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                    progressBar.setProgress(0);
                    */
                    mProgressDlg = new ProgressDialog(Upload.this);
                    mProgressDlg.setMessage("DB 추출 중입니다...");
                    mProgressDlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    mProgressDlg.setMax(var);
                    mProgressDlg.show();
                    mProgressDlg.setCancelable(false);

                    break;
                }
            }
        }

    };


    public void exportPrivateFile(String fileName){
        File dir = new File(uploadFilePath);
        // 폴더가 존재하지 않을 경우 폴더를 만듦
        if (!dir.exists()) {
            dir.mkdir();
        }

        List<String> result = new ArrayList<String>();
        dbHandler.PrivateDataExport(result,ProgressHandler);

        ProgressHandler.sendMessage(Message.obtain(ProgressHandler,-4));
        try {
            FileWriter writer = new FileWriter(uploadFilePath + "/" + fileName);
            for(String str: result) {
                writer.write(str);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ProgressHandler.sendMessage(Message.obtain(ProgressHandler,-2));


    }





}
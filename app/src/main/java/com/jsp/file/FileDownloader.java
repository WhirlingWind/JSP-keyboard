package com.jsp.file;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class FileDownloader extends AsyncTask<String, Integer, Boolean> {

	public interface OnPostListener {
		
		public void onPostExecute(File rtn, boolean result);
	}
	
	private OnPostListener l = null;

	private String fileName = "downloadFile.txt";

	private Context context;

    private File downFile;

    private ProgressDialog progress;

	public FileDownloader (Context cxt, OnPostListener l, ProgressDialog progress) {

		context = cxt;
		this.l = l;
        this.progress = progress;
	}
	
	@Override
	protected Boolean doInBackground(String... serverUrl) {

        boolean rtn = true;

        downFile = new File(Environment.getDownloadCacheDirectory().getAbsolutePath() + fileName);

		try {
			
			URL url = new URL(serverUrl[0]);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			int len = conn.getContentLength();
			int total = 0, show = 0;
			byte[] tmpByte = new byte[1024];
			
			InputStream is = conn.getInputStream();
			FileOutputStream fos = new FileOutputStream(downFile);

			while (true) {
				
				int read = is.read(tmpByte);
				
				if (read <= 0)
					break;
				
				fos.write(tmpByte, 0, read);
				
				if (10*total/len > show) {
					
					show++;
					
					publishProgress(show);
				}
			}

			is.close();
			fos.close();
			conn.disconnect();

		} catch (Exception e) {

            e.printStackTrace();

            rtn = false;
		}
		
		return rtn;
	}
	@Override
	protected void onPostExecute(Boolean result) {

		if (l != null)
			l.onPostExecute(downFile, result);
	}
	@Override
	protected void onProgressUpdate(Integer... values) {

        progress.setMessage("Download Database :) " + values[0] + "%.");
	}

}

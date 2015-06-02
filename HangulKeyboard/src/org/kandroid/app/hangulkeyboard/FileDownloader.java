package org.kandroid.app.hangulkeyboard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class FileDownloader extends AsyncTask<Void, Integer, Void> {

	public interface OnPostListener {
		
		public void onPostExcute ();
	}
	
	private OnPostListener l = null;

	private String Save_Path="";
	private String Save_folder = "/mydown";

	private String ServerUrl;
	private String LocalPath;

	private Context context;
	
	public FileDownloader (Context cxt, String serverUrl, String localPath, OnPostListener l) {
		
		super ();
		
		context = cxt;
		this.l = l;
		
		String ext = Environment.getExternalStorageState();
		if (ext.equals(Environment.MEDIA_MOUNTED)) {
			Save_Path = Environment.getExternalStorageDirectory()
					.getAbsolutePath() + Save_folder;
		}

		File dir = new File(Save_Path);

		if (!dir.exists()) {
			dir.mkdir();
		}

	}
	
	@Override
	protected Void doInBackground(Void... params) {

		URL imgurl;
		int read;
		
		try {
			
			imgurl = new URL(ServerUrl);
			HttpURLConnection conn = (HttpURLConnection) imgurl.openConnection();
			int len = conn.getContentLength();
			int total = 0, show = 0;
			byte[] tmpByte = new byte[len];
			
			InputStream is = conn.getInputStream();
			File file = new File(LocalPath);
			FileOutputStream fos = new FileOutputStream(file);
			
			while (true) {
				
				read = is.read(tmpByte);
				
				if (read <= 0)
					break;
				
				fos.write(tmpByte, 0, read);
				
				total += read;
				if (10*total/len > show) {
					
					show++;
					
					publishProgress(show);
				}
			}

			is.close();
			fos.close();
			conn.disconnect();

		} catch (MalformedURLException e) {
			Log.e("ERROR1", e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("ERROR2", e.getMessage());
			e.printStackTrace();
		}
		
		return null;
	}
	@Override
	protected void onPostExecute(Void result) {

		Toast.makeText(context, "DB Download Complete.", Toast.LENGTH_SHORT).show();

		if (l != null)
			l.onPostExcute();
	}
	@Override
	protected void onProgressUpdate(Integer... values) {

		Toast.makeText(context, "DB Download... " + values[0] + "%.", Toast.LENGTH_SHORT).show();
	}

}

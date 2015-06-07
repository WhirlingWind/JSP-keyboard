package org.kandroid.app.hangulkeyboard;

import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.util.Log;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


public class CandidateGenerator extends AsyncTask<Void, Void, List<String>> {
	
	public interface OnPostListener {
		
		public void onPostExcute (List<String> res, boolean valid);
	}
	
	private List<String> array;
	private OnPostListener l;
	private boolean valid;
	
    private SQLiteDatabase db;
    private Cursor cursor;
    private String SQL;
    private String previous;
    private int resultCnt;
    private int ctxCnt;

	   
	public CandidateGenerator (List<String> arr, boolean valid, OnPostListener l, SQLiteDatabase db, String previous) {
		
		super ();
		
		array = arr;
		this.valid = valid;
		this.l = l;
		this.db = db;
		this.previous = previous;
	}
	
	@Override
	protected List<String> doInBackground(Void... params) {

		List<String> rtn = new ArrayList<String>();
//		List<String> ctx = new ArrayList<String>();
		SQL = "select * "
				+ "from Private "
				+ "where initial = '" + array.get(0) + "' "
				+ "order by priority desc "
				+ "limit 5";
		cursor = db.rawQuery(SQL, null);
		resultCnt = cursor.getCount();
		Log.d("check", "count "+ resultCnt);
		if(resultCnt > 0)
		{
			for(int i=0;i<resultCnt;i++)
			{
				cursor.moveToNext();
				rtn.add(cursor.getString(1));
			}
		}
		int temp = resultCnt;
		if(resultCnt < 5)
		{
			SQL = "select * "
					+ "from Server "
					+ "where initial = '" + array.get(0) + "' "
					+ "order by access_num desc "
					+ "limit " + (5 - resultCnt);
			cursor = db.rawQuery(SQL, null);
			Log.d("check", "test");
			resultCnt = cursor.getCount();
			if(resultCnt > 0)
			{
				for(int i=0;i<resultCnt;i++)
				{
					cursor.moveToNext();
					rtn.add(cursor.getString(1));
				}
				for(int i=0;i<resultCnt;i++)
				{
					Log.d("check", array.get(0) + " " + rtn.get(temp) + " " + (500 - i * 100));
					SQL = "insert into Private "
							+ "values('" + array.get(0) + "', '" + rtn.get(temp++) + "', " + (500 - i * 100) + ", 0)";
					try{
					db.execSQL(SQL);
					} catch (Exception e) {
						Log.d("check error", e.toString());
					}
					SQL = "select * "
							+ "from Private "
							+ "where word = '" + rtn.get(temp-1) + "'";
					cursor = db.rawQuery(SQL, null);
					cursor.moveToNext();
					Log.d("check1", cursor.getString(0) + " "+ cursor.getString(1) + " "+ cursor.getString(2) + " "+ cursor.getString(3));
					
				}
			}
		}
/*		if(previous != null) {
			SQL = "select word "
					+ "from Context "
					+ "where pre1 = '" + previous + "' "
					+ "or pre2 = '" + previous + "' "
					+ "or pre3 = '" + previous + "' "
					+ "or pre4 = '" + previous + "' "
					+ "or pre5 = '" + previous + "'";
			cursor = db.rawQuery(SQL, null);
			if((ctxCnt = cursor.getCount()) > 0)
				for(int i=0;i<ctxCnt;i++)
					ctx.add(cursor.getString(i));
			for(int i=0;i<resultCnt;i++)
				for(int j=0;j<ctxCnt;j++)
					;
		}*/
		return rtn;
	}

	@Override
	protected void onPostExecute(List<String> result) {

		l.onPostExcute(result, valid);
	}
}

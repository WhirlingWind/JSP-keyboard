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
					if (temp == 0)
						rtn.add(cursor.getString(1));
					else {
						int j;
						Log.d("hoho", temp + " " + rtn.size());
						for(j=0;j<temp;j++)
							if(cursor.getString(1).compareTo(rtn.get(j)) == 0)
								break;
						if(temp == j)
						{
							rtn.add(cursor.getString(1));
							SQL = "insert into Private "
									+ "values('" + array.get(0) + "', '" + rtn.get(temp+j) + "', " + (500 - i * 100) + ", 0)";
							db.execSQL(SQL);
						}
					}
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

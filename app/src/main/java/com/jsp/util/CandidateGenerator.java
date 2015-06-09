package com.jsp.util;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class CandidateGenerator extends AsyncTask<Void, Void, List<String>> {

	public interface OnPostListener {
		
		public void onPostExcute(List<String> res, boolean valid);
	}

    private List<String> array;
    private OnPostListener l;
    private boolean valid;
    private String previous;
    private DatabaseHandler dbHandler;

    public CandidateGenerator (List<String> arr, boolean valid, OnPostListener l, DatabaseHandler dbHandler, String previous) {
		
		super ();
		
		array = arr;
		this.valid = valid;
		this.l = l;
        this.previous = previous;
        this.dbHandler = dbHandler;
    }
	
	@Override
	protected List<String> doInBackground(Void... params) {

        List<String> rtn = new ArrayList<String>();

        dbHandler.CandidateListGenerator(rtn, array.get(0), previous);

        return rtn;
	}

	@Override
	protected void onPostExecute(List<String> result) {

		l.onPostExcute(result, valid);
	}

}

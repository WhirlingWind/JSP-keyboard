package org.kandroid.app.hangulkeyboard;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Setting extends Activity {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		LinearLayout mainLayout = new LinearLayout(this);
		mainLayout.setBackgroundColor(Color.parseColor("#ffffff"));
		
		TextView txtView = new TextView(this);
		txtView.setText("First one");
		mainLayout.addView(txtView);
		
		setContentView(mainLayout);
	}
}
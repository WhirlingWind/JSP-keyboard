package org.kandroid.app.hangulkeyboard;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;

public class Setting extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting);
		
		LinearLayout update = (LinearLayout)findViewById(R.id.data_update);
		LinearLayout clear = (LinearLayout)findViewById(R.id.clear_data);
		LinearLayout key_arr = (LinearLayout)findViewById(R.id.key_arrangement);
		//update.setOnClickListener();
		
	}
}
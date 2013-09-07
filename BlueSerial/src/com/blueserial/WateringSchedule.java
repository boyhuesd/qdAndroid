package com.blueserial;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public class WateringSchedule extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_watering_schedule);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.watering_schedule, menu);
		return true;
	}
	
	public void back(View view) {
		this.finish();
	}

}

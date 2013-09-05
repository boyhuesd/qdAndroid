package com.blueserial;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public class FlowerSelect extends Activity {
	
	Global global;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_flower_select);
		
		global = (Global) getApplication();
		global.set_plantType((byte) 0x05);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.flower_select, menu);
		return true;
	}
	
	public void back(View view)
	{
		this.finish();
	}

}

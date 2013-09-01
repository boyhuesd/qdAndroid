package com.blueserial;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;


import android.content.Intent;

import android.os.Bundle;

import android.view.View;

import android.view.Window;

import android.widget.Button;

import android.widget.Toast;



public class Welcome extends Activity implements View.OnClickListener {

	public static final int BT_ENABLE_REQUEST = 10; // This is the code we use for BT Enable
	public static final int SETTINGS = 20;

	public static BluetoothAdapter mBTAdapter;
	
	private Button mBtnPlanManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		 

		setContentView(R.layout.activity_welcomescreen);
		ActivityHelper.initialize(this);
		
		
		mBtnPlanManager = (Button) findViewById(R.id.btnPlantManager);

		mBtnPlanManager.setOnClickListener(this);

/*		mBTAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBTAdapter == null) 
		{
			Toast.makeText(getApplicationContext(), "Bluetooth not found", Toast.LENGTH_SHORT).show();
		} else if (!mBTAdapter.isEnabled()) 
		{
			Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBT, BT_ENABLE_REQUEST);
	    }
*/
	}

		
	
	/**
	 * Called when the screen rotates. If this isn't handled, data already generated is no longer available
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		
	}
	
	@Override
	public void onClick(View v) {
	switch (v.getId()) {
	
	case R.id.btnPlantManager:
		Intent intent = new Intent(Welcome.this, Homescreen.class);
		startActivity(intent);
/*	
	case R.id.btnSearch:
		
		//Log.d(TAG, "in onClick(" + v + ")");
		// IntentFilter for found devices
		IntentFilter foundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		// Broadcast receiver for any matching filter
		WelcomeActivity.this.registerReceiver(mReceiver, foundFilter);

		IntentFilter doneFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		WelcomeActivity.this.registerReceiver(mReceiver, doneFilter);
	*/
		};
		
	}
	



	
	@Override
	protected void onPause() {

		super.onPause();
	}

	@Override
	protected void onResume() {

		super.onResume();
	}

	@Override
	protected void onStop() {

		super.onStop();
	}
}

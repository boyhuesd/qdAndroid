/*
 * Released under MIT License http://opensource.org/licenses/MIT
 * Copyright (c) 2013 Plasty Grove
 * Refer to file LICENSE or URL above for full text 
 */

package com.blueserial;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.UUID;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
	
	
	private Handler tickRoutine = new Handler();
	private Handler rxBufferHandler = new Handler();
	private Handler initHandler = new Handler();
	private Handler sendTimerHandler = new Handler();
	
	private static final String TAG = "BlueTest5-MainActivity";
	private int mMaxChars = 50000;//Default
	private UUID mDeviceUUID;
	public static BluetoothSocket mBTSocket;
	private ReadInput mReadThread = null;
	static char[] command = {'A','B','C','D'};
	
	byte[] rxBuffer = new byte[256];
	byte[] txBuffer = new byte[256];
	
	
	private boolean mIsUserInitiatedDisconnect = false;

	private boolean mIsBluetoothConnected = false;

	private BluetoothDevice mDevice;

	private ProgressDialog progressDialog;

	

	
	public static byte[] REQ_DATA = {-0x78, 0x00};
	public static byte[] CMD_START= {0X33, 0X00};	

	public static byte[] CMD_FIND = {0X11, 0X00};
	
	public static int MOISTURE;
	public static int LIGHT;
	public static int HUMIDITY;
	public static int TEMP;
	public static int BATTERY;
	public static byte CHECKSUM;
	public static byte PUMB;
	
	
	
	// Variables for initialCommunication
	public boolean flag = false;
	public static final byte FLAG_ID = 0x72;
	public static final int MAX_COUNTER = 150;	// Timeout = maxcounter x postdelay 
	public static final byte REQ_PLANT_TYPE = 0x23;
	public static final byte REQ_FLAG = 0x22;
	public boolean flagRxed = false;
	public static final int POST_DELAY_TIME = 500;
	public static final byte CMD_SET_PLANT = 0x24;
	public static final byte CMD_SET_FLAG = 0x25;
	
	
	// Constant for DATE
	public static final byte MON = 0x02;
	public static final byte TUE = 0x03;
	public static final byte WED = 0x04;
	public static final byte THU = 0x05;
	public static final byte FRI = 0x06;
	public static final byte SAT = 0x07;
	public static final byte SUN = 0x01;
	
	// Constant for common communication
	public static final byte RESP_OK = 0x77;
	public static final byte CMD_DAY = 0x01;
	public static final byte CMD_HOUR = 0x02;
	public static final byte CMD_MIN = 0x03;
	
	
	//Intent plantSelIntent = new Intent(this, FlowerSelect.class);
	
	Global global;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mainactivity);
		ActivityHelper.initialize(this);

		Intent intent = getIntent();
		Bundle b = intent.getExtras();
		mDevice = b.getParcelable(Homescreen.DEVICE_EXTRA);
		mDeviceUUID = UUID.fromString(b.getString(Homescreen.DEVICE_UUID));
		mMaxChars = b.getInt(Homescreen.BUFFER_SIZE);
		
		
	}

	
	private Runnable sendDate = new Runnable() {
		
	@Override
	public void run() { 
		byte counter = 0;
		byte sendQueue = 1;
		
		Calendar now = Calendar.getInstance(); 
			
			switch (sendQueue)
			{
			case 1:{					// Send DAY command
				txBuffer[0] = CMD_DAY;
				txBuffer[1] = intToByte(now.get(Calendar.DAY_OF_WEEK));
				if (rxBuffer[0] == RESP_OK) {
					sendQueue = 2;		// Next
				}
				break;
			}
			case 2:{					// Send HOUR
				txBuffer[0] = CMD_HOUR;
				txBuffer[1] = intToByte(now.get(Calendar.HOUR_OF_DAY));
				if (rxBuffer[0] == RESP_OK) {
					sendQueue = 3;
				}
				break;
			}
			case 3: {					// Send MIN
				txBuffer[0] = CMD_HOUR;
				txBuffer[1] = intToByte(now.get(Calendar.MINUTE));
				if (rxBuffer[0] == RESP_OK) {
				}
				break;
			}
			
			default: break;
			}
			
			if (counter < MAX_COUNTER) {
				sendTimerHandler.postDelayed(sendDate, POST_DELAY_TIME);
			}
		} 
	};
	
	
	
	private Runnable Routine = new Runnable () 
	{	
		private boolean res = false;
		private boolean bStop = false;
		
		@Override
		public void run()
		{
			try
			{
				while(!bStop)
				{
					while(!res)
					{
						mBTSocket.getOutputStream().write(REQ_DATA);
				
						Thread.sleep(100);
				
						if (mBTSocket.getInputStream().available() == 10)
						{
							mBTSocket.getInputStream().read(rxBuffer);
					
							UpdateData.run();
					
							res = true;
						}

					}	
				}
					res = false;
					rxBuffer = null;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				
			}
			tickRoutine.postDelayed(Routine, 60000);
		}
		
		public void stop() 
		{
			bStop = true;
		}
	};

	private Runnable UpdateData = new Runnable() 
	{
		@Override
		public void run()
		{	
			
			MOISTURE 	= rxBuffer[0] << 8 + rxBuffer[1];
			LIGHT		= rxBuffer[2] << 8 + rxBuffer[3];
			HUMIDITY    = rxBuffer[4];
			TEMP 		= rxBuffer[5];
			BATTERY    	= rxBuffer[6] << 8 + rxBuffer[7];
			PUMB 		= rxBuffer[8];
		}
	};
	
	private Runnable bufferRx = new Runnable()
	{
		int i = 0;
		
		@Override
		public void run()
		{	
			//updateRx.append("5");
			//txBuffer[0] = (byte) ++i;
			//new Sendata().execute();
			rxBufferHandler.postDelayed(bufferRx, 500);
		}
	};

	private Runnable initialCommunication = new Runnable() {
		int counter = 0;
		byte rx = 0;
		int sendQueue = 1;
		
		@Override
		public void run() {
			
			Calendar now = Calendar.getInstance(); 
			/*
			if (flagRxed == false) {
				txBuffer[0] = 0x22;						// TODO define REQ_FLAG
				new Sendata().execute();
			}
			rx = rxBuffer[0];
			if ((rx & 0xfe) == FLAG_ID) {	// FlagID received, check the flag
				flagRxed = true;
				if ((rx & 0x01) == 1) {
					flag = true;			// Plant type declared
				}
				else {						// Switch to Select plant activity
					runOnUiThread(new Runnable() {
						  public void run() {
						    startActivity(new Intent(MainActivity.this, FlowerSelect.class));;
						  }
						});
				}
			}
			
			
			// Clear data buffer
			rxBuffer[0] = 0;
			counter++;
		
		if ((counter < MAX_COUNTER) && (flagRxed == false)) {
			initHandler.postDelayed(initialCommunication, 500);
		}
		else if ((counter < MAX_COUNTER) && (flagRxed == true) && (flag == true)) {
			txBuffer[0] = REQ_PLANT_TYPE;
			new Sendata().execute();
			initHandler.postDelayed(initialCommunication, 500);
		}
		else if (counter >= MAX_COUNTER) {			
			initHandler.removeCallbacks(initialCommunication);
			//Toast.makeText(getApplicationContext(), "@string/deviceNotCompatiable", Toast.LENGTH_LONG).show();
			TextView updateRx = (TextView) findViewById(R.id.txtAdvice);
			updateRx.setText("Device not compatible!");
			updateRx.setBackgroundColor(Color.RED);
		}	
		*/
			counter++;
			
			switch (sendQueue)
			{
			case 1: {					// Request FLAG
				txBuffer[0] = REQ_FLAG;
				new Sendata().execute();
				rx = rxBuffer[0];
				rxBuffer[0] = 0;
				if ((rx & 0xfe) == FLAG_ID) { // Flag rxed
					if ((rx & 0x01) == 1) // Flag set
					{
						sendQueue = 2;	// Goto request plantType command
					}
					else {
						global.set_plantType((byte) 0x00); // Clear plantType
						sendQueue = 3;	// Goto switch to select plantType
					}
				}
				break;
			}
			
			case 2: {					// Request plantType
				txBuffer[0] = REQ_PLANT_TYPE;
				new Sendata().execute();
				rx = rxBuffer[0];
				rxBuffer[0] = 0;
				//if 
				break;
			}
			
			case 3: {
				if (global.get_plantType() == (byte) 0x00)	// Select plantType
				{
					runOnUiThread(new Runnable() {
						  public void run() {
						    startActivity(new Intent(MainActivity.this,
						    		FlowerSelect.class));;
						  }
					});
				}
				else {
					sendQueue = 4; // Goto setFlag and set plantType
				}
				break;
			}
			
			case 4: {				// Set plantType
				txBuffer[0] = CMD_SET_PLANT;
				txBuffer[1] = (byte) global.get_plantType();
				new Sendata().execute();
				rx = rxBuffer[0];
				rxBuffer[0] = 0;
				if (rx == RESP_OK) {
					sendQueue = 5;
				}
				break;
			}
			
			case 5: {				// Set flag
				txBuffer[0] = CMD_SET_FLAG;
				txBuffer[1] = (byte) 0x00;
				new Sendata().execute();
				rx = rxBuffer[0];
				rxBuffer[0] = 0;
				if (rx == RESP_OK)
				{
					sendQueue = 6; 
				}
				break;
			}
			
			case 6: {				// Send day
				txBuffer[0] = CMD_DAY;
				txBuffer[1] = intToByte(now.get(Calendar.DAY_OF_WEEK));
				new Sendata().execute();
				rx = rxBuffer[0];
				rxBuffer[0] = 0;
				if (rx == RESP_OK) {
					sendQueue = 7;	// Next
				}
				break;
			}
			
			case 7: {				// Send hour
				txBuffer[0] = CMD_HOUR;
				txBuffer[1] = intToByte(now.get(Calendar.HOUR_OF_DAY));
				new Sendata().execute();
				rx = rxBuffer[0];
				rxBuffer[0] = 0;
				if (rx == RESP_OK) {
					sendQueue = 8; // Next
				}
				break;
			}
			
			case 8: {
				txBuffer[0] = CMD_MIN;
				txBuffer[1] = intToByte(now.get(Calendar.MINUTE));
				new Sendata().execute();
				rx = rxBuffer[0];
				rxBuffer[0] = 0;
				if (rx == RESP_OK) {
					sendQueue = 0; // END initial session
				}
			}
			
			
			
			default: break;
			}
			
			if (counter >= MAX_COUNTER) { // Init failed
				// TODO Code for initfailed
				initHandler.removeCallbacks(initialCommunication);
			}
			else {
				initHandler.postDelayed(initialCommunication,
						POST_DELAY_TIME);
			}
			
		}
	};
	


	private class unsignedbyte implements Runnable 
	{
		private Thread t;
		
		public unsignedbyte() 
		{
			t = new Thread(this, "unsigned byte");
			t.start();
		}
		@Override
		public void run() 
		{
			
		}
	}

	private class ReadInput implements Runnable 
	{
		
		private boolean bStop = false;
		private Thread t;

		public ReadInput() 
		{
			t = new Thread(this, "Input Thread");
			t.start();
		}

		public boolean isRunning() 
		{
			return t.isAlive();
		}

		@Override
		public void run() 
		{
			InputStream inputStream;

			try 
			{
				inputStream = mBTSocket.getInputStream();
				while (!bStop) {
					if (inputStream.available() > 0) {
						inputStream.read(rxBuffer);
					}
					}
					//Thread.sleep(500);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} /*catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/

		}

		public void stop() 
		{
			bStop = true;
		}

	}
	
	private class DisConnectBT extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Void doInBackground(Void... params) {

			if (mReadThread != null) {
				mReadThread.stop();
				while (mReadThread.isRunning())
					; // Wait until it stops
				mReadThread = null;

			}

			try {
				mBTSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mIsBluetoothConnected = false;
			if (mIsUserInitiatedDisconnect) {
				finish();
			}
		}

	}
	
	


	private class ConnectBT extends AsyncTask<Void, Void, Void> {
		private boolean mConnectSuccessful = true;

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(MainActivity.this, "Hold on", "Connecting");// http://stackoverflow.com/a/11130220/1287554
		}

		@Override
		protected Void doInBackground(Void... devices) {

			try {
				if (mBTSocket == null || !mIsBluetoothConnected) {
					mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
					BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
					mBTSocket.connect();
				}
			} catch (IOException e) {
				// Unable to connect to device
				e.printStackTrace();
				mConnectSuccessful = false;
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			if (!mConnectSuccessful) {
				Toast.makeText(getApplicationContext(), "@string/couldNotConnect", Toast.LENGTH_LONG).show();
				finish();
			} else {
				msg("@string/connected");
				mIsBluetoothConnected = true;
				mReadThread = new ReadInput(); // Kick off input reader
			}

			progressDialog.dismiss();
		}

	}
	
	private class Sendata extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Void doInBackground(Void... params) {
			//InputStream inputStream;
			
			if (txBuffer[0] != 0x00) {
				try{
	//			inputStream = mBTSocket.getInputStream();
	//			//mBTSocket.getOutputStream().write(hexStringToByteArray(mEditSend.getText().toString())[0]);
	//			if (inputStream.available() > 0) {
	//				byte[] buffer = new byte[2];
	//				if (inputStream.read(buffer) == 0x4f) {
	//					//mBTSocket.getOutputStream().write(hexStringToByteArray(mEditSend.getText().toString())[1]);
	//				}
	//			}
					mBTSocket.getOutputStream().write(txBuffer, 0, 2);
					txBuffer[0] = 0x00;
				} catch (IOException e){
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

		}

	}
	
	
	  public static int unsignedByte(byte b) 
	  {
		    return b & 0xFF;
	  }
	  
	public static byte intToByte(int value) {
	    return (byte)value;
	}
	

	
	private void msg(String s) {
		Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
	}
	/*
	 * 
	 * 
	 * Ham convert string to hex
	 */
	
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	@Override
	protected void onPause() {
		if (mBTSocket != null && mIsBluetoothConnected) {
			new DisConnectBT().execute();
		}
		Log.d(TAG, "Paused");
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mBTSocket == null || !mIsBluetoothConnected) {
			new ConnectBT().execute();
		}
		
		// Set flag to false
		flag = false;
		
		// Get global var
		global = ((Global) this.getApplication());
		
		//Start listening Rx
		rxBufferHandler.postDelayed(bufferRx, 2);
		
		// Initial communication
		while (mBTSocket == null);

		if (flag == false)				// Device din't setup
		{
			initHandler.post(initialCommunication);
		}
		
//		while (flagRxed == false);				// Wait until flag rxed
//		if (flag == false)				// Plant is not declared
//		{
//			initHandler.removeCallbacks(initialCommunication);
//			rxBufferHandler.removeCallbacks(bufferRx);
//			
//			// Switch to plant type select acitvity
//			Intent intent = new Intent(this, FlowerSelect.class);
//			startActivity(intent);
//		}
		
		Log.d(TAG, "Resumed");
		
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "Stopped");
		
	rxBufferHandler.removeCallbacks(bufferRx);
	flag = false;
	flagRxed = false;
	initHandler.removeCallbacks(initialCommunication);

		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}

	
	public void goBack(View view)
	{
		// Go back to connection screen
		this.finish();
	}
	
	private void refreshData()
	{
		TextView txtLight = (TextView) findViewById(R.id.txtLight);
		TextView txtHumidity = (TextView) findViewById(R.id.txtHumidity);
		TextView txtTemp = (TextView) findViewById(R.id.txtTemp);
		TextView txtMoisture = (TextView) findViewById(R.id.txtMoisture);
	}
}

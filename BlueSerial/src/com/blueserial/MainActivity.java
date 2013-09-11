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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
	
	final protected static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	private Handler tickRoutine = new Handler();
	private Handler initHandler = new Handler();
	private Handler sendTimerHandler = new Handler();
	private Handler updateUiHandler = new Handler();
	
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

	

	
	public static byte[] CMD_START= {0X33, 0X00};	

	public static byte[] CMD_FIND = {0X11, 0X00};
	
	public static int MOISTURE;
	public static int LIGHT;
	public static int HUMIDITY;
	public static int TEMP;
	public static int BATTERY;
	public static byte CHECKSUM;
	public static byte PUMB;
	
	// Variables for slave data
	public long rMoisture;
	public long rLight;
	public long rBattery;
	public long rHumidity;
	public long rTemperature;
	public boolean pumpAttached = false;
	
	// Variables for profile data
	public long pMoisture;
	public long pLight;
	public long pBattery;
	public long pHumidity;
	public long pTemperature;
	
	
	// Variables for initialCommunication
	public boolean flag = false;
	public static final byte FLAG_ID = 0x72;	// LSB must be zero
	public static final int MAX_COUNTER = 150;	// Timeout = maxcounter x postdelay 
	public static final byte REQ_PLANT_TYPE = 0x23;
	public static final byte REQ_FLAG = 0x22;
	public boolean flagRxed = false;
	public static final int POST_DELAY_TIME = 100;
	public static final byte CMD_SET_PLANT = 0x24;
	public static final byte CMD_SET_FLAG = 0x25;
	public static final byte CMD_END = 0x77;
	
	
	// Constant for DATE
	public static final byte MON = 0x02;
	public static final byte TUE = 0x03;
	public static final byte WED = 0x04;
	public static final byte THU = 0x05;
	public static final byte FRI = 0x06;
	public static final byte SAT = 0x07;
	public static final byte SUN = 0x01;
	
	// Constant for common communication
	public static final byte CMD_DAY = 0x01;
	public static final byte CMD_HOUR = 0x02;
	public static final byte CMD_MIN = 0x03;
	
	// Constant for slave responds
	public static final byte RESP_OK = 0x77;
	public static final byte RESP_PLANT_TYPE = 0x55;
	public static final byte RESP_DATA = 0x78;	// LSB must be zero
	
	// Constant for Master request
	public static final byte REQ_DATA = 0x30;

	
	// UI variables
	public TextView txtLight;
	public TextView txtHumidity;
	public TextView txtTemp;
	public TextView txtMoisture;
	public TextView txtAdvice;
	public ImageView imgWatering;
	
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
		
		
		txtLight = (TextView) findViewById(R.id.txtLight);
		txtHumidity = (TextView) findViewById(R.id.txtHumidity);
		txtTemp = (TextView) findViewById(R.id.txtTemp);
		txtMoisture = (TextView) findViewById(R.id.txtMoisture);
		txtAdvice = (TextView) findViewById(R.id.txtAdvice);
		imgWatering = (ImageView) findViewById(R.id.imgWatering);
		
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
	

	private Runnable updateUi = new Runnable() 
	{
		boolean updateSuccess = false;
		byte rx = 0;
		byte[] data = new byte[256];
		byte checksum = 0;
		byte counter = 0;
		boolean updating = true;
		int i;
		
		@Override
		public void run()
		{	
			
			/* Data format 
			 * <B_RESP_DATA + PUMP> <W_MOISTURE> <W_LIGHT> <W_BATTERY> <B_HUMIDITY>
			 * <B_TEMP> <B_CHECKSUM>
			 */
			
			if (updating) {
				runOnUiThread(new Runnable() {
					  public void run() {
						  progressDialog = ProgressDialog.show(MainActivity.this, "Refreshing data", "");
					  }
				});	
			}
			updating = false;
			
			
			// Request data
			counter++;
			txBuffer[0] = REQ_DATA;
			txBuffer[1] = 0;
			new Sendata().execute();
			while (rxBuffer[0] == 0) {
			}
			rx = rxBuffer[0];
			// Copy rxBuffer to data
			for (i = 0; i < 10; i++) {
				data[i] = rxBuffer[i];
			}
			
			
			if ((rx & 0xfe) == RESP_DATA) { // This deesn't work as rx = 0x79
				checksum = (byte) (RESP_DATA +
						data[1] +
						data[2] +
						data[3] +
						data[4] +
						data[5] +
						data[6] +
						data[7] +
						data[8]);
				

				
				if (checksum == data[9]) { 	// checksum valid
					// Update rValues
					rMoisture = (long) ((data[1] << 8) | data[2]);
					rLight = (long) ((data[3] << 8) | data[4]);
					rBattery = (long) ((data[5] << 8) | data[6]);
					rHumidity = (long) data[7];
					rTemperature = (long) data[8];
					
					if ((rx & 0x01) == 1) {
						pumpAttached = true;
					}
					else {
						pumpAttached = false;
					}
					
					// Update UI
					runOnUiThread(new Runnable() {
						  public void run() {
							  txtLight.setText(String.valueOf(rLight));
							  txtMoisture.setText(String.valueOf(rMoisture));
							  txtTemp.setText(String.valueOf(rTemperature));
							  txtHumidity.setText(String.valueOf(rHumidity));
							  
							  if (pumpAttached == true) {
								  imgWatering.setVisibility(ImageView.VISIBLE);
							  }
							  else {
								  imgWatering.setVisibility(ImageView.INVISIBLE);
							  }
							  
//							  txtAdvice.append(String.valueOf(checksum & 0xff) + " " +
//							  String.valueOf((int) (data[9] & 0xff)) + " ");
//							  txtAdvice.append(String.valueOf(data[0]) + " " +
//									  String.valueOf(data[1] & 0xff) + " " +
//									  String.valueOf(data[2] & 0xff) + " " +
//									  String.valueOf(data[3] & 0xff) + " " +
//									  String.valueOf(data[4] & 0xff) + " " +
//									  String.valueOf(data[5] & 0xff) + " " +
//									  String.valueOf(data[6] & 0xff) + " " +
//									  String.valueOf(data[7] & 0xff) + " " +
//									  String.valueOf(data[8] & 0xff) + " "
//									  );
						  }
					});	
					updateSuccess = true;
				}
			}
		
			
			if ((counter < MAX_COUNTER) && (updateSuccess == false))
			{
				updateUiHandler.postDelayed(updateUi, POST_DELAY_TIME);
			}
			else {						// Success
				counter = 0;
				updateSuccess = false;
				updateUiHandler.postDelayed(updateUi, 20000);
				
				updating = true;
				progressDialog.dismiss();
			}
		}
	};
	
	private Runnable initialCommunication = new Runnable() {
		int counter = 0;
		byte rx = 0;
		byte rx1 = 0;
		byte rx2 = 0;
		int sendQueue = 1;
		boolean success = false;
		
		@Override
		public void run() {

			Calendar now = Calendar.getInstance(); 
			counter++;
			
			switch (sendQueue)
			{
			case 1: {					// Request FLAG
				txBuffer[0] = REQ_FLAG;
				txBuffer[1] = 0;
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
				rx1 = rxBuffer[1];
				rx2 = rxBuffer[2];		// Checksum byte
				
				rxBuffer[0] = 0;
				
				if (rx == RESP_PLANT_TYPE) {
					// Check checksum
					if (rx2 == (byte) (rx + rx1)) {
						global.set_plantType(rx1);
						sendQueue = 6;
					}
				}
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
					sendQueue = 9;
					// START UPDATE UI
					//updateUiHandler.postDelayed(updateUi, POST_DELAY_TIME);
				}
			}
			
			case 9: {
				txBuffer[0] = CMD_END;
				txBuffer[1] = 0x00;
				new Sendata().execute();
				rx = rxBuffer[0];
				rxBuffer[0] = 0;
				if (rx == RESP_OK) {
					success = true;
				}
			}
			
			default: break;
			}
			
			if (success == false) {
				if (counter >= MAX_COUNTER) {	// Timed out
					counter = 0;
					initHandler.removeCallbacks(initialCommunication);
				}
				else {							// Repeat
					initHandler.postDelayed(initialCommunication,
							POST_DELAY_TIME);
				}
			}
			else {
				success = false;
				sendQueue = 1;
				counter = 0;
				// START UPDATE UI
				updateUiHandler.postDelayed(updateUi, POST_DELAY_TIME);
				initHandler.removeCallbacks(initialCommunication);
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
						
						runOnUiThread(new Runnable() {
							  public void run() {
								  if (rxBuffer[0] != 0) {
								  //txtAdvice.append(String.format("%02x", rxBuffer[0]&0xff) + " ");
								  }
							  }
						});	
						
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
	
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for ( int j = 0; j < bytes.length; j++ ) {
	        v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
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
		
	flag = false;
	flagRxed = false;
	initHandler.removeCallbacks(initialCommunication);
	updateUiHandler.removeCallbacks(updateUi);

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
	
	public void gotoWateringSchedule(View view) {
		Intent intent = new Intent(this, WateringSchedule.class);
		startActivity(intent);
	}
	
	public void refreshData(View view) {
		updateUiHandler.post(updateUi);
	}
}

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
import android.graphics.Color;

import com.blueserial.R;


import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
	
	
	private Handler tickRoutine = new Handler();
	private Handler m1Handler = new Handler();
	
	private static final String TAG = "BlueTest5-MainActivity";
	private int mMaxChars = 50000;//Default
	private UUID mDeviceUUID;
	public static BluetoothSocket mBTSocket;
	private ReadInput mReadThread = null;
	static char[] command = {'A','B','C','D'};
	
	public static byte[] databuffer;
	
	
	private boolean mIsUserInitiatedDisconnect = false;

	// All controls here
	private TextView mTxtReceive;
	private EditText mEditSend;
	private Button mBtnDisconnect;
	private Button mBtnSend;
	private Button mBtnClear;
	private Button mBtnClearInput;
	private ScrollView scrollView;
	private CheckBox chkScroll;
	private CheckBox chkReceiveText;

	private boolean mIsBluetoothConnected = false;

	private BluetoothDevice mDevice;

	private ProgressDialog progressDialog;

	

	
	public static byte[] REQ_DATA = {-0x78, 0x00};
	public static byte[] RESP_OK  = {0X77, 0x00};	
	public static byte[] CMD_DAY  = {0X66, 0x00};
	public static byte[] CMD_HOUR = {0X55, 0X00};
	public static byte[] CMD_MIN  = {0X44, 0X00};
	public static byte[] CMD_START= {0X33, 0X00};	

	public static byte[] CMD_FIND = {0X11, 0X00};
	public static byte[] RES_FLAG = {0x22, 0x00};
	
	public static int MOISTURE;
	public static int LIGHT;
	public static int HUMIDITY;
	public static int TEMP;
	public static int BATTERY;
	public static byte CHECKSUM;
	public static byte PUMB;
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ActivityHelper.initialize(this);

		Intent intent = getIntent();
		Bundle b = intent.getExtras();
		mDevice = b.getParcelable(Homescreen.DEVICE_EXTRA);
		mDeviceUUID = UUID.fromString(b.getString(Homescreen.DEVICE_UUID));
		mMaxChars = b.getInt(Homescreen.BUFFER_SIZE);

		Log.d(TAG, "Ready");

		mBtnDisconnect = (Button) findViewById(R.id.btnDisconnect);
		mBtnSend = (Button) findViewById(R.id.btnSend);
		mBtnClear = (Button) findViewById(R.id.btnClear);
		mTxtReceive = (TextView) findViewById(R.id.txtReceive);
		mEditSend = (EditText) findViewById(R.id.editSend);
		scrollView = (ScrollView) findViewById(R.id.viewScroll);
		chkScroll = (CheckBox) findViewById(R.id.chkScroll);
		chkReceiveText = (CheckBox) findViewById(R.id.chkReceiveText);
		mBtnClearInput = (Button) findViewById(R.id.btnClearInput);

		mTxtReceive.setMovementMethod(new ScrollingMovementMethod());

		mBtnDisconnect.setOnClickListener(new OnClickListener() 
		{

			@Override
			public void onClick(View v) 
			{
				mIsUserInitiatedDisconnect = true;
				new DisConnectBT().execute();
			}
		});

		mBtnSend.setOnClickListener(new OnClickListener() 
		{

			@Override
			public void onClick(View arg0) {
				new Sendata().execute();
			}
		});

		mBtnClear.setOnClickListener(new OnClickListener() 
		{

			@Override
			public void onClick(View arg0) {
				mEditSend.setText("");
			}
		});
		
		mBtnClearInput.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				mTxtReceive.setText("");
			}
		});

		Initial.run();
		tickRoutine.postDelayed(Routine, 60000);
		//m1Handler.postDelayed(update1TxtReceive, 500);
		

	}
	
	
	/*
	private Runnable updateTxtReceive = new Runnable () {
		int inc = 0;
		
		  @Override
		   public void run() {
			  mTxtReceive.append(String.valueOf(++inc));
			  mHandler.postDelayed(updateTxtReceive, 500);
		   }
		  
	};
	private Runnable update1TxtReceive = new Runnable () {
		  @Override
		   public void run() {
			  if (buffer[0] != 0)
			  {
			  mTxtReceive.append(String.valueOf(buffer[0]));
			  mTxtReceive.append(String.valueOf(buffer[1]));
			  }
			  
			  if (buffer[0] == 3)
			  {
				  mTxtReceive.setBackgroundColor(Color.CYAN);
				  mTxtReceive.append("OK!");
			  }
			  
			  buffer[0] = 0;
			  m1Handler.postDelayed(update1TxtReceive, 500);
		   }
		  
	};
	*/
	
	private Runnable getTime = new Runnable() {
		
	@Override
	public void run() { 
		Calendar now = Calendar.getInstance(); 

				CMD_DAY[1]  = intToByte(now.get(Calendar.DAY_OF_WEEK)); 
				CMD_HOUR[1] = intToByte(now.get(Calendar.HOUR_OF_DAY));
				CMD_MIN[1]  = intToByte(now.get(Calendar.MINUTE));

		} 
	};
	
	
	private Runnable  Initial = new Runnable() 
	{	
		private boolean res = false;
		
		private byte[] readbuffer = null;
		
		@Override
		public void run()
		{
			getTime.run();
			try
			{
				
				while(!res)
				{
					mBTSocket.getOutputStream().write(CMD_DAY);
				
					Thread.sleep(100);
				
					if (mBTSocket.getInputStream().available() > 0)
					{
					mBTSocket.getInputStream().read(readbuffer);
					}
					
					if (readbuffer == RESP_OK)
					{
						res = true;
					}

				}
				res = false;
				readbuffer = null;
				while(!res)
				{
					mBTSocket.getOutputStream().write(CMD_HOUR);
				
					Thread.sleep(100);
				
					if (mBTSocket.getInputStream().available() > 0)
					{
					mBTSocket.getInputStream().read(readbuffer);
					}
					
					if (readbuffer == RESP_OK)
					{
						res = true;
					}
				
				}
				res = false;
				readbuffer = null;
				while(!res)
				{
					mBTSocket.getOutputStream().write(CMD_MIN);
				
					Thread.sleep(100);
				
					if (mBTSocket.getInputStream().available() > 0)
					{
					mBTSocket.getInputStream().read(readbuffer);
					}
					
					if (readbuffer == RESP_OK)
					{
						res = true;
					}

				}

				readbuffer = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
							mBTSocket.getInputStream().read(databuffer);
					
							UpdateData.run();
					
							res = true;
						}

					}	
				}
					res = false;
					databuffer = null;
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
			
			MOISTURE 	= databuffer[0] << 8 + databuffer[1];
			LIGHT		= databuffer[2] << 8 + databuffer[3];
			HUMIDITY    = databuffer[4];
			TEMP 		= databuffer[5];
			BATTERY    	= databuffer[6] << 8 + databuffer[7];
			PUMB 		= databuffer[8];
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
						inputStream.read(Homescreen.buffer);
					}
					}
					Thread.sleep(500);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

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
				Toast.makeText(getApplicationContext(), "Could not connect to device. Is it a Serial device? Also check if the UUID is correct in the settings", Toast.LENGTH_LONG).show();
				finish();
			} else {
				msg("Connected to device");
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
			InputStream inputStream;
			try{
			inputStream = mBTSocket.getInputStream();
			mBTSocket.getOutputStream().write(hexStringToByteArray(mEditSend.getText().toString())[0]);
			if (inputStream.available() > 0) {
				byte[] buffer = new byte[2];
				if (inputStream.read(buffer) == 0x4f) {
					mBTSocket.getOutputStream().write(hexStringToByteArray(mEditSend.getText().toString())[1]);
				}
			}
			} catch (IOException e){
				e.printStackTrace();
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
		if (mBTSocket == null || !mIsBluetoothConnected) {
			new ConnectBT().execute();
		}
		Log.d(TAG, "Resumed");
		super.onResume();
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "Stopped");
		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}


}

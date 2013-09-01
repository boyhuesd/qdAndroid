package com.example.handl;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;

import android.widget.Button;
import android.widget.ImageView;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity {

	public TextView t;
	private Handler mHandler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		   t =new TextView(this); 
		   t=(TextView)findViewById(R.id.textView1); 
		   
		   

		   mHandler.postDelayed(runnable, 1000);
	}
	
	


	private Runnable runnable = new Runnable () {
		int i =0;
		ImageView image;
		
		  @Override
		   public void run() {
			  image = (ImageView) findViewById(R.id.imageView1);
			  
			  t.setText(String.valueOf(++i));
			  if ((i % 2) == 0)
			  {
				  image.setImageResource(R.drawable.qd);
			  }
			  else
			  {
				  image.setImageResource(R.drawable.qd2);
			  }
			  
			  mHandler.postDelayed(runnable, 1000);
		   }
		  
	};
		  
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}

	

}

/* ECE 155 - LAB 3
 * (ANNA MA 20458438)
 * (RAFIC DALATI 20526978)
 * (CHAITANYA KHANNA 20542268)
 * MARCH 7, 2014
 */

package ca.uwaterloo.lab3_203_28;

import java.util.Arrays;

import mapper.MapLoader;
import mapper.MapView;
import mapper.NavigationalMap;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


public class MainActivity extends Activity implements OnClickListener {
	
	// DECLARING GLOBAL VARIABLES
	LinearLayout ll;
	int position = 0;
	int steps = 0;
	MapView mv;
    
	float [] smoothedAccel;
	float[] mGeomagnetic;
	float[] mGravity;
	float rot[] = new float[16];
	float azimut;
	float orientation[] = new float[3];
	
	double north = 0;
	double east = 0;
	
	@Override
	public void onCreateContextMenu ( ContextMenu menu , View v, ContextMenuInfo menuInfo ) {
	super.onCreateContextMenu (menu , v, menuInfo );
	mv.onCreateContextMenu (menu , v, menuInfo );
	}
	@Override
	public boolean onContextItemSelected ( MenuItem item ) {
	return super.onContextItemSelected ( item ) || mv.onContextItemSelected ( item );
	}
      
	// VARIABLES TO SHOW SENSOR VALUES
	TextView acceltv;
	
	@Override
	public void onClick(View V){
		steps = 0;
		north = 0;
		east = 0;
	}
	// MAIN BODY CODE
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
      
        
		// CHANGES LAYOUT TO BE LINEAR
        ll = (LinearLayout) findViewById(R.id.label2);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setBackgroundColor(0xff66FF66);
		
		mv = new MapView ( getApplicationContext (), 1920 , 720 , 50, 50);
		
		registerForContextMenu (mv);
		
		// DISPLAY ACCELEROMETER'S SENSOR
		
		NavigationalMap map = MapLoader.loadMap(getExternalFilesDir(null), "Lab-room.svg");
		mv.setMap(map);
		ll.addView(mv);
		
		acceltv= new TextView(getApplicationContext());
		ll.addView(acceltv);
		
		// ------- SENSORS -------------	
		// -----------------------------
		// ACCELERATOR SENSOR
		
		SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);	
		Sensor accelSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		SensorEventListener a = new accelerometerSensorEventListener(acceltv);
		sm.registerListener(a, accelSensor, SensorManager.SENSOR_DELAY_UI);	
		
		// MAGNETIC FIELD SENSOR
		
		SensorManager magneticManager = (SensorManager) getSystemService(SENSOR_SERVICE);		
		Sensor magneticSensor = magneticManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);		
		SensorEventListener m = new MagneticSensorEventListener(acceltv);
		magneticManager.registerListener(m, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
		
		//LINEAR ACCELERATION
		
		SensorManager accelmanager = (SensorManager) getSystemService(SENSOR_SERVICE);	
		Sensor linearaccel = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		SensorEventListener c = new accelSensorEventListener(acceltv);
		sm.registerListener(c, linearaccel, accelmanager.SENSOR_DELAY_FASTEST);
		
		// CLEAR BUTTON IMPLEMENTATION
		
		Button B = new Button(getApplicationContext());
	    B.setText("Would you rather reset?");
	    ll.addView(B);
	    B.setOnClickListener(this);
	        
    }

	
    
// ---------- SENSOR CLASSES ----------- 

class MagneticSensorEventListener implements SensorEventListener {
    TextView output;
    	
    public MagneticSensorEventListener(TextView outputView){
    	output = outputView;
   	}	
    public void onAccuracyChanged(Sensor s, int i) {}
   
   	public void onSensorChanged(SensorEvent mag) {
   		if (mag.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
   			mGeomagnetic = mag.values;
    		}
   	}
}

class accelerometerSensorEventListener implements SensorEventListener {
    TextView output;
    	
    public accelerometerSensorEventListener(TextView outputView){
    	output = outputView;
   	}	
    public void onAccuracyChanged(Sensor s, int i) {}
   
   	public void onSensorChanged(SensorEvent accel) {
   		if (accel.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
   			mGravity = accel.values;
    		}
   	}
}
       
    
// ACCELEROMETER SENSOR CLASS

class accelSensorEventListener implements SensorEventListener {
	TextView output;
	
	public accelSensorEventListener(TextView outputView){
		output = outputView;
	}
	
	public void onAccuracyChanged(Sensor s, int i) {}
	
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			
			// LOW-PASS FILTER
			smoothedAccel = event.values;
			smoothedAccel[1] += (event.values[1] - smoothedAccel[1]) / 2.5;
			smoothedAccel[0] += (event.values[0] - smoothedAccel[0]) / 2.5;
			smoothedAccel[2] += (event.values[2] - smoothedAccel[2]) / 2.5;
		
			if (mGravity != null && mGeomagnetic != null) {
				
				SensorManager.getRotationMatrix(rot, null, mGravity, mGeomagnetic);
		    	
		    	SensorManager.getOrientation(rot, orientation);
		    	azimut =(float) orientation[0]; // orientation contains: azimuth, pitch and roll
		      
		    }
			// STATE MACHINE
			if (position == 0 && smoothedAccel[1] >= 0.1 && smoothedAccel[1] <= 0.2)
			{
				 position= 1;				// RISING STATE
			}
			if (position == 1 && smoothedAccel[1] >= 0.6 && smoothedAccel[1] <= 1.5){
				position = 2; 				// PEAK STATE
			}
			if (position == 2 && smoothedAccel[1] >= 0.6 && smoothedAccel[1] <= 1.2){
				position = 3;				//FALLING STATE
			}
			if (position == 3){
				steps++;
				north += Math.cos(azimut);
				east += Math.sin(azimut);
				position = 0;
			}
		}
		
			output.setText(String.format("counter: %d \nPosition: %d \ndegree: %f \nnorth: %f \neast: %f", steps, position, (azimut*180/Math.PI), north, east));
			output.setTextSize(16);
			output.setGravity(Gravity.CENTER_HORIZONTAL);
			output.setTextColor(Color.RED);
			output.setTypeface(output.getTypeface(), Typeface.BOLD);
		}
	}
}


  


    
   




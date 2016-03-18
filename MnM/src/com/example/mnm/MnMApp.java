package com.example.mnm;

/*
 * ****************************************************************************
 * Copyright 2015 Rufeng Meng <mengrufeng@gmail.com>
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 * ****************************************************************************
 */


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.View;
import android.text.*;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.*;
import android.content.BroadcastReceiver;
import android.location.*;
import android.app.*;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

@SuppressLint("NewApi")
public class MnMApp extends Activity implements SensorEventListener, OnInitListener {
	private static final String TAG = "MnMApp";
	
	private String[] m_arrsGroundTruth = {
			"N/A",
			"Mounted",
			"Not Mounted"};

	private int[] m_arrnGroundTruth = {
		Utility.UNDECIDED,
		Utility.MOUNT,
		Utility.NONMOUNT};
	
	private static boolean m_blnRecordStatus = false; // true: Recording; false: Stopped 
	
	//Sensor data type
	private static final int DATA_TYPE_SENSOR = 1;
	private static final int DATA_TYPE_GPS = 4;
	
	//Sensor event type
	private static int SENSOR_EVENT_NULL = 0;
	private static int SENSOR_EVENT_ACCL = 1;
	private static int SENSOR_EVENT_LINEAR_ACCL = 2;
	private static int SENSOR_EVENT_GYRO = 3;
	private static int SENSOR_EVENT_GPS = 10;
	private static int SENSOR_EVENT_GRAVITY = 12;
		
	private SensorManager m_smMnM = null;
			
	/* Sensor is available or not */
	private static boolean m_blnGyroPresent = false;
	private static boolean m_blnAcclPresent = false;
	private static boolean m_blnLinearAcclPresent = false;
	private static boolean m_blnGravityPresent = false;
	
	/* Sensor is selected or not */
	private static boolean m_blnGyroEnabled = false;
	private static boolean m_blnAcclEnabled = false;
	private static boolean m_blnLinearAcclEnabled = false;
	private static boolean m_blnGravityEnabled = false;
	

	/* Default delay mode for sensors */
	private static int m_nAcclMode = SensorManager.SENSOR_DELAY_FASTEST;
	private static int m_nLinearAcclMode = SensorManager.SENSOR_DELAY_FASTEST;
	private static int m_nGravityMode = SensorManager.SENSOR_DELAY_FASTEST;
	private static int m_nGyroMode = SensorManager.SENSOR_DELAY_FASTEST;
	
	private Location m_location;
			
	private MnMApp m_actHome = this;
	private ResolveInfo m_riHome;
	
	private boolean m_blnGPSSignalEnabled = false; // true: GPS signal is enabled
	
	private LocationManager m_locManager = null;
	private String m_sGPSProvider = LocationManager.GPS_PROVIDER; //GPS provider

	private static String m_sSensorAccuracy=",,";
			
	private String sDetectionTimeFile = "DetectionTime.csv";   //File to record time of detection
	private String m_sDetectionTimePathFile = "";
	private FileWriter m_fwDetectionTime = null;

	private String sDetectionResultFile = "DetectionResult";   //File to record time of detection
	private String m_sDetectionResultPathFile = "";
	private FileWriter m_fwDetectionResult = null;
	
	private Spinner m_spnGroundTruth;
	private ArrayAdapter<String> m_adpGroundTruth;
	private int m_nGroundTruthIndex = Utility.UNDECIDED;
	private static int m_nDetecedMountCnt = 0;
	private static int m_nDetectedNonmountCnt = 0;
	private static int m_nTotalDetectionCnt = 0;

	private CheckBox m_chkNoVoice;

	private Button m_btnStartStop;
	private TextView m_tvShowInfo;

	//For Sensor working after screen off
	private WakeLock m_wakeLock;
	
	private List<GyroData> m_lstGyro = new ArrayList<GyroData>();
	private static double m_fGpsSpeed = Utility.INVALID_SPEED;
	private static boolean m_blnDetecting = false;
	
	private static long m_lDetectionStartTime = 0;   //in Nanosecond
	private static long m_lDetectionEndTime = 0;   //in Nanosecond
		
    private static final int REQ_TTS_STATUS_CHECK = 0;  
    private TextToSpeech mTts = null;  

    
	/* Class for Mount/Nonmount detection task */
	private class ClassifyMnM extends AsyncTask<Void, Void, Void> {
		
		private List<GyroData> m_lstGyroMnM = new ArrayList<GyroData>();
		private int m_nMnMResult = Utility.UNDECIDED;
					
		public ClassifyMnM(List<GyroData> lstGyro) {
			m_lstGyroMnM = lstGyro;
		}
					
		@Override
		protected Void doInBackground(Void...params) {
			// TODO Auto-generated method stub
			double fDetectionTime = 0;
			
			MnMDetector mnmDet = new MnMDetector();
			m_nMnMResult = mnmDet.detect(m_lstGyroMnM);    //Detect Mount/Nonmount
						
			m_lDetectionEndTime = System.nanoTime();
			
			//Save time of one detection into file
			if (m_lDetectionStartTime > 0) {
				fDetectionTime = Utility.calTimeDiff(m_lDetectionStartTime, m_lDetectionEndTime);
				
				try {
					m_fwDetectionTime = new FileWriter(m_sDetectionTimePathFile, true);  //Append

					String sLine = "" + fDetectionTime + "\n";
					m_fwDetectionTime.write(sLine);
					m_fwDetectionTime.close();
				} catch (Exception e) {
					 
				}
				
			}
			
			m_nTotalDetectionCnt = m_nTotalDetectionCnt + 1;
			
			if (m_nMnMResult == Utility.MOUNT) {
				m_nDetecedMountCnt = m_nDetecedMountCnt + 1;
			} else if (m_nMnMResult == Utility.NONMOUNT) {
				m_nDetectedNonmountCnt = m_nDetectedNonmountCnt + 1;
			}
					
			return null;
		}
		
		protected void onProgressUpdate(Void... params) {

		}	
		
		protected void onPostExecute(Void result) {
			refreshResultGUI(m_nMnMResult);
			
			m_lstGyro.clear();

			m_blnDetecting = false;
		}

	}
	
	private void refreshResultGUI(int nMnMResult) {
		if (nMnMResult == Utility.MOUNT) {
			m_tvShowInfo.setText("MOUNTED");
			if ((mTts != null) && (m_chkNoVoice.isChecked() == false)) {
				mTts.speak("Mounted", TextToSpeech.QUEUE_ADD, null);  
			}
		} else if (nMnMResult == Utility.NONMOUNT) {
			m_tvShowInfo.setText("NOT MOUNTED");
			if ((mTts != null) && (m_chkNoVoice.isChecked() == false)) {
				mTts.speak("Not Mounted", TextToSpeech.QUEUE_ADD, null);  
			}
		} else {
			m_tvShowInfo.setText("Undecided");
		}
		
		//Save detection result
		try {
			m_fwDetectionResult = new FileWriter(m_sDetectionResultPathFile, true);  //Append

			m_nGroundTruthIndex = m_spnGroundTruth.getSelectedItemPosition();

			String sLine = m_arrnGroundTruth[m_nGroundTruthIndex] + "," + nMnMResult + "\n";
			m_fwDetectionResult.write(sLine);
			m_fwDetectionResult.close();
		} catch (Exception e) {
			 
		}
		
	}
	
	//When the power button is pushed, screen goes off, to still keep the service running, some need to be re-enabled
    public BroadcastReceiver m_ScreenOffReceiver = new BroadcastReceiver() {
    	
    	public void onReceive(Context context, Intent intent) {
    		
    		if (!intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
    			return;
    		}
    		    		
    		if (m_blnRecordStatus == true) {
    			    			
     			if (m_blnGPSSignalEnabled == true) {
     				if (m_locManager != null) {
     					m_locManager.removeUpdates(m_locListener);
     					m_locManager.requestLocationUpdates(m_sGPSProvider, 0L, 0.0f, m_locListener);
     				}
     			}
	
    		}
    			
    	}
    	
    };
    
    	
	private void resetValues() {
		m_sSensorAccuracy=",,";
		
		m_nDetecedMountCnt = 0;
		m_nDetectedNonmountCnt = 0;
		m_nTotalDetectionCnt = 0;

		m_lstGyro.clear();
		m_blnDetecting = false;
		
		m_lDetectionStartTime = 0;   //in Nanosecond
		m_lDetectionEndTime = 0;   //in Nanosecond	
	}
		
	
	/* Set default widget status and setting */
    private void setDefaultStatus() {
    	m_blnGyroEnabled = false;
    	m_blnAcclEnabled = false;
    	m_blnLinearAcclEnabled = false;
    	m_blnGravityEnabled = false;
    	
    	m_nGyroMode = SensorManager.SENSOR_DELAY_FASTEST;
    	m_nAcclMode = SensorManager.SENSOR_DELAY_FASTEST;
    	m_nLinearAcclMode = SensorManager.SENSOR_DELAY_FASTEST;
    	m_nGravityMode = SensorManager.SENSOR_DELAY_FASTEST;
    	
    	m_blnRecordStatus = false;
    }
	    
    
     /* Get GPS provider */
    private boolean getGPSProvider() {
    	Location location = null;
 		Criteria crit = new Criteria();
 		float fLat,fLng,fAlt;

		m_sGPSProvider = m_locManager.getBestProvider(crit, true); //false?
		if (m_sGPSProvider != null) {
			m_blnGPSSignalEnabled = true;
			location = m_locManager.getLastKnownLocation(m_sGPSProvider);
			if (location != null ) {
				fLat = (float)(location.getLatitude());
				fLng = (float)(location.getLongitude());
				if (location.hasAltitude()) {
					fAlt = (float)(location.getAltitude());
				}
			}
			return true;
		} else {
			return false;
		} 
    }
     
    private LocationListener m_locListener = new LocationListener() {
    	public void onLocationChanged(Location location) {
    		if (location != null) {    		
    			recordLocation(location);
    		}
    	}

    	public void onProviderDisabled(String provider) {    		 
    		if (provider.equals(m_sGPSProvider)) {
    			m_blnGPSSignalEnabled = false;
    		}
    	}
    	 
    	public void onProviderEnabled(String provider) {
    		if (provider.equals(m_sGPSProvider)) {
    			m_blnGPSSignalEnabled = true;
    			if (m_blnRecordStatus == false) {
    			}
    		} 
     	}
    	 
    	public void onStatusChanged(String provider, int status, Bundle extras) {
    		if (provider.equals(m_sGPSProvider)) {
    			if (status == LocationProvider.OUT_OF_SERVICE) {
    				m_blnGPSSignalEnabled = false;
    			} else {
    				m_blnGPSSignalEnabled = true;
    				if (m_blnRecordStatus == false) {
    				}
    			}
    		}	 
    	}
    	 
    };
               
     
    private void UpdateSensorServiceRegistration(boolean blnEnabled) {
		//Register sensors according to the enable/disable status

	 	if (blnEnabled) {
	 		m_smMnM.registerListener(m_actHome, m_smMnM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), m_nAcclMode);
	 		m_smMnM.registerListener(m_actHome, m_smMnM.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), m_nLinearAcclMode);
	 		m_smMnM.registerListener(m_actHome, m_smMnM.getDefaultSensor(Sensor.TYPE_GRAVITY), m_nGravityMode);
	 		m_smMnM.registerListener(m_actHome, m_smMnM.getDefaultSensor(Sensor.TYPE_GYROSCOPE), m_nGyroMode);	 		
	 	} else {
	 		m_smMnM.unregisterListener(m_actHome, m_smMnM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
	 		m_smMnM.unregisterListener(m_actHome, m_smMnM.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
	 		m_smMnM.unregisterListener(m_actHome, m_smMnM.getDefaultSensor(Sensor.TYPE_GRAVITY));
	 		m_smMnM.unregisterListener(m_actHome, m_smMnM.getDefaultSensor(Sensor.TYPE_GYROSCOPE));

	 	}	 	 				    	 
    }
     
         
	/* Event listener for Start/Stop button */
	private Button.OnClickListener m_btnStartStopRecordListener = new Button.OnClickListener() {
		public void onClick(View v) {
			int nGroundTruth;
		    Date dtFileStart;
			final String DATE_FORMAT = "yyyyMMddHHmmss";
			SimpleDateFormat spdCurrentTime = new SimpleDateFormat(DATE_FORMAT);
			String sStartTime = "";
			boolean blnResult = false;

			if (m_blnRecordStatus == false) {  // Start

				UpdateSensorServiceRegistration(true);
				
				if (m_blnGPSSignalEnabled == true) {
					m_locManager.requestLocationUpdates(m_sGPSProvider, 0L, 0.0f, m_locListener);
				}

				dtFileStart = new Date();
				sStartTime = spdCurrentTime.format(dtFileStart);
				
				m_sDetectionResultPathFile = Environment.getExternalStorageDirectory().getAbsolutePath() + 
						File.separator + getString(R.string.app_folder) + File.separator + sDetectionResultFile + sStartTime + ".csv";
				
				m_btnStartStop.setText(getString(R.string.btn_stop));
				
				m_tvShowInfo.setText("Detecting...");
				
				m_blnRecordStatus = true;

				m_spnGroundTruth.setEnabled(false);
				m_chkNoVoice.setEnabled(false);
				
				m_wakeLock.acquire();

			} else {    //Stop

				UpdateSensorServiceRegistration(false);

				if (m_blnGPSSignalEnabled) {
					m_locManager.removeUpdates(m_locListener);
				}
								
				m_btnStartStop.setText(getString(R.string.btn_start));
				
				m_spnGroundTruth.setEnabled(true);
				m_chkNoVoice.setEnabled(true);
	
				m_blnRecordStatus = false;

				m_nGroundTruthIndex = m_spnGroundTruth.getSelectedItemPosition();
				
				nGroundTruth = m_arrnGroundTruth[m_nGroundTruthIndex];
				
				blnResult = show_detection_stat(nGroundTruth);
				if (blnResult == false) {
					m_tvShowInfo.setText("");
				}
				
				m_fwDetectionResult = null;
				m_fwDetectionTime = null;
								
				m_wakeLock.release();

				resetValues();
			}
		}
	};     

     
	public boolean show_detection_stat(int nGroundTruth) {
		double fAccuracy = 0;
		String sInfo = "";
		String sPattern = "##.##";
		DecimalFormat decimalFormat = new DecimalFormat(sPattern);

		String sFormatedAccuracy;
		
		if (m_nTotalDetectionCnt == 0) {
			return false;
		}
		
		if (nGroundTruth == Utility.MOUNT) {
			fAccuracy = m_nDetecedMountCnt*100.0/m_nTotalDetectionCnt;
			sFormatedAccuracy = decimalFormat.format(fAccuracy);
			sInfo = "Accuracy: " + m_nDetecedMountCnt + "/" + m_nTotalDetectionCnt + " = " + sFormatedAccuracy + "%";
		} else if (nGroundTruth == Utility.NONMOUNT) {
			fAccuracy = m_nDetectedNonmountCnt*100.0/m_nTotalDetectionCnt;
			sFormatedAccuracy = decimalFormat.format(fAccuracy);
			sInfo = "Accuracy: " + m_nDetectedNonmountCnt + "/" + m_nTotalDetectionCnt + " = " + sFormatedAccuracy + "%";			
		} else  {
			sInfo = "Mounted/Not-Mounted = " + m_nDetecedMountCnt + "/" + m_nDetectedNonmountCnt;
		}
		
		m_tvShowInfo.setText(sInfo);
		return true;
	}
	
     
	/* Check the availability of sensors, disable relative widgets */
	private void checkSensorAvailability() {

		List<Sensor> lstSensor = m_smMnM.getSensorList(Sensor.TYPE_GYROSCOPE);
		if (lstSensor.size() > 0) {
			m_blnGyroPresent = true;
		} else {
			m_blnGyroPresent = false;
			m_blnGyroEnabled = false;
		}

		lstSensor = m_smMnM.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (lstSensor.size() > 0) {
			m_blnAcclPresent = true;
		} else {
			m_blnAcclPresent = false;
			m_blnAcclEnabled = false;
		}

		lstSensor = m_smMnM.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
		if (lstSensor.size() > 0) {
			m_blnLinearAcclPresent = true;
		} else {
			m_blnLinearAcclPresent = false;
			m_blnLinearAcclEnabled = false;
		}

		lstSensor = m_smMnM.getSensorList(Sensor.TYPE_GRAVITY);
		if (lstSensor.size() > 0) {
			m_blnGravityPresent = true;
		} else {
			m_blnGravityPresent = false;
			m_blnGravityEnabled = false;
		}
		
	}    

	      
    public void show_main_window() {
    	setContentView(R.layout.main);
    	
        m_spnGroundTruth = (Spinner)findViewById(R.id.spnGroundTruth);
        m_adpGroundTruth = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, m_arrsGroundTruth);
        m_adpGroundTruth.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        m_spnGroundTruth.setAdapter(m_adpGroundTruth);
        m_spnGroundTruth.setSelection(m_nGroundTruthIndex);

    	m_chkNoVoice = (CheckBox)findViewById(R.id.chkNoVoice);   
        
        m_tvShowInfo = (TextView)findViewById(R.id.tvInfo);  
        m_tvShowInfo.setText("");

        m_btnStartStop = (Button)findViewById(R.id.btnStart);
        m_btnStartStop.setOnClickListener(m_btnStartStopRecordListener);
        
    }

    
    //This function prepare the existing project names/usernames/labels
    private void preconfigSetting() {
    	String sAppFolder = "";
    	File flFolder;
    	
		// Check whether SD Card has been plugged in
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			sAppFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + 
									File.separator + getString(R.string.app_folder);
			flFolder = new File(sAppFolder);
			//Check whether /mnt/sdcard/Sensing/ exists
			if (!flFolder.exists()) {
				//Does not exist, create it
				if (flFolder.mkdir()) {

				} else {
					//Failed to create
					Toast.makeText(getApplicationContext(), "SD Card is not accessible!", Toast.LENGTH_SHORT).show();
					return;
				}
			} 
						
			m_sDetectionTimePathFile = Environment.getExternalStorageDirectory().getAbsolutePath() + 
					File.separator + getString(R.string.app_folder) + File.separator + sDetectionTimeFile;

			
		} else {        				
			//NO SD Card
			Toast.makeText(getApplicationContext(), "Please insert SD Card!", Toast.LENGTH_SHORT).show();
			return;
		}
    	
    }
    
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	int i;
    	Location location = null;
     	
    	super.onCreate(savedInstanceState);
    	   	
    	if (android.os.Build.VERSION.SDK_INT > 9) {
    	    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    	    StrictMode.setThreadPolicy(policy);
    	}
    	
    	try {
            Class.forName("android.os.AsyncTask");
    	} catch (ClassNotFoundException e) {
            e.printStackTrace();
    	}
    	
    	m_smMnM = (SensorManager) getSystemService(SENSOR_SERVICE);
    	
        PackageManager pm = getPackageManager();
        m_riHome = pm.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0);
        
        checkSensorAvailability();
        
    	//Get Existing Project Name and Existing User Name
    	preconfigSetting();
    	
    	/* When the power button is pressed and the screen goes off, the sensors will stop work by default,
    	 * Here keep the CPU on to keep sensor alive and also use SCREEN_OFF notification to re-enable GPS/WiFi
    	 */
    	PowerManager pwrManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
    	m_wakeLock = pwrManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
    	IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
    	filter.addAction(Intent.ACTION_SCREEN_OFF);
    	
    	registerReceiver(m_ScreenOffReceiver,filter);
    	
    	m_locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		m_blnGPSSignalEnabled = m_locManager.isProviderEnabled(m_sGPSProvider);
				
		if ((m_locManager != null) && (m_blnGPSSignalEnabled == true)) {
			location = m_locManager.getLastKnownLocation(m_sGPSProvider);
			if (location != null) {
				float fLat = (float)(location.getLatitude());
				float fLng = (float)(location.getLongitude());
				if (location.hasAltitude()) {
					float fAlt = (float)(location.getAltitude());
				}
			}
		}

    	show_main_window();	
    	
        Intent checkIntent = new Intent();  
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);  
        startActivityForResult(checkIntent, REQ_TTS_STATUS_CHECK);  

    }
    
    @Override  
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
        super.onActivityResult(requestCode, resultCode, data);  
        if(requestCode == REQ_TTS_STATUS_CHECK){  
            switch(resultCode){  
                case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS:  
                    mTts = new TextToSpeech(this, this);  
                    break;  
                case TextToSpeech.Engine.CHECK_VOICE_DATA_BAD_DATA://需要的语音数据已损坏  
                case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_DATA://缺少需要语言的语音数据  
                case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_VOLUME://缺少需要语言的发音数据  
                    Intent dataIntent = new Intent();  
                    dataIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);  
                    startActivity(dataIntent);  
                    break;  
                case TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL://检查失败   
                default:break;  
            }  
        }   
    }  
   
    
    public void startActivitySafely(Intent intent) {
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	try {
    		startActivity(intent);
    	} catch (ActivityNotFoundException e) {
    		Toast.makeText(this, "unable to open", Toast.LENGTH_SHORT).show();
    	} catch (SecurityException e) {
    		Toast.makeText(this, "unable to open", Toast.LENGTH_SHORT).show();
    	}
    	
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		//Show MAIN app without finishing current activity
    		ActivityInfo ai = m_riHome.activityInfo;
    		Intent startIntent = new Intent(Intent.ACTION_MAIN);
    		startIntent.addCategory(Intent.CATEGORY_LAUNCHER);
    		startIntent.setComponent(new ComponentName(ai.packageName,ai.name));
    		startActivitySafely(startIntent);
    		return true;
    	} else {
    		return super.onKeyDown(keyCode, event);
    	}
    }
    
    protected void onResume() {
    	super.onResume();
    }
        
    protected void onStop() {
    	super.onStop();    	
    }

    
    //Record GPS Readings
    public void recordLocation(Location location) {
    	SensorData senData = new SensorData(DATA_TYPE_GPS, location);
    	recordSensingInfo(senData);
    }
    
    //Record Sensor readings
    public void onSensorChanged(SensorEvent event) {
    	SensorData senData = new SensorData(DATA_TYPE_SENSOR, event);
    	recordSensingInfo(senData);    	
    }
    
    //Organize the sensing information and save into CSV file
    public void recordSensingInfo(SensorData senData) {
    	String sRecordLine;
    	String sTimeField;
    	Date dtCurDate;
    	int i;
    	long lStartTime = 0;
    	long lCurrentTime = 0;
		SimpleDateFormat spdRecordTime,spdCurDateTime;
        final String DATE_FORMAT = "yyyyMMddHHmmss";
		final String DATE_FORMAT_S = "yyMMddHHmmssSSS"; //"yyyyMMddHHmmssSSS"
		int nSensorReadingType = SENSOR_EVENT_NULL; 
		int nSensorDataType;
		
        if (m_blnRecordStatus == false) { //Stopped
        	return;
        }
        
        if (m_blnDetecting == true) {  //Detecing it is mount or nonmount
        	return;
        }

        dtCurDate = new Date();
		        
		// Timestamp for the record
        spdRecordTime = new SimpleDateFormat(DATE_FORMAT_S);
		sTimeField = spdRecordTime.format(dtCurDate);
				
		nSensorDataType = senData.getSensorDataType();
		
		if (nSensorDataType == DATA_TYPE_SENSOR) {
			SensorEvent event;
			
			event = senData.getSensorEvent();
			
			synchronized(this) {
	    		switch (event.sensor.getType()){
				    			
		    		case Sensor.TYPE_ACCELEROMETER:
		    			//X, Y, Z
		    			if (m_blnAcclEnabled) {
		    			
		    				nSensorReadingType = SENSOR_EVENT_ACCL;
		    			}
		    			
		    			break;
		    			
		    		case Sensor.TYPE_LINEAR_ACCELERATION:
		    			//X,Y,Z
		    			if (m_blnLinearAcclEnabled) {
		    			
		    				nSensorReadingType = SENSOR_EVENT_LINEAR_ACCL;
		    			}
		    			
		    			break;
		    			
		    		case Sensor.TYPE_GRAVITY:
		    			//X,Y,Z
		    			if (m_blnGravityEnabled) {
		    				
		    				nSensorReadingType = SENSOR_EVENT_GRAVITY;
		    			}
		    			
	    				break;

		    		case Sensor.TYPE_GYROSCOPE:
		    			//X,Y,Z		    			
		    			nSensorReadingType = SENSOR_EVENT_GYRO;
		    			lCurrentTime = System.nanoTime();
		    			GyroData gyro = new GyroData();
		    			gyro.setGyroData(lCurrentTime, event.values[0], event.values[1], event.values[2], m_fGpsSpeed);
		    			
		    			m_lstGyro.add(gyro);
		    			
		    			double fTimeDiff = Utility.calTimeDiff(m_lstGyro.get(0).getTimestamp(), lCurrentTime);
		    			
		    			if (fTimeDiff >= Utility.DATA_DURATION && m_lstGyro.size() % Utility.DURATION_DATA_SIZE == 0) {
		    				 m_blnDetecting = true;
		    				 m_tvShowInfo.setText("Detecting...");
		    				 m_lDetectionStartTime = System.nanoTime();
		    	    		 new ClassifyMnM(m_lstGyro).execute();
		    			}
		    			
		    			break;
		    					    			
	    		}
	    	}
		} else if (nSensorDataType == DATA_TYPE_GPS){
			Location locationGps;
			locationGps = senData.getGpsLocation();
			
			if (locationGps != null) {

				m_location = new Location(locationGps);
				
				if (locationGps.hasSpeed()) {
					m_fGpsSpeed = locationGps.getSpeed();
				} else {
					m_fGpsSpeed = Utility.INVALID_SPEED;
				}
				
				nSensorReadingType = SENSOR_EVENT_GPS;
			}
		} 
				
    }
    
     
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    	switch(sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				m_sSensorAccuracy = "1,"; //Accl
				break;
	
			case Sensor.TYPE_LINEAR_ACCELERATION:
				m_sSensorAccuracy = "2,"; //LinearAccl
				break;
	
	    	case Sensor.TYPE_GYROSCOPE:
    			m_sSensorAccuracy = "3,"; //Gyro
    			break;

    		case Sensor.TYPE_GRAVITY:
    			m_sSensorAccuracy = "5,"; //Gravity		
    			break;
    			
    		default:
    			m_sSensorAccuracy = "11,"; //Other
    	}
    	
    	switch (accuracy) {
    		case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
    			m_sSensorAccuracy = m_sSensorAccuracy + "1,"; //H
    			break;

    		case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
    			m_sSensorAccuracy = m_sSensorAccuracy + "2,"; //M
    			break;
    			
    		case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
    			m_sSensorAccuracy = m_sSensorAccuracy + "3,"; //L
    			break;
    			
    		case SensorManager.SENSOR_STATUS_UNRELIABLE:
    			m_sSensorAccuracy = m_sSensorAccuracy + "4,"; //U
    			break;
    	}
    }


    @Override  
    public void onInit(int status) {  
        if(status == TextToSpeech.SUCCESS){  
            int result = mTts.setLanguage(Locale.US);  
            if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){   
                Toast.makeText(this, "Lang not supported", Toast.LENGTH_LONG).show();  
            }else{  
                //mTts.speak(m_etInputText.getText().toString(), TextToSpeech.QUEUE_ADD, null);  
            }  
        }   
    }        
   
    @Override  
    protected void onDestroy() {  
        super.onDestroy();  
        mTts.shutdown();  
    }  
  
  
    @Override  
    protected void onPause() {  
        super.onPause();  
        if(mTts != null)mTts.stop();  
    }  
    
}

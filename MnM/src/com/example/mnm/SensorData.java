package com.example.mnm;

import java.util.List;

import android.hardware.SensorEvent;
import android.location.*;

public class SensorData {
	private int m_nSensorDataType;	//Sensor date type
	
	private Location m_locGps;		//GPS location information
	private SensorEvent m_senEvt; 	//Sensor Event
	
	public SensorData(int nSensorDataType, SensorEvent senEvt) {
		// TODO Auto-generated constructor stub
		m_nSensorDataType = nSensorDataType;
		m_senEvt = senEvt;
	}
	
	public SensorData(int nSensorDataType, Location locGps) {
		m_nSensorDataType = nSensorDataType;
		m_locGps = locGps;
	}
			
	public int getSensorDataType() {
		return m_nSensorDataType;
	}
	
	public SensorEvent getSensorEvent() {
		return m_senEvt;
	}
	
	public Location getGpsLocation() {
		return m_locGps;
	}

}

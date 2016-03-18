package com.example.mnm;

public class GyroData {
 
	private long m_lTimestamp;  //Nano seconds
	private double m_fGyroX;
	private double m_fGyroY;
	private double m_fGyroZ;
	private double m_fGpsSpeed;
	
	public GyroData() {
		// TODO Auto-generated constructor stub
	}
	
	public void setGyroData(long lTimestamp, double fGyroX, double fGyroY, double fGyroZ, double fGpsSpeed) {
		m_lTimestamp = lTimestamp;
		m_fGyroX = fGyroX;
		m_fGyroY = fGyroY;
		m_fGyroZ = fGyroZ;
		m_fGpsSpeed = fGpsSpeed;
	}
	
	public long getTimestamp() {
		return m_lTimestamp;
	}
	
	public double getGyroX() {
		return m_fGyroX;
	}
	
	public double getGyroY() {
		return m_fGyroY;
	}
	
	public double getGyroZ() {
		return m_fGyroZ;
	}
	
	public double getGpsSpeed() {
		return m_fGpsSpeed;
	}

}

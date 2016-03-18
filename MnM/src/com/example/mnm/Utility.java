package com.example.mnm;

import java.util.ArrayList;
import java.util.List;

public class Utility {
	public static final int INVALID_SPEED = 9999;
	public static final int DATA_DURATION = 10;  //seconds
	public static final int SEGMENT_SIZE = 256;  //Each segment has that many samples
	public static final int SEGMENT_STEP = SEGMENT_SIZE/2;
	public static final int DURATION_DATA_SIZE = SEGMENT_SIZE*DATA_DURATION/2;  //Detect every 1280 or 2560 samples depends on the sample rate
	
	public static final int UNDECIDED = 0;
	public static final int MOUNT = 1;
	public static final int NONMOUNT = 2;
	public static final int CUPHOLDER = 3;
	public static final int POCKET = 4;
	
	public static final int UNKNOWN = 0;
	public static final int HIGHWAY = 1;
	public static final int URBAN = 2;
	public static final double STOP_GPS_RATIO = 0.25;
	public static final double SPEED_THRESHOLD = 26.8;   // m/s = 60 mph
		
	private static final double STD_LINEARACCL_MAGTHRESHOLD_STOP = 0.1;
	private static final double STD_GYRO_MSGTHRESHOLD_STOP = 0.005;
	
	public static final double MNM_RATIO = 0.75;
	
	public Utility() {
		// TODO Auto-generated constructor stub
	}
	
	//Calcualte sample standard deviation
	public static double calStd(List<Double> lstfValue) {
        if (lstfValue.size() == 0) return Double.NaN;
        double avg = calMean(lstfValue);
        double sum = 0.0;
        double fValue;
        double fStd = 0.0;
        
        for (int i = 0; i < lstfValue.size(); i++) {
        	fValue = lstfValue.get(i).doubleValue();
            sum += (fValue - avg) * (fValue - avg);
        }
        
        fStd = sum / (lstfValue.size() - 1);
        
        fStd = Math.sqrt(fStd);
        
        return fStd;
    }	

	
	//Return time in second
	public static double calTimeDiff(long lFromNano, long lEndNano) {
		double fTimeDiff = 0;
		
		long lDifference = lEndNano - lFromNano;
		
		fTimeDiff = lDifference*1.0/1e9;
		
		return fTimeDiff;
	}
	
	public static double calMean(List<Double> lstValue) {
		double fMean = 0;
		double fTotal = 0;
		
		for (Double fValue:lstValue) {
			fTotal = fTotal + fValue;
		}
		
		fMean = fTotal/lstValue.size();
		
		return fMean;
	}

	
	public static double calMean(List<Double> lstValue, int nStartIdx, int nEndIdx) {
		double fMean = 0;
		double fTotal = 0;
		double fValue = 0;
		int i;
		
		for (i=nStartIdx; i<=nEndIdx; i++) {
			fValue = lstValue.get(i).doubleValue();
			fTotal = fTotal + fValue;
		}
		
		fMean = fTotal/(nEndIdx-nStartIdx+1);
		
		return fMean;
	}
	
			
	public static List<Double> MovingAverage(List<Double> lstValue, int nWinSize) {   //nWinSize should be odd
		int i, j;
		int nTotalCnt = lstValue.size();
		List<Double> lstFiltered = new ArrayList<Double>();
		int nHalfWinSize = (nWinSize-1)/2;
		double fAverage = 0;
		double fWindowTotal = 0;
		
		if (nWinSize == 0) return lstValue;
		
		for (i=0; i< nTotalCnt; i++) {
			if (i < nHalfWinSize) {
				lstFiltered.add(lstValue.get(i));
			} else if (nTotalCnt-1-i < nHalfWinSize) {
				lstFiltered.add(lstValue.get(i));				
			} else {
				fWindowTotal = 0;
				for (j=i-nHalfWinSize; j<=i+nHalfWinSize; j++) {
					fWindowTotal = fWindowTotal + lstValue.get(j);
				}
				
				fAverage = fWindowTotal/nWinSize;
				lstFiltered.add(fAverage);
			}
		}
		
		return lstFiltered;
	}
	
	
	public static void DEBUG(String sInfo) {
		//System.out.println("##################" + sInfo + "#####################");
	}
	
}



package com.example.mnm;

/*
 * This class is used to detect phone position, i.e. Mounted or Not Mounted
 * 
 * The detection is based on David's method
 * 
 */

import java.util.ArrayList;
import java.util.List;

public class MnMDetector {

	private static final double POSITION_RATIO_THRESHOLD = 0.75;   //For cupholder and pocket, if the corresponding condition is true for this ratio, = Non-mount
	
	public MnMDetector() {
		// TODO Auto-generated constructor stub
	}
	
	//Find the start/end position of buckets
	//Buckets are:  0~5 Hz;  5~15 Hz, 15~25 Hz, 30~40 Hz  (25~30 Hz is not used)
	private List<Integer> findBucketRange(List<Double> lstfFreq) {
		List<Integer> lstBucketRange = new ArrayList<Integer>();
		int nTotalLen = lstfFreq.size();
		int i;
		int nNewIndex = 0;
		int nIndex = 0;
		
		lstBucketRange.add(0);    // 0 Hz
		
		for (i=nIndex+1; i<nTotalLen; i++) {
			if (lstfFreq.get(i).doubleValue() <= 5) {
				nNewIndex = i;
			} else {
				break;
			}
		}
		
		lstBucketRange.add(nNewIndex);   // 5 Hz
		
		nIndex = nNewIndex;
		
		for (i=nIndex+1; i<nTotalLen; i++) {
			if (lstfFreq.get(i).doubleValue() <= 15) {
				nNewIndex = i;
			} else {
				break;
			}
		}
		
		lstBucketRange.add(nNewIndex);  // 15 Hz
		
		nIndex = nNewIndex;
		
		for (i=nIndex+1; i<nTotalLen; i++) {
			if (lstfFreq.get(i).doubleValue() <= 25) {
				nNewIndex = i;
			} else {
				break;
			}
		}
		
		lstBucketRange.add(nNewIndex);  // 25 Hz
		
		nIndex = nNewIndex;
		
		for (i=nIndex+1; i<nTotalLen; i++) {
			if (lstfFreq.get(i).doubleValue() <= 30) {
				nNewIndex = i;
			} else {
				break;
			}
		}
		
		lstBucketRange.add(nNewIndex);   // 30 Hz
		
		nIndex = nNewIndex;
		
		for (i=nIndex+1; i<nTotalLen; i++) {
			if (lstfFreq.get(i).doubleValue() <= 40) {
				nNewIndex = i;
			} else {
				break;
			}
		}
		
		lstBucketRange.add(nNewIndex);    // 40 Hz
		
		return lstBucketRange;
	}
	
	
	// Based on the features to decide the phone is mounted or non-mounted
	private int meansureMnM(double fFeatureBucket1, double fFeatureBucket2, double fFeatureBucket3, double fFeatureBucket5, 
			                                                      double fVarX, double fVarY, double fVarZ,  int nRoadArea) {
		  int nMnMResult = Utility.UNDECIDED;
		  double fVarRatio = 0;
		  double fVarXY = Math.sqrt(Math.pow(fVarX, 2) + Math.pow(fVarY, 2));
		  
		  if (fVarXY == 0) {
			 if (fVarZ == 0) {
				 fVarRatio = 0;
			 } else {
				 fVarRatio = 9999;
			 }
		  } else {
			  fVarRatio = fVarZ/fVarXY;
		  }
		  
		  if (nRoadArea == Utility.URBAN) {
			  
			  if (fFeatureBucket1 < 0.1 && fFeatureBucket2 < 0.01 && fFeatureBucket3 < 0.02 && fFeatureBucket5 < 0.0075 && fVarRatio < 0.001) {
				  nMnMResult = Utility.CUPHOLDER;  // Cup Holder
			  } else if (fFeatureBucket1 * 0.5 > fFeatureBucket3 && fFeatureBucket2 < fFeatureBucket1) {
				  nMnMResult = Utility.POCKET;  // Pocket 
			  } else {
				  nMnMResult = Utility.MOUNT;  // Mount
			  }
			    
		  } else {
			  if (fFeatureBucket1 < 0.1 && fFeatureBucket2 < 0.01 && fFeatureBucket3 < 0.02 && fFeatureBucket5 < 0.0075 && fVarRatio > 0.2 && fVarY < 0.001 && fVarZ < 0.001) {
				  nMnMResult = Utility.CUPHOLDER;  // Cup Holder
			  } else if (fFeatureBucket1 * 0.5 + 0.01 > fFeatureBucket3 && fVarY > fVarX) {
				  nMnMResult = Utility.POCKET;  // Pocket 
			  } else {
				  nMnMResult = Utility.MOUNT;  // Mount
			  }
			  
		  }
		  
		  return nMnMResult;
	}
	
	
	//Detect phone position for a segment data
	private int detectMnM(List<GyroData> lstGyro, int nSegStartIdx, int nSegEndIdx, int nRoadArea) {
		int nMnMResult = Utility.UNDECIDED;
		int i;
		
		List<Double> lstfSegGyroX = new ArrayList<Double>();
		List<Double> lstfSegGyroY = new ArrayList<Double>();
		List<Double> lstfSegGyroZ = new ArrayList<Double>();

		List<Double> lstfSegGyroSingleX = new ArrayList<Double>();
		List<Double> lstfSegGyroSingleY = new ArrayList<Double>();
		List<Double> lstfSegGyroSingleZ = new ArrayList<Double>();
		
		for (i=nSegStartIdx; i<=nSegEndIdx; i++) {
			lstfSegGyroX.add(lstGyro.get(i).getGyroX());
			lstfSegGyroY.add(lstGyro.get(i).getGyroY());
			lstfSegGyroZ.add(lstGyro.get(i).getGyroZ());			
		}
		
		int nSegSize = nSegEndIdx - nSegStartIdx + 1;
		double fTimeDuration = Utility.calTimeDiff(lstGyro.get(nSegStartIdx).getTimestamp(), lstGyro.get(nSegEndIdx).getTimestamp());
		double dt = fTimeDuration/nSegSize;
		double fs = Math.round(nSegSize/fTimeDuration + 0.5);
		double n = Math.round(nSegSize*1.0/2);
		List<Double> lstfFreq = new ArrayList<Double>();
		double fFreq = 0;
		
		//Frequency
		for (i=0; i<=n-1; i++) {
			fFreq = i*1.0*fs/nSegSize;
			lstfFreq.add(fFreq);
		}

		double fSingleValue = 0;
		
		//Calculate Spectrum of X-axis data
		double fMeanX = Utility.calMean(lstfSegGyroX);
		
		for (i=0; i<lstfSegGyroX.size(); i++) {
			fSingleValue = lstfSegGyroX.get(i).doubleValue() - fMeanX;
			lstfSegGyroSingleX.add(fSingleValue);
		}
		
		List<Double>  lstfAmpSpecX = FFT.calFFT(lstfSegGyroSingleX);
		

		//Calculate Spectrum of Y-axis data
		double fMeanY = Utility.calMean(lstfSegGyroY);
		
		for (i=0; i<lstfSegGyroY.size(); i++) {
			fSingleValue = lstfSegGyroY.get(i).doubleValue() - fMeanY;
			lstfSegGyroSingleY.add(fSingleValue);
		}
		
		List<Double>  lstfAmpSpecY = FFT.calFFT(lstfSegGyroSingleY);
		
		
		//Calculate Spectrum of Z-axis data
		double fMeanZ = Utility.calMean(lstfSegGyroZ);
		
		for (i=0; i<lstfSegGyroZ.size(); i++) {
			fSingleValue = lstfSegGyroZ.get(i).doubleValue() - fMeanZ;
			lstfSegGyroSingleZ.add(fSingleValue);
		}
		
		List<Double> lstfAmpSpecZ = FFT.calFFT(lstfSegGyroSingleZ);
		
		//Split into buckets
		List<Integer> lstnBucketRange = findBucketRange(lstfFreq);

		//Calculate bucket features (for Bucket 1,2,3,5)
		double fMeanXBucket1 = Utility.calMean(lstfAmpSpecX, lstnBucketRange.get(0).intValue(), lstnBucketRange.get(1).intValue());
		double fMeanXBucket2 = Utility.calMean(lstfAmpSpecX, lstnBucketRange.get(1).intValue(), lstnBucketRange.get(2).intValue());
		double fMeanXBucket3 = Utility.calMean(lstfAmpSpecX, lstnBucketRange.get(2).intValue(), lstnBucketRange.get(3).intValue());
		double fMeanXBucket5 = Utility.calMean(lstfAmpSpecX, lstnBucketRange.get(4).intValue(), lstnBucketRange.get(5).intValue());

		double fMeanYBucket1 = Utility.calMean(lstfAmpSpecY, lstnBucketRange.get(0).intValue(), lstnBucketRange.get(1).intValue());
		double fMeanYBucket2 = Utility.calMean(lstfAmpSpecY, lstnBucketRange.get(1).intValue(), lstnBucketRange.get(2).intValue());
		double fMeanYBucket3 = Utility.calMean(lstfAmpSpecY, lstnBucketRange.get(2).intValue(), lstnBucketRange.get(3).intValue());
		double fMeanYBucket5 = Utility.calMean(lstfAmpSpecY, lstnBucketRange.get(4).intValue(), lstnBucketRange.get(5).intValue());

		double fMeanZBucket1 = Utility.calMean(lstfAmpSpecZ, lstnBucketRange.get(0).intValue(), lstnBucketRange.get(1).intValue());
		double fMeanZBucket2 = Utility.calMean(lstfAmpSpecZ, lstnBucketRange.get(1).intValue(), lstnBucketRange.get(2).intValue());
		double fMeanZBucket3 = Utility.calMean(lstfAmpSpecZ, lstnBucketRange.get(2).intValue(), lstnBucketRange.get(3).intValue());
		double fMeanZBucket5 = Utility.calMean(lstfAmpSpecZ, lstnBucketRange.get(4).intValue(), lstnBucketRange.get(5).intValue());
	
		double fFeatureBucket1 = Math.sqrt(Math.pow(fMeanXBucket1,2) + Math.pow(fMeanYBucket1,2) + Math.pow(fMeanZBucket1,2));
		double fFeatureBucket2 = Math.sqrt(Math.pow(fMeanXBucket2,2) + Math.pow(fMeanYBucket2,2) + Math.pow(fMeanZBucket2,2));
		double fFeatureBucket3 = Math.sqrt(Math.pow(fMeanXBucket3,2) + Math.pow(fMeanYBucket3,2) + Math.pow(fMeanZBucket3,2));
		double fFeatureBucket5 = Math.sqrt(Math.pow(fMeanXBucket5,2) + Math.pow(fMeanYBucket5,2) + Math.pow(fMeanZBucket5,2));
	
		double fVarX = Utility.calStd(lstfSegGyroX);
		double fVarY = Utility.calStd(lstfSegGyroY);
		double fVarZ = Utility.calStd(lstfSegGyroZ);
		
		//Detection position based on features
		nMnMResult = meansureMnM(fFeatureBucket1, fFeatureBucket2, fFeatureBucket3, fFeatureBucket5, fVarX, fVarY, fVarZ, nRoadArea);
		
		return nMnMResult;
	}
	
	
	//Preprocess data (moving average to filter outliers)
	private List<GyroData> preprocess(List<GyroData> lstGyro) {
		List<GyroData> lstGyroFiltered = new ArrayList<GyroData>();
		int nLen = lstGyro.size();
		List<Double> lstGyroX = new ArrayList<Double>();
		List<Double> lstGyroY = new ArrayList<Double>();
		List<Double> lstGyroZ = new ArrayList<Double>();
		List<Double> lstGyroFilteredX = new ArrayList<Double>();
		List<Double> lstGyroFilteredY = new ArrayList<Double>();
		List<Double> lstGyroFilteredZ = new ArrayList<Double>();
		int i;
		int nWinSize = 5;
		
		for (i=0; i<nLen; i++) {
			lstGyroX.add(lstGyro.get(i).getGyroX());
			lstGyroY.add(lstGyro.get(i).getGyroY());
			lstGyroZ.add(lstGyro.get(i).getGyroZ());
		}
		
		lstGyroFilteredX = Utility.MovingAverage(lstGyroX, nWinSize);
		lstGyroFilteredY = Utility.MovingAverage(lstGyroY, nWinSize);
		lstGyroFilteredZ = Utility.MovingAverage(lstGyroZ, nWinSize);
		
		for (i=0; i<nLen; i++) {
			GyroData gyro = new GyroData();
			gyro.setGyroData(lstGyro.get(i).getTimestamp(), lstGyroFilteredX.get(i).doubleValue(), lstGyroFilteredY.get(i).doubleValue(), lstGyroFilteredZ.get(i).doubleValue(), lstGyro.get(i).getGpsSpeed());
			lstGyroFiltered.add(gyro);
		}
		
		return lstGyroFiltered;
	}
	
	
	//Detect phone position based on Gyro data
	public int detect(List<GyroData> lstGyroRaw) {
		int nMnMResult = Utility.UNDECIDED;
		int nTotalCnt = lstGyroRaw.size();
		int i;
		int nTotalValidSpeedCnt = 0;
		double fSpeed = 0;
		double fTotalSpeed = 0;
		double fAvgSpeed = 0;
		int nRoadType = Utility.URBAN;
		int nZeroSpeedCnt = 0;
		double fStopRatio = 0;
		List<Integer> lstMnMResult = new ArrayList<Integer>();
		int nSegMnMResult = 0;
		int nEndIdx;
		int nCupHolderCnt = 0;
		int nPocketCnt = 0;
		double fCupHolderRatio = 0;
		double fPocketRatio = 0;
		
		List<GyroData> lstGyro = new ArrayList<GyroData>();
		
		if (nTotalCnt == 0) return nMnMResult;
		
		//Decide road type
		for (i=0; i<nTotalCnt; i++) {
			fSpeed = lstGyroRaw.get(i).getGpsSpeed();
			
			if (fSpeed != Utility.INVALID_SPEED) {
				nTotalValidSpeedCnt = nTotalValidSpeedCnt + 1;
				fTotalSpeed = fTotalSpeed + fSpeed;
				
				if (fSpeed == 0) {
					nZeroSpeedCnt = nZeroSpeedCnt + 1;
				}
			} 
		}
		
		if (nTotalValidSpeedCnt > 0) {   //Speed information is available
			fAvgSpeed = fTotalSpeed/nTotalValidSpeedCnt;
			
			if (fAvgSpeed >= Utility.SPEED_THRESHOLD) {
				nRoadType = Utility.HIGHWAY;
			}
			
			fStopRatio = nZeroSpeedCnt*1.0/nTotalValidSpeedCnt;
			
			if (fStopRatio > Utility.STOP_GPS_RATIO) {
				return nMnMResult;
			}
		}
		 
		
		// Here preprocess data (filter out outliers)
		lstGyro = lstGyroRaw;     // Replace this with lstGyro = preprocess(lstGyroRaw) in case preprocess is needed
		
		
		//Process segments
		for (i=0; i+Utility.SEGMENT_SIZE-1 < nTotalCnt ; i=i+Utility.SEGMENT_STEP) {
			nEndIdx = i + Utility.SEGMENT_SIZE - 1;
			
			nSegMnMResult = detectMnM(lstGyro, i, nEndIdx, nRoadType);  //Detect position for a segment
			
			if (nSegMnMResult == Utility.CUPHOLDER) {
				nCupHolderCnt = nCupHolderCnt + 1;
			} else if (nSegMnMResult == Utility.POCKET) {
				nPocketCnt = nPocketCnt + 1;
			}
			
			lstMnMResult.add(nSegMnMResult);
		}
		
		//Decide final position
		if (lstMnMResult.size() > 0) {
			fCupHolderRatio = nCupHolderCnt*1.0/lstMnMResult.size();
			fPocketRatio = nPocketCnt*1.0/lstMnMResult.size();;
			
			if (fCupHolderRatio >= POSITION_RATIO_THRESHOLD || fPocketRatio >= POSITION_RATIO_THRESHOLD) {
				nMnMResult = Utility.NONMOUNT;
			} else {
				nMnMResult = Utility.MOUNT;
			}
			
		}
		

		return nMnMResult;
	}

}

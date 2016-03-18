package com.example.mnm;

import java.util.ArrayList;
import java.util.List;

public class FFT {
	
	/* Fast Fourier Transform
	 * Size of lstfSignalR should be power of 2
	 * 
	 * The code is modified based on the following implementation of 
	 * FFT in Java: https://www.ee.columbia.edu/~ronw/code/MEAPsoft/doc/html/FFT_8java-source.html
	 */
	public static List<Double> calFFT(List<Double> lstfSignalR) {
		int i,j,k,n1,n2,a;
		double c,s,e,t1,t2;
		double fReal;
		double fImaginary;

		int n = lstfSignalR.size();
		int m = (int)(Math.log(n) / Math.log(2));
		int nHalf = Math.round(n/2);
		
		double[] cos = new double[n/2];
		double[] sin = new double[n/2];
		
		for(i=0; i<n/2; i++) {
			cos[i] = Math.cos(-2*Math.PI*i/n);
			sin[i] = Math.sin(-2*Math.PI*i/n);
		}
		
		//Generate imaginary part
		List<Double> lstfSignalI = new ArrayList<Double>();
		for (i = 0; i < n; i++) {
			lstfSignalI.add(Double.valueOf(0));
		}
		
		j = 0;
		n2 = n/2;
		
	    for (i=1; i < n - 1; i++) {
		    n1 = n2;
		    while ( j >= n1 ) {
		       j = j - n1;
		       n1 = n1/2;
		    }
		    
		    j = j + n1;
		     
		    if (i < j) {
		        t1 = lstfSignalR.get(i).doubleValue();
		        lstfSignalR.set(i, lstfSignalR.get(j));
		        lstfSignalR.set(j, t1);
		        
		        t1 = lstfSignalI.get(i).doubleValue();
		        lstfSignalI.set(i, lstfSignalI.get(j));
		        lstfSignalI.set(j, t1);
		    }
		}
			 
	    // FFT
	    n1 = 0;
		n2 = 1;
			   
	    for (i=0; i < m; i++) {
	        n1 = n2;
	        n2 = n2 + n2;
	        a = 0;
	     
	        for (j=0; j < n1; j++) {
	           c = cos[a];
	           s = sin[a];
	           a +=  1 << (m-i-1);
	 
	           for (k=j; k < n; k=k+n2) {
	               t1 = c*lstfSignalR.get(k+n1).doubleValue() - s*lstfSignalI.get(k+n1).doubleValue();
	               t2 = s*lstfSignalR.get(k+n1).doubleValue() + c*lstfSignalI.get(k+n1).doubleValue();
	               lstfSignalR.set(k+n1, lstfSignalR.get(k).doubleValue() - t1);
	               lstfSignalI.set(k+n1, lstfSignalI.get(k).doubleValue() - t2);
	               lstfSignalR.set(k, lstfSignalR.get(k).doubleValue() + t1);	               
	               lstfSignalI.set(k, lstfSignalI.get(k).doubleValue() + t2);
	           }
	        }
	     }

		//Calculate amplitude
		List<Double>  lstfFFTRaw = new ArrayList<Double>();

		for (i=0; i<lstfSignalR.size(); i++) {
			fReal = lstfSignalR.get(i).doubleValue();
			fImaginary = lstfSignalI.get(i).doubleValue();
			lstfFFTRaw.add(Math.sqrt(fReal*fReal + fImaginary*fImaginary)/nHalf);
		}
		
		//Take only half (according to Nyquist theory)
		List<Double>  lstfFFT = new ArrayList<Double>();
		for (i=0; i<nHalf; i++) {
			lstfFFT.add(lstfFFTRaw.get(i));
		}
		
		Utility.DEBUG("calFFT");
		
		return lstfFFT;
	}

}


package edu.berkeley.boinc.utils;

import android.annotation.SuppressLint;

import java.io.IOException;
import java.io.Reader;

public class BOINCUtils {
	
	public static String readLineLimit(Reader reader, int limit) throws IOException {
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < limit; i++) {
			int c = reader.read(); //Read in single character
			if(c == -1) {
				return ((sb.length() > 0) ? sb.toString() : null);
			}
			
			if(((char) c == '\n') || ((char) c == '\r')) { //Found end of line, break loop.
				break;
			}
			
			sb.append((char) c); // String is not over and end line not found
		}
		
		return sb.toString(); //end of line was found.
	}

	@SuppressLint("DefaultLocale")
	public static String formatSize(double fBytesSent, double fFileSize) {
		String buf = new String();
	    double xTera = 1099511627776.0;
	    double xGiga = 1073741824.0;
	    double xMega = 1048576.0;
	    double xKilo = 1024.0;

	    if (fFileSize != 0) {
	        if        (fFileSize >= xTera) {
	            buf = String.format("%.2f/%.2f TB", fBytesSent/xTera, fFileSize/xTera);
	        } else if (fFileSize >= xGiga) {
	        	buf = String.format("%.2f/%.2f GB", fBytesSent/xGiga, fFileSize/xGiga);
	        } else if (fFileSize >= xMega) {
	        	buf = String.format("%.2f/%.2f MB", fBytesSent/xMega, fFileSize/xMega);
	        } else if (fFileSize >= xKilo) {
	        	buf = String.format("%.2f/%.2f KB", fBytesSent/xKilo, fFileSize/xKilo);
	        } else {
	        	buf = String.format("%.0f/%.0f bytes", fBytesSent, fFileSize);
	        }
	    } else {
	        if        (fBytesSent >= xTera) {
	        	buf = String.format("%.2f TB", fBytesSent/xTera);
	        } else if (fBytesSent >= xGiga) {
	        	buf = String.format("%.2f GB", fBytesSent/xGiga);
	        } else if (fBytesSent >= xMega) {
	        	buf = String.format("%.2f MB", fBytesSent/xMega);
	        } else if (fBytesSent >= xKilo) {
	        	buf = String.format("%.2f KB", fBytesSent/xKilo);
	        } else {
	        	buf = String.format("%.0f bytes", fBytesSent);
	        }
	    }

	    return buf;
	}
    
}

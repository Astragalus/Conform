package org.mtc.conform;

import android.graphics.Bitmap;

public class ConformLib {

	private static ConformLib lib = null; 

	private ConformLib() {
	}
	
	public static synchronized ConformLib get() {
		if (lib == null) {
			System.loadLibrary("conform");
			lib = new ConformLib();
		}
		return lib;
	}
	
	//Boundary Treatments
	public static final int TILE = 0; 
	public static final int CLAMP = 1; 
	
	public native int pullbackBitmaps(Bitmap bmFrom, Bitmap bmTo, float x, float y, int wrapMode);
}

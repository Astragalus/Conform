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
	
	public native int pullbackBitmaps(Bitmap bmFrom, Bitmap bmTo, float x, float y);
}

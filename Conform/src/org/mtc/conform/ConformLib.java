package org.mtc.conform;

import android.graphics.Bitmap;

public class ConformLib {

	static {
		System.loadLibrary("conform");
	}
	
	public static native int pullbackBitmaps(Bitmap bmFrom, Bitmap bmTo);
}

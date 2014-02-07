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
	
	public enum WrapMode {
		TILE(0),
		CLAMP(1);
		private WrapMode(final int mode) {this.mode =  mode;}
		public int getConstant() {return mode;}
		private final int mode;
	}
	
	public native int pullbackBitmaps(Bitmap bmFrom, Bitmap bmTo, float x, float y, float pivotX, float pivotY, float scaleFac, int wrapMode);
}

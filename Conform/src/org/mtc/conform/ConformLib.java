package org.mtc.conform;

import java.nio.FloatBuffer;

import org.mtc.conform.math.ComplexAffineTrans;

import android.graphics.Bitmap;

public class ConformLib {

	public final static ConformLib INSTANCE = new ConformLib(); 

	private ConformLib() {
		System.loadLibrary("conform");
	}
	
	public enum WrapMode {
		TILE(0),
		CLAMP(1);
		private WrapMode(final int mode) {this.mode =  mode;}
		public int getInt() {return mode;}
		private final int mode;
	}
	
	public int pullback(Bitmap sourceBitmap, Bitmap viewBitmap, FloatBuffer paramBuffer, int numParams, ComplexAffineTrans currTrans, WrapMode wrapMode) {
		return pullbackBitmaps(sourceBitmap, viewBitmap, paramBuffer, numParams, currTrans.tr.re, currTrans.tr.im, currTrans.sc.re, wrapMode.getInt());
	}
	
	private native int pullbackBitmaps(Bitmap sourceBitmap, Bitmap viewBitmap, FloatBuffer paramBuffer, int numParams, float pivotX, float pivotY, float scaleFac, int wrapMode);
}

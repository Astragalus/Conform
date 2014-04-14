package org.mtc.conform;

import org.mtc.conform.math.Complex;
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
	
	public int pullback(Bitmap sourceBitmap, Bitmap viewBitmap, Complex param, ComplexAffineTrans currTrans, WrapMode wrapMode, int degree) {
		return pullbackBitmaps(sourceBitmap, viewBitmap, param.re, param.im, currTrans.tr.re, currTrans.tr.im, currTrans.sc.re, wrapMode.getInt(), degree);
	}
	
	private native int pullbackBitmaps(Bitmap sourceBitmap, Bitmap viewBitmap, float x, float y, float pivotX, float pivotY, float scaleFac, int wrapMode, int degree);
}

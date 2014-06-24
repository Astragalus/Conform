package org.mtc.conform;

import org.mtc.conform.math.ComplexAffineTrans;
import org.mtc.conform.math.ComplexArray;

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
	
	public int pullback(Bitmap sourceBitmap, Bitmap viewBitmap, ComplexArray params, ComplexAffineTrans currTrans, WrapMode wrapMode) {
		return pullbackBitmaps(sourceBitmap, viewBitmap, params.size(), currTrans.tr.re, currTrans.tr.im, currTrans.sc.re, wrapMode.getInt(),
				params.arr[0], params.arr[1],
				params.arr[2], params.arr[3], 
				params.arr[4], params.arr[5], 
				params.arr[6], params.arr[7],
				params.arr[8], params.arr[9],
				params.arr[10],params.arr[11]);
	}
	
	private native int pullbackBitmaps(Bitmap sourceBitmap, Bitmap viewBitmap, int numParams, float pivotX, float pivotY, float scaleFac, int wrapMode,
				float p1r, float p1i, 
				float p2r, float p2i, 
				float p3r, float p3i, 
				float p4r, float p4i, 
				float p5r, float p5i, 
				float p6r, float p6i);
}

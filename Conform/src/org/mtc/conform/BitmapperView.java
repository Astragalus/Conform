/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.mtc.conform;

import org.mtc.conform.BitmapperView.BitmapperMode.TouchMode;
import org.mtc.conform.TransformationState.OnTransformStateChangedListener;
import org.mtc.conform.math.Complex;
import org.mtc.conform.math.ComplexArray;
import org.mtc.conform.math.ComplexArray.ComplexElement;
import org.mtc.conform.math.ComplexArray.IComplexAction;
import org.mtc.conform.math.ComplexArray.IComplexPredicate;
import org.mtc.conform.math.IComplex;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

public class BitmapperView extends ImageView {

	public static final String TAG = "Conform";
	
	private static class ParamDrawer implements IComplexAction {
		private Canvas canvas;
		private final Paint paint;
		private final int radius;
		
		ParamDrawer(final Paint paint, final int radius) {
			this.paint = paint;
			this.radius = radius;
		}
		ParamDrawer setCanvas(final Canvas canvas) {
			this.canvas = canvas;
			return this;
		}
		@Override
		public void actOn(IComplex param) {
			canvas.drawCircle(param.re(), param.im(), radius, paint);
		}
	}

	public class ParamHolder implements OnTransformStateChangedListener {
		public final static int MAX_PARAMS = 6;
		public final static float RADIUS = 100.0f;

		public ParamHolder(final TransformationState transStateHolder) {
			m_screenCoords = new ComplexArray(MAX_PARAMS);
			m_normCoords = new ComplexArray(MAX_PARAMS);
			m_trans = transStateHolder;
			m_trans.setOnTransformStateChangedListener(this);
			addParamNormCoords(0.0f, 0.0f);
		}
		
		public void addParamScreenCoords(float scrX, float scrY) {
			m_screenCoords.append().assignFrom(scrX, scrY);
			m_normCoords.append();
			m_trans.screenToNormalizedPoints(m_screenCoords, m_normCoords);
			invalidate();
		}
		
		public void addParamNormCoords(float re, float im) {
			m_normCoords.append().assignFrom(re, im);
			m_screenCoords.append();
			updateScreenCoords();
		}

		public void removeParam() {
			m_normCoords.remove();
			m_screenCoords.remove();
			invalidate();
		}
		
		public void applyScreenCoords(final IComplexAction action) {
			m_screenCoords.apply(action);
		}
		
		public void applyNormCoords(final IComplexAction action) {
			m_normCoords.apply(action);
		}
		
		public ComplexElement findParamNearCoords(float scrX, float scrY) {
			final Complex at = new Complex(scrX, scrY);
			final ComplexElement result = m_screenCoords.find(new IComplexPredicate() {
				@Override
				public boolean eval(IComplex z) {
					return (z.distSq(at) <= RADIUS*RADIUS);
				}
			});
			return result;
		}
		
		public int size() {
			return m_normCoords.size();
		}
		
		public void setParamScreenCoords(final ComplexElement scrParam, float scrX, float scrY) {
			m_trans.screenToNormalizedPoint(scrParam.re(scrX).im(scrY), m_normCoords.atIndexOf(scrParam));
			invalidate();
		}
		
		private void updateScreenCoords() {
			m_trans.normalizedToScreenPoints(m_normCoords, m_screenCoords);
			invalidate();
		}
		
		public ComplexArray getNormalizedParams() {
			return m_normCoords;
		}
		
		@Override
		public String toString() {
			return "norm[" + m_normCoords.toString() + "] screen[" + m_screenCoords.toString() + "]";
		}
		
		final private ComplexArray m_normCoords;
		final private ComplexArray m_screenCoords;
		final private TransformationState m_trans;

		@Override
		public void onTransformStateChanged() {
			updateScreenCoords();
		}
	}
	
	public static class BitmapperMode {
		public enum TouchMode {
			PARAM(0),
			PAN(1);
			private TouchMode(final int mode) {this.mode =  mode;}
			public int getInt() {return mode;}
			private final int mode;
		}
		private TouchMode m_touchMode = TouchMode.PARAM;
		public TouchMode getTouchMode() {
			return m_touchMode;
		}
		public void setTouchMode(final TouchMode touchMode) {
			m_touchMode = touchMode;
		}
	}

	private Bitmap m_srcBitmap;
	private Bitmap m_destBitmap = null;
	
	private int m_drawWidth = 384;
	private int m_drawHeight = 384;
	
	private final BitmapperTouchHandler m_touchHandler;
	private final TransformationState m_transState;

	ConformLib.WrapMode m_wrapMode = ConformLib.WrapMode.TILE;
	private final BitmapperMode m_mode;
	
	private final ParamDrawer m_poleDrawer;
	private final ParamHolder m_paramHolder;

//	long start;
//	long time;
//	int count;
//	float fps;
	
	public static final int RADIUS = 10;
	
	public BitmapperView(Context context, AttributeSet attrs) {
		super(context, attrs);
		m_mode = new BitmapperMode();
		setSourceBitmap(((BitmapDrawable) getDrawable()).getBitmap());
		m_destBitmap = Bitmap.createBitmap(m_drawWidth, m_drawHeight, Config.ARGB_8888);
		m_destBitmap.eraseColor(0);
		setImageBitmap(m_destBitmap);
		m_transState = new TransformationState(this, m_drawWidth, m_drawHeight);
		m_paramHolder = new ParamHolder(m_transState);
		m_touchHandler = new BitmapperTouchHandler(context, m_transState, m_mode, m_paramHolder);
		final Paint paint = new Paint();
		paint.setAntiAlias(false);
		paint.setARGB(255, 255, 55, 178);
		m_poleDrawer = new ParamDrawer(paint, RADIUS);
	}
	
	@Override
	protected void onDraw(final Canvas canvas) {	
//		if (count == 0)
//			start = System.currentTimeMillis();
		m_destBitmap.eraseColor(0);
		ConformLib.INSTANCE.pullback(m_srcBitmap, m_destBitmap, m_paramHolder.getNormalizedParams(), m_transState.getCurrTrans(), m_wrapMode);
		canvas.drawBitmap(m_destBitmap, getImageMatrix(), null);
		m_paramHolder.applyScreenCoords(m_poleDrawer.setCanvas(canvas));
//		++count;
//		if ((time = System.currentTimeMillis()-start) < 3000) {
//			Log.i(TAG,String.format("fps: %2.2f", (float)(1000*count)/(float)time));
//		} else {
//			count = 0;
//		}
	}
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (changed) {
			m_transState.updateMatrices();
			invalidate();
		}
	}
	
	public void addParam() {
		m_paramHolder.addParamNormCoords(0.0f, 0.0f);
	}
	
	public void removeParam() {
		m_paramHolder.removeParam();
	}
	
	public void setSourceBitmap(final Bitmap sourceBitmap) {
		m_srcBitmap = sourceBitmap;
		invalidate();
	}
	
	public Bitmap getImageBitmap() {
		return ((BitmapDrawable)getDrawable()).getBitmap(); 
	}
	
	public void setWrapMode(final ConformLib.WrapMode wrapMode) {
		m_wrapMode = wrapMode;
		Log.i(TAG, "Wrap mode set to: " + wrapMode.name());
		invalidate();
	}
	
	public void setTouchMode(final TouchMode touchMode) {
		m_mode.m_touchMode = touchMode;
		Log.i(TAG, "touch mode  [" + touchMode.name() + "]");
		invalidate();
	}
	
	public TouchMode getTouchMode() {
		return m_mode.m_touchMode;
	}
	
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (m_touchHandler.onTouchEvent(event)) {
			invalidate();
			return true;
		} else {
			return false;
		}
	}
}

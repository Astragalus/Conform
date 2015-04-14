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

import org.mtc.conform.BitmapperView.BitmapperMode.Mode;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class BitmapperView extends ImageView implements TextWatcher {

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
		public enum Mode {
			PINK_DOTS(0),
			THREE_POINTS(1);
			private Mode(final int mode) {this.mode =  mode;}
			public int getInt() {return mode;}
			private final int mode;
		}
		private Mode m_touchMode = Mode.PINK_DOTS;
		public Mode getTouchMode() {
			return m_touchMode;
		}
		public void setTouchMode(final Mode touchMode) {
			m_touchMode = touchMode;
		}
	}

	private Bitmap m_srcBitmap;
	private Bitmap m_destBitmap = null;
	
	private int m_drawWidth = 420;
	private int m_drawHeight = 420;
	
	private final BitmapperTouchHandler m_touchHandler;
	private final TransformationState m_transState;

	ConformLib.WrapMode m_wrapMode = ConformLib.WrapMode.TILE;
	private final BitmapperMode m_mode;
	
	private final ParamDrawer m_poleDrawer;
	private final ParamHolder m_paramHolder;
	
	private String m_expression = "";

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
		m_destBitmap.eraseColor(0);
		ConformLib.INSTANCE.pullback(m_srcBitmap, m_destBitmap, m_paramHolder.getNormalizedParams(), m_transState.getCurrTrans(), m_wrapMode);
		canvas.drawBitmap(m_destBitmap, getImageMatrix(), null);
		m_paramHolder.applyScreenCoords(m_poleDrawer.setCanvas(canvas));
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
	
	public void setExpression(String expr) {
		m_expression = expr;
	}
	
	public void setWrapMode(final ConformLib.WrapMode wrapMode) {
		m_wrapMode = wrapMode;
		Log.i(TAG, "Wrap mode set to: " + wrapMode.name());
		invalidate();
	}
	
	public void setTouchMode(final Mode touchMode) {
		m_mode.m_touchMode = touchMode;
		Log.i(TAG, "touch mode  [" + touchMode.name() + "]");
		View exp = findViewById(R.id.editTextExpression);
		if (exp != null) {
			exp.setActivated(touchMode.equals(Mode.THREE_POINTS));
		} else {
			Log.e(TAG,"nullzz");
		}
		invalidate();
	}
	
	public Mode getTouchMode() {
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

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,	int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	@Override
	public void afterTextChanged(Editable s) {
		setExpression(s.toString());
		invalidate();
	}
}

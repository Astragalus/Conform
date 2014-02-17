package org.mtc.conform;

import org.mtc.conform.math.Complex;
import org.mtc.conform.math.ComplexAffineTrans;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.widget.ImageView;

public class BitmapperView extends ImageView {

	public static final String TAG = "Conform";

	private class TransformationState {
		final private Matrix m_screenToSquareMat = new Matrix();
		final private ComplexAffineTrans m_currTrans = ComplexAffineTrans.IDENT;
		final private Complex m_param = new Complex(0.5f, 0.5f);
		final private Complex m_pivot = new Complex(Complex.ONE);
		final private Complex m_translate = new Complex(Complex.ZERO);
		private float[] m_point = new float[2];
		public void updateViewTransformation() {
			getImageMatrix().invert(m_screenToSquareMat);
			m_screenToSquareMat.postScale(1.0f/m_drawWidth, 1.0f/m_drawHeight);
		}
		public void scale(final float s, final float x, final float y) {
			screenToSquarePointComplex(x, y, m_pivot);
			m_currTrans.postMult(ComplexAffineTrans.scaling(s, m_pivot));
		}
		public void translate(final float x, final float y) {
			screenToSquareVectorComplex(-x, -y, m_translate);
			m_currTrans.postMult(ComplexAffineTrans.translation(m_translate));
		}
		public void paramChange(final float screenX, final float screenY) {
			screenToSquarePointComplex(screenX, screenY, m_param);
			m_currTrans.applyInverse(m_param);
		}
		private void screenToSquarePointComplex(final float x, final float y, final Complex output) {
			m_point[0] = x;
			m_point[1] = y;
			m_screenToSquareMat.mapPoints(m_point);
			output.re = m_point[0];
			output.im = m_point[1];			
		}
		private void screenToSquareVectorComplex(final float x, final float y, final Complex output) {
			m_point[0] = x;
			m_point[1] = y;
			m_screenToSquareMat.mapVectors(m_point);
			output.re = m_point[0];
			output.im = m_point[1];
		}
	};

	private class BitmapperTouchHandler extends SimpleOnGestureListener implements OnScaleGestureListener {
		private final ScaleGestureDetector m_zoomDetector;
		private final GestureDetector m_gestureDetector;
		private final TransformationState m_state;
		public BitmapperTouchHandler(final Context context, final TransformationState state) {
			m_zoomDetector = new ScaleGestureDetector(context, this);
			m_gestureDetector = new GestureDetector(getContext(), this);
			m_state = state;
		}
		public boolean onTouchEvent(MotionEvent event) {
			boolean processed = false;
			switch (m_touchMode) {
			case PARAM:
				processed |= onParamChgEvent(event);
				break;
			case PAN:
				processed |= m_zoomDetector.onTouchEvent(event);
				processed |= m_gestureDetector.onTouchEvent(event);
				break;
			}
			return processed;
		}
		private boolean onParamChgEvent(MotionEvent event) {
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				m_state.paramChange(event.getX(), event.getY());
				return true;
			}
			return false;
		}
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			m_state.scale(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
			return true;
		}
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			return true;
		}
		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
		}
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,	float distanceX, float distanceY) {
			m_state.translate(distanceX, distanceY);
			return true;
		}
	};

	public enum TouchMode {
		PARAM(0),
		PAN(1);
		private TouchMode(final int mode) {this.mode =  mode;}
		public int getInt() {return mode;}
		private final int mode;
	}

	private Bitmap m_srcBitmap;
	private Bitmap m_destBitmap = null;
	
	public int m_drawWidth = 256;
	public int m_drawHeight = 256;
	
	private final BitmapperTouchHandler m_touchHandler;
	private final TransformationState m_transState;

	ConformLib.WrapMode m_wrapMode = ConformLib.WrapMode.TILE;
	TouchMode m_touchMode = TouchMode.PARAM;
	
	public BitmapperView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setSourceBitmap(((BitmapDrawable) getDrawable()).getBitmap());
		m_destBitmap = Bitmap.createBitmap(m_drawWidth, m_drawHeight, Config.ARGB_8888);
		setImageBitmap(m_destBitmap);
		m_transState = new TransformationState();
		m_touchHandler = new BitmapperTouchHandler(context, m_transState);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		m_destBitmap.eraseColor(0);
		ConformLib.INSTANCE.pullback(m_srcBitmap, m_destBitmap, m_transState.m_param, m_transState.m_currTrans, m_wrapMode);
		canvas.drawBitmap(m_destBitmap, getImageMatrix(), null);
	}
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (changed) {
			m_transState.updateViewTransformation();
			invalidate();
		}
	}
	
	public void setSourceBitmap(final Bitmap sourceBitmap) {
		m_srcBitmap = sourceBitmap;
		invalidate();
	}
	
	public void setWrapMode(final ConformLib.WrapMode wrapMode) {
		m_wrapMode = wrapMode;
		Log.d(TAG, "Wrap mode set to: " + wrapMode.name());
		invalidate();
	}
	
	public void setTouchMode(final TouchMode touchMode) {
		m_touchMode = touchMode;
		Log.d(TAG, "Touch mode set to: " + touchMode.name());
		invalidate();
	}
	
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

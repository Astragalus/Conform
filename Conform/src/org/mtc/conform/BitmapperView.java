package org.mtc.conform;

import java.util.Observable;
import java.util.Observer;

import org.mtc.conform.math.ComplexAffineTrans;
import org.mtc.conform.math.ComplexArrayBacked;
import org.mtc.conform.math.IComplex;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
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

	private static abstract class Operation {
		abstract void operate(IComplex param);
	}
	
	private static class DrawOp extends Operation {
		private Canvas canvas;
		private final Paint paint;
		private final int radius;
		
		DrawOp(final Paint paint, final int radius) {
			this.paint = paint;
			this.radius = radius;
		}
		DrawOp setCanvas(final Canvas canvas) {
			this.canvas = canvas;
			return this;
		}
		@Override
		void operate(IComplex param) {
			canvas.drawCircle(param.re(), param.im(), radius, paint);
		}
	}
	
	public class ParamHolder implements Observer {
		public final static int MAX_PARAMS = 6;
		public final static float RADIUS = 100.0f;

		public ParamHolder(final TransformationState transStateHolder) {
			m_screenCoords = new float[MAX_PARAMS];
			m_normCoords = new float[MAX_PARAMS];
			m_size = 1;
			m_screenComplexView = new ComplexArrayBacked(m_screenCoords);
			m_normComplexView = new ComplexArrayBacked(m_normCoords);
			m_trans = transStateHolder;
			m_trans.addObserver(this);
		}
		
		public void addParamScreenCoords(float scrX, float scrY) {
			if (m_size < MAX_PARAMS) {
				m_screenComplexView.atIndex(m_size).assignFrom(scrX, scrY);
				updateNormCoordsFor(m_size);
				++m_size;
			}
		}
		
		public void addParamNormCoords(float im, float re) {
			if (m_size < MAX_PARAMS) {
				m_normComplexView.atIndex(m_size).assignFrom(re, im);
				updateScreenCoordsFor(m_size);
				++m_size;
			}
		}
		
		public void applyToScreenParams(final Operation operation) {
			for (int i = 0; i < 2*m_size; i+=2) {
				operation.operate(m_screenComplexView.atIndex(i));
			}
		}
		
		public int findParamIndex(float scrX, float scrY) {
			for (int i = 0; i < m_size; ++i) {
				final float distX = m_screenCoords[2*i]-scrX;
				final float distY = m_screenCoords[2*i+1]-scrY;
				if (distX*distX + distY*distY <= RADIUS*RADIUS) {
					return i;
				}
			}
			return -1;
		}
		
		public int size() {
			return m_size;
		}
		
		public void setParamScreenCoords(int paramNum, float newX, float newY) {
			final int i = 2*paramNum;
			m_screenCoords[i] = newX;
			m_screenCoords[i+1] = newY;
			updateNormCoordsFor(paramNum);
		}
		
		private void updateNormCoordsFor(int paramNum) {
			m_trans.screenToNormalized(m_screenComplexView.atIndex(2*paramNum), m_normComplexView.atIndex(2*paramNum));
		}

		private void updateScreenCoordsFor(int paramNum) {
			m_trans.normalizedToScreen(m_normComplexView.atIndex(2*paramNum), m_screenComplexView.atIndex(2*paramNum));
			invalidate();
		}
		
		private void updateAllScreenCoords() {
			m_trans.normalizedToScreen(m_normCoords, m_screenCoords, m_size);
			invalidate();
		}
		
		/**
		 * TransformationState has changed.
		 */
		@Override
		public void update(Observable observable, Object data) {
			updateAllScreenCoords();
		}
		
		public int getSize() {
			return m_size;
		}
		
		public float[] getParams() {
			return m_normCoords;
		}
		
		private int m_size;
		final private float[] m_normCoords;
		final private float[] m_screenCoords;
		final private ComplexArrayBacked m_normComplexView;
		final private ComplexArrayBacked m_screenComplexView;
		final private TransformationState m_trans;
	}
	
	private class TransformationState extends Observable {
		final private Matrix m_screenToSquareMat = new Matrix();
		final private Matrix m_squareToScreenMat = new Matrix();
		final private ComplexAffineTrans m_currTrans = ComplexAffineTrans.IDENT;
		final private ComplexArrayBacked m_complexView = new ComplexArrayBacked();
		final private ComplexArrayBacked m_pivot = new ComplexArrayBacked(new float[] {1.0f,0.0f});
		final private ComplexArrayBacked m_translate = new ComplexArrayBacked(new float[] {0.0f,0.0f});

		@Override
		public void addObserver(Observer observer) {
			super.addObserver(observer);
		}
		
		public void updateMatrices() {
			m_squareToScreenMat.set(getImageMatrix());
			m_squareToScreenMat.preScale(m_drawWidth, m_drawHeight);
			getImageMatrix().invert(m_screenToSquareMat);
			m_screenToSquareMat.postScale(1.0f/m_drawWidth, 1.0f/m_drawHeight);
		}
		public void scale(final float s, final float x, final float y) {
			m_currTrans.postMult(ComplexAffineTrans.scaling(s, screenToNormalized(m_pivot.re(x).im(y))));
			notifyObservers();
		}
		public void translate(final float x, final float y) {
			m_currTrans.postMult(ComplexAffineTrans.translation(screenToNormalized(m_translate.re(-x).im(-y))));
			notifyObservers();
		}
		public ComplexArrayBacked screenToNormalized(ComplexArrayBacked srcdst) {
			screenToNormalized(srcdst, srcdst);
			return srcdst;
		}
		public void screenToNormalized(ComplexArrayBacked src, ComplexArrayBacked dst) {
			m_screenToSquareMat.mapVectors(dst.getBackingArray(), dst.getIndex(), src.getBackingArray(), src.getIndex(), 1);
			m_currTrans.applyInverse(dst);
		}	
		public void screenToNormalized(float[] src, float[] dst, int count) {
			m_screenToSquareMat.mapVectors(dst, src);
			m_complexView.setBackingArray(dst); 
			for (int i = 0; i < 2*count; i += 2) {
				m_currTrans.applyInverse(m_complexView.atIndex(i));				
			}
		}
		public void normalizedToScreen(ComplexArrayBacked src, ComplexArrayBacked dst) {
			m_currTrans.apply(dst.assignFrom(src));
			m_squareToScreenMat.mapVectors(dst.getBackingArray(), dst.getIndex(), src.getBackingArray(), src.getIndex(), 1);
		}
		public void normalizedToScreen(float[] src, float[] dst, int count) {
			System.arraycopy(src, 0, dst, 0, 2*count);
			m_complexView.setBackingArray(dst);
			for (int i = 0; i < count; i += 2) {
				m_currTrans.apply(m_complexView.atIndex(i));				
			}
			m_squareToScreenMat.mapVectors(dst, src);
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
			final int numPtrs = event.getPointerCount();
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				final int ptrIdx = event.getActionIndex();
				final int i = m_paramHolder.findParamIndex(event.getX(ptrIdx), event.getY(ptrIdx));
				Log.d(TAG, "onParamChgEvent: numPtrs=" + numPtrs + " actionPtr=" + ptrIdx + " paramIdx=" + i +" x=" + event.getX(ptrIdx)+" y=" + event.getY(ptrIdx));
				if (i >= 0) {
					m_paramHolder.setParamScreenCoords(i, event.getX(ptrIdx), event.getY(ptrIdx));
					return true;
				}
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
	
	private int m_drawWidth = 320;
	private int m_drawHeight = 320;
	
	private final BitmapperTouchHandler m_touchHandler;
	private final TransformationState m_transState;

	ConformLib.WrapMode m_wrapMode = ConformLib.WrapMode.TILE;
	TouchMode m_touchMode = TouchMode.PARAM;
	
	private final DrawOp m_poleDrawOp;
	private final ParamHolder m_paramHolder;

	long start;
	long time;
	int count;
	float fps;
	
	public static final int RADIUS = 10;
	
	public BitmapperView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setSourceBitmap(((BitmapDrawable) getDrawable()).getBitmap());
		m_destBitmap = Bitmap.createBitmap(m_drawWidth, m_drawHeight, Config.ARGB_8888);
		m_destBitmap.eraseColor(0);
		m_destBitmap.setHasAlpha(false);
		setImageBitmap(m_destBitmap);
		m_transState = new TransformationState();
		m_paramHolder = new ParamHolder(m_transState);
		m_touchHandler = new BitmapperTouchHandler(context, m_transState);
		final Paint paint = new Paint();
		paint.setAntiAlias(false);
		paint.setARGB(180, 255, 110, 130);
		m_poleDrawOp = new DrawOp(paint, RADIUS);
	}
	
	@Override
	protected void onDraw(final Canvas canvas) {	
		if (count == 0)
			start = System.currentTimeMillis();
		
		m_destBitmap.eraseColor(0);
		ConformLib.INSTANCE.pullback(m_srcBitmap, m_destBitmap, m_paramHolder.getParams(), m_paramHolder.getSize(), m_transState.m_currTrans, m_wrapMode);
		canvas.drawBitmap(m_destBitmap, getImageMatrix(), null);
		
		m_paramHolder.applyToScreenParams(m_poleDrawOp.setCanvas(canvas));
		
		++count;
		if ((time = System.currentTimeMillis()-start) < 3000) {
			Log.i(TAG,String.format("fps: %2.2f", (float)(1000*count)/(float)time));
		} else {
			count = 0;
		}
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

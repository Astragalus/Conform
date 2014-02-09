package org.mtc.conform;

import org.mtc.conform.math.Complex;
import org.mtc.conform.math.ComplexAffineTrans;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.widget.ImageView;

public class BitmapperView extends ImageView {

	public static final String TAG = "Conform";

	private static class TransformationState {
		public TransformationState(final ImageView view) {
			m_view = view;
		}
		final private ImageView m_view;
		final private Matrix m_screenToSquareMat = new Matrix();
		final private Rect m_drawingRect = new Rect();
		final private ComplexAffineTrans m_currTrans = ComplexAffineTrans.IDENT;
		final private Complex m_param = new Complex(0.5f, 0.5f);
		private float[] m_point = new float[2];
		
		public void updateMatrix() {
			m_view.getDrawingRect(m_drawingRect);
			m_view.getImageMatrix().invert(m_screenToSquareMat);
			m_screenToSquareMat.postScale(1.0f/m_drawingRect.width(), 1.0f/m_drawingRect.height());
			//Log.d(TAG, "Matrix updated: drawing rect w=" + m_drawingRect.width() + ", h=" + m_drawingRect.height());
			m_view.invalidate();
		}
		public void scale(final float s, final float x, final float y) {
			m_point[0] = x;
			m_point[1] = y;
			m_screenToSquareMat.mapPoints(m_point);
			m_currTrans.postMult(ComplexAffineTrans.scaling(s, m_point[0], m_point[1]));
			m_view.invalidate();
		}
		public void paramChamge(final float screenX, final float screenY) {
			m_point[0] = screenX;
			m_point[1] = screenY;
			m_screenToSquareMat.mapPoints(m_point);
			m_param.re = m_point[0];
			m_param.im = m_point[1];
			m_view.invalidate();
		}
	};

	private static class BitmapperTouchHandler implements OnScaleGestureListener {
		private final ScaleGestureDetector m_detector;
		private final View m_view;
		private final TransformationState m_state;
		public BitmapperTouchHandler(final Context context, final View view, final TransformationState state) {
			m_detector = new ScaleGestureDetector(context, this);
			m_view = view;
			m_state = state;
		}
		private boolean onParamChgEvent(MotionEvent event) {
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				m_state.paramChamge(event.getX(), event.getY());
				m_view.invalidate();
				return true;
			}
			return false;
		}
		public boolean onTouchEvent(MotionEvent event) {
			//Log.d(TAG, "Processing touch event: x1=" + event.getX(0) + ", y1=" + event.getY(0) + ((event.getPointerCount()>1)?(", x2=" + event.getX(1) + ", y2=" + event.getY(1)):""));
			boolean processed = false;
			if (event.getPointerCount() == 1) {
				processed |= onParamChgEvent(event);
			}
			processed |= m_detector.onTouchEvent(event);
			return processed;
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
	};

	private Bitmap m_srcBitmap;
	private Bitmap m_destBitmap = null;
	
	public int m_destWidth = 256;
	public int m_destHeight = 256;
	
	private final BitmapperTouchHandler m_touchHandler;
	private final TransformationState m_transState;

	private ConformLib.WrapMode m_wrapMode = ConformLib.WrapMode.TILE;
	
//	long start;
//	int count;
	
	public BitmapperView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setSourceBitmap(((BitmapDrawable) getDrawable()).getBitmap());
		m_destBitmap = Bitmap.createBitmap(m_destWidth, m_destHeight, Config.ARGB_8888);
		setImageBitmap(m_destBitmap);
		m_transState = new TransformationState(this);
		m_touchHandler = new BitmapperTouchHandler(context, this, m_transState);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		m_destBitmap.eraseColor(0);
//		if (count == 0)
//			start = System.currentTimeMillis();
		ConformLib.INSTANCE.pullback(m_srcBitmap, m_destBitmap, m_transState.m_param, m_transState.m_currTrans, m_wrapMode);
//		if (System.currentTimeMillis()-start < 1000) {
//			++count;
//		} else {
//			Log.i(TAG,String.format("fps: %2d", count));
//			count = 0;
//		}
		canvas.drawBitmap(m_destBitmap, getImageMatrix(), null);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		//Log.d(TAG, "Size changed: w=" + w + ", h=" + h + ", oldw=" + oldw + ", oldh=" + oldh);
		m_transState.updateMatrix();
	}
	
	public void setSourceBitmap(final Bitmap sourceBitmap) {
		m_srcBitmap = sourceBitmap;
		invalidate();
	}
	
	public void setWrapMode(final ConformLib.WrapMode wrapMode) {
		m_wrapMode = wrapMode;
		invalidate();
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return m_touchHandler.onTouchEvent(event);
	}

}

package org.mtc.conform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;


public class BitmapperView extends ImageView {

	public static final String TAG = "BitmapperView";
	
	private Bitmap m_srcBitmap;	
	private Bitmap m_destBitmap = null;
	
	private int m_destWidth = 256;
	private int m_destHeight = 256;
	
	private Matrix m_screenToUnitSquareMatrix = new Matrix();
	
	private float[] pos = new float[] {0.5f, 0.5f};
	
	private ConformLib.WrapMode m_wrapMode = ConformLib.WrapMode.TILE;
	
	private ScaleGestureDetector m_scaleGestureDetector;

	
	private float[] panCurr = new float[] {0.0f, 0.0f};

	private float m_scaleFac = 1.0f;
	
	long start;
	int count;
	
	public BitmapperView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setSourceBitmap(((BitmapDrawable) getDrawable()).getBitmap());
		m_destBitmap = Bitmap.createBitmap(m_destWidth, m_destHeight, Config.ARGB_8888);
		setImageBitmap(m_destBitmap);
		m_scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.OnScaleGestureListener(){
			@Override
			public boolean onScaleBegin(ScaleGestureDetector detector) {
				return true;
			}
			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				m_scaleFac *= detector.getScaleFactor();
				return true;
			}
			@Override
			public void onScaleEnd(ScaleGestureDetector detector) {
			}
		});
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		m_destBitmap.eraseColor(0);
//		if (count == 0)
//			start = System.currentTimeMillis();
		ConformLib.get().pullbackBitmaps(m_srcBitmap, m_destBitmap, pos[0], pos[1], panCurr[0], panCurr[1], m_scaleFac, m_wrapMode.getConstant());
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
		m_screenToUnitSquareMatrix.reset();
	}
	
	public void setSourceBitmap(final Bitmap sourceBitmap) {
		m_srcBitmap = sourceBitmap;
		m_screenToUnitSquareMatrix.reset();
		pos[0] = 0.5f;
		pos[1] = 0.5f;
		invalidate();
	}
	
	public void setWrapMode(final ConformLib.WrapMode wrapMode) {
		m_wrapMode = wrapMode;
		invalidate();
	}
	
	private Matrix getScreenToUnitSquareMatrix() {
		if (m_screenToUnitSquareMatrix.isIdentity()) {
			getImageMatrix().invert(m_screenToUnitSquareMatrix);
			m_screenToUnitSquareMatrix.postScale(1.0f/m_destWidth, 1.0f/m_destHeight);
		}
		return m_screenToUnitSquareMatrix;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final Matrix toUnitSqMat = getScreenToUnitSquareMatrix();
		m_scaleGestureDetector.onTouchEvent(event);
		if (m_scaleGestureDetector.isInProgress()) {
			panCurr[0] = m_scaleGestureDetector.getFocusX();
			panCurr[1] = m_scaleGestureDetector.getFocusY();
			toUnitSqMat.mapPoints(panCurr);
			invalidate();
			return true;
		} else {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				pos[0] = event.getX();
				pos[1] = event.getY();
				toUnitSqMat.mapPoints(pos);
				invalidate();
				return true;
			}
			return super.onTouchEvent(event);
		}
	}

}

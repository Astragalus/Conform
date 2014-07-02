package org.mtc.conform;

import java.util.Observable;
import java.util.Observer;

import org.mtc.conform.math.Complex;
import org.mtc.conform.math.ComplexAffineTrans;
import org.mtc.conform.math.ComplexArray;
import org.mtc.conform.math.ComplexArray.ComplexElement;
import org.mtc.conform.math.IComplex;
import org.mtc.conform.math.IComplexActor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.widget.ImageView;
import android.widget.Toast;

public class BitmapperView extends ImageView {

	public static final String TAG = "Conform";
	
	private static class ParamDrawer implements IComplexActor {
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

	public class ParamHolder implements Observer {
		public final static int MAX_PARAMS = 6;
		public final static float RADIUS = 100.0f;

		public ParamHolder(final TransformationState transStateHolder) {
			m_screenCoords = new ComplexArray(MAX_PARAMS);
			m_normCoords = new ComplexArray(MAX_PARAMS);
			m_trans = transStateHolder;
			m_trans.addObserver(this);
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
		
		public void applyScreenCoords(final IComplexActor action) {
			m_screenCoords.apply(action);
		}
		
		public void applyNormCoords(final IComplexActor action) {
			m_normCoords.apply(action);
		}
		
		public ComplexElement findParamNearCoords(float scrX, float scrY) {
			ComplexElement result = null;
			final Complex at = new Complex(scrX, scrY);
			for (IComplex param : m_screenCoords) {
				if (param.distSq(at) <= RADIUS*RADIUS) {
					result = (ComplexElement) param;
					break;
				} 
			}
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
		
		/**
		 * TransformationState has changed.
		 */
		@Override
		public void update(Observable observable, Object data) {
			updateScreenCoords();
		}
		
		public ComplexArray getNormalizedParams() {
			return m_normCoords;
		}
		
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("Params").append('(').append(size()).append(')');
			sb.append("Normal[");
			for (IComplex norm : m_normCoords) {
				sb.append(norm.toString()).append(' ');
			}
			sb.append(']');
			
			sb.append("Screen[");
			for (IComplex scr : m_screenCoords) {
				sb.append(scr.toString()).append(' ');
			}
			sb.append(']');
			return sb.toString();
		}
		
		final private ComplexArray m_normCoords;
		final private ComplexArray m_screenCoords;
		final private TransformationState m_trans;
	}
	
	private class TransformationState extends Observable {
		final private Matrix m_screenToSquareMat = new Matrix();
		final private Matrix m_squareToScreenMat = new Matrix();
		final private ComplexAffineTrans m_currTrans = ComplexAffineTrans.IDENT;
		final private ComplexElement m_pivot = new ComplexArray(1,1).front().assignFrom(IComplex.ZERO);
		final private ComplexElement m_translate = new ComplexArray(1,1).front().assignFrom(IComplex.ZERO);
		final private IComplexActor m_fwdTransformer = new IComplexActor() {
			@Override
			public void actOn(IComplex param) {
				m_currTrans.apply(param);
			}			
		};
		final private IComplexActor m_invTransformer = new IComplexActor() {			
			@Override
			public void actOn(IComplex param) {
				m_currTrans.applyInverse(param);
			}			
		};

		public TransformationState() {
			clearChanged();
		}
		
		public void updateMatrices() {
			m_squareToScreenMat.set(getImageMatrix());
			m_squareToScreenMat.preScale(m_drawWidth, m_drawHeight);
			m_squareToScreenMat.preScale(0.5f, 0.5f, 1.0f, 1.0f);			
			getImageMatrix().invert(m_screenToSquareMat);
			m_screenToSquareMat.postScale(1.0f/m_drawWidth, 1.0f/m_drawHeight);
			m_screenToSquareMat.postScale(2.0f, 2.0f, 1.0f, 1.0f);
			setChanged();
			notifyObservers();
			setChanged();
		}
		public void scale(final float s, final float x, final float y) {
			m_currTrans.postMult(ComplexAffineTrans.scaling(s, screenToNormalizedPoint(m_pivot.re(x).im(y))));
			notifyObservers();
			setChanged();
		}
		public void translate(final float x, final float y) {
			m_currTrans.postMult(ComplexAffineTrans.translation(screenToNormalizedVector(m_translate.re(-x).im(-y))));
			notifyObservers();
			setChanged();
		}
		public ComplexElement screenToNormalizedVector(ComplexElement srcdst) {
			final ComplexArray backingArray = srcdst.getParent();
			m_screenToSquareMat.mapVectors(backingArray.arr, 0, backingArray.arr, 0, 1);
			return srcdst;
		}
		public ComplexElement screenToNormalizedPoint(ComplexElement srcdst) {
			final ComplexArray backingArray = srcdst.getParent();
			m_screenToSquareMat.mapPoints(backingArray.arr, 0, backingArray.arr, 0, 1);
			return srcdst;
		}
		public void screenToNormalizedPoints(final ComplexArray src, final ComplexArray dst) {			
			m_screenToSquareMat.mapPoints(dst.arr, 0, src.arr, 0, src.size);
			dst.apply(m_invTransformer);
		}
		public void screenToNormalizedPoint(final ComplexElement src, final ComplexElement dst) {			
			m_screenToSquareMat.mapPoints(dst.getParent().arr, dst.getIndex(), src.getParent().arr, src.getIndex(), 1);
			m_invTransformer.actOn(dst);
		}
		public void normalizedToScreenPoints(final ComplexArray src, final ComplexArray dst) {
			dst.copyFrom(src).apply(m_fwdTransformer);
			m_squareToScreenMat.mapPoints(dst.arr, 0, dst.arr, 0, dst.size);
		}
	};

	private class BitmapperTouchHandler extends SimpleOnGestureListener implements OnScaleGestureListener {
		private final ScaleGestureDetector m_zoomDetector;
		private final GestureDetector m_gestureDetector;
		private final TransformationState m_state;
		private final SparseArray<ComplexElement> m_ptrIdToParam;
		
		public BitmapperTouchHandler(final Context context, final TransformationState state) {
			m_zoomDetector = new ScaleGestureDetector(context, this);
			m_gestureDetector = new GestureDetector(getContext(), this);
			m_ptrIdToParam = new SparseArray<ComplexElement>(5);
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
			boolean eventConsumed = false;
			final int numPtrs = event.getPointerCount();
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				for (int i = 0; i < numPtrs; ++i) {
					final ComplexElement param = m_paramHolder.findParamNearCoords(event.getX(i), event.getY(i));
					if (param != null) {
						m_ptrIdToParam.put(event.getPointerId(i), param);
						eventConsumed |= true;
					}
				}
				break;
			case MotionEvent.ACTION_MOVE:
				for (int i = 0; i < numPtrs; ++i) {
					final ComplexElement param = m_ptrIdToParam.get(event.getPointerId(i));
					if (param != null) {
						m_paramHolder.setParamScreenCoords(param, event.getX(i), event.getY(i));
						eventConsumed |= true;
					}
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				for (int i = 0; i < numPtrs; ++i) {
					m_ptrIdToParam.remove(event.getPointerId(i));
				}
				break;
			case MotionEvent.ACTION_CANCEL:
				m_ptrIdToParam.clear();
				break;
			}
			return eventConsumed;
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
		@Override
		public void onShowPress(MotionEvent e) {
			final ComplexElement param = m_paramHolder.findParamNearCoords(e.getX(), e.getY());
			if (param != null) {
				final Toast toast = Toast.makeText(getContext(), Complex.toString(param), Toast.LENGTH_LONG);
				toast.setGravity(Gravity.BOTTOM, 0, 0);
				toast.show();
			}
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
	
	private final ParamDrawer m_poleDrawer;
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
		paint.setARGB(255, 255, 55, 178);
		m_poleDrawer = new ParamDrawer(paint, RADIUS);
	}
	
	@Override
	protected void onDraw(final Canvas canvas) {	
//		if (count == 0)
//			start = System.currentTimeMillis();
		if (m_transState.hasChanged()) {
			m_destBitmap.eraseColor(0);
			ConformLib.INSTANCE.pullback(m_srcBitmap, m_destBitmap, m_paramHolder.getNormalizedParams(), m_transState.m_currTrans, m_wrapMode);
			canvas.drawBitmap(m_destBitmap, getImageMatrix(), null);
			m_paramHolder.applyScreenCoords(m_poleDrawer.setCanvas(canvas));
		}
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
	
	public void setWrapMode(final ConformLib.WrapMode wrapMode) {
		m_wrapMode = wrapMode;
		Log.i(TAG, "Wrap mode set to: " + wrapMode.name());
		invalidate();
	}
	
	public void setTouchMode(final TouchMode touchMode) {
		m_touchMode = touchMode;
		Log.i(TAG, "touch mode  [" + touchMode.name() + "]");
		Log.i(TAG, "normParams  [" + m_paramHolder.m_normCoords + "]");
		Log.i(TAG, "screenParams[" + m_paramHolder.m_screenCoords + "]");
		Log.i(TAG, "currTrans   [" + m_transState.m_currTrans + "]");		
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

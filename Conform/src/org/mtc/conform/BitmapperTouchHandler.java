package org.mtc.conform;

import org.mtc.conform.BitmapperView.BitmapperMode;
import org.mtc.conform.BitmapperView.ParamHolder;
import org.mtc.conform.math.Complex;
import org.mtc.conform.math.ComplexArray.ComplexElement;

import android.content.Context;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.widget.Toast;

public class BitmapperTouchHandler extends SimpleOnGestureListener implements OnScaleGestureListener {
	private final Context m_context;
	private final ScaleGestureDetector m_zoomDetector;
	private final GestureDetector m_gestureDetector;
	private final TransformationState m_state;
	private final BitmapperMode m_touchMode;
	private final ParamHolder m_paramHolder;
	private final SparseArray<ComplexElement> m_ptrIdToParam;
	
	public BitmapperTouchHandler(final Context context, final TransformationState state, final BitmapperMode touchMode, final ParamHolder paramHolder) {
		m_context = context;
		m_zoomDetector = new ScaleGestureDetector(context, this);
		m_gestureDetector = new GestureDetector(context, this);
		m_ptrIdToParam = new SparseArray<ComplexElement>(5);
		m_state = state;
		m_touchMode = touchMode;
		m_paramHolder = paramHolder;
	}
	public boolean onTouchEvent(MotionEvent event) {
		boolean processed = false;
		switch (m_touchMode.getTouchMode()) {
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
			final Toast toast = Toast.makeText(m_context, Complex.toString(param), Toast.LENGTH_LONG);
			toast.setGravity(Gravity.BOTTOM, 0, 0);
			toast.show();
		}
	}
}

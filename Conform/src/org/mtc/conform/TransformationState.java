package org.mtc.conform;

import org.mtc.conform.math.ComplexAffineTrans;
import org.mtc.conform.math.ComplexArray;
import org.mtc.conform.math.ComplexArray.ComplexElement;
import org.mtc.conform.math.IComplex;
import org.mtc.conform.math.ComplexArray.IComplexAction;

import android.graphics.Matrix;
import android.widget.ImageView;

public class TransformationState {
	final private ImageView m_view;
	final private int m_drawWidth;
	final private int m_drawHeight;
	final private Matrix m_screenToSquareMat = new Matrix();
	final private Matrix m_squareToScreenMat = new Matrix();
	final private ComplexAffineTrans m_currTrans = ComplexAffineTrans.IDENT;
	final private ComplexElement m_pivot = new ComplexArray(1,1).front().assignFrom(IComplex.ZERO);
	final private ComplexElement m_translate = new ComplexArray(1,1).front().assignFrom(IComplex.ZERO);
	final private IComplexAction m_fwdTransformer = new IComplexAction() {
		@Override
		public void actOn(IComplex param) {
			m_currTrans.apply(param);
		}			
	};
	final private IComplexAction m_invTransformer = new IComplexAction() {			
		@Override
		public void actOn(IComplex param) {
			m_currTrans.applyInverse(param);
		}			
	};
	private OnTransformStateChangedListener m_listener;
	
	public static interface OnTransformStateChangedListener {
		public void onTransformStateChanged();
	}

	public TransformationState(final ImageView view, final int drawWidth, final int drawHeight) {
		m_view = view;
		m_drawWidth = drawWidth;
		m_drawHeight = drawHeight;
	}
	
	public void setOnTransformStateChangedListener(OnTransformStateChangedListener listener) {
		m_listener = listener;
	}
	
	private void updateTranformStateListener() {
		if (m_listener != null) {
			m_listener.onTransformStateChanged();
		}
	}
	
	public void updateMatrices() {
		m_squareToScreenMat.set(m_view.getImageMatrix());
		m_squareToScreenMat.preScale(m_drawWidth, m_drawHeight);
		m_squareToScreenMat.preScale(0.5f, 0.5f, 1.0f, 1.0f);			
		m_view.getImageMatrix().invert(m_screenToSquareMat);
		m_screenToSquareMat.postScale(1.0f/m_drawWidth, 1.0f/m_drawHeight);
		m_screenToSquareMat.postScale(2.0f, 2.0f, 1.0f, 1.0f);
		updateTranformStateListener();
	}
	public ComplexAffineTrans getCurrTrans() {
		return m_currTrans;
	}
	public void scale(final float s, final float x, final float y) {
		m_currTrans.postMult(ComplexAffineTrans.scaling(s, screenToNormalizedPoint(m_pivot.re(x).im(y))));
		updateTranformStateListener();
	}
	public void translate(final float x, final float y) {
		m_currTrans.postMult(ComplexAffineTrans.translation(screenToNormalizedVector(m_translate.re(-x).im(-y))));
		updateTranformStateListener();
	}
	public ComplexElement screenToNormalizedVector(ComplexElement srcdst) {
		final ComplexArray backingArray = srcdst.getParent();
		m_screenToSquareMat.mapVectors(backingArray.arr, 0, backingArray.arr, 0, 1);
		return srcdst;
	}
	public ComplexElement screenToNormalizedPoint(ComplexElement srcdst) {
		final ComplexArray backingArray = srcdst.getParent();
		m_screenToSquareMat.mapPoints(backingArray.arr, 0, backingArray.arr, 0, 1);
		m_invTransformer.actOn(srcdst);
		return srcdst;
	}
	public void screenToNormalizedPoints(final ComplexArray src, final ComplexArray dst) {			
		m_screenToSquareMat.mapPoints(dst.arr, 0, src.arr, 0, src.size);
		dst.apply(m_invTransformer);
	}
	public void screenToNormalizedPoint(final ComplexElement src, final ComplexElement dst) {			
		m_screenToSquareMat.mapPoints(dst.getParent().arr, dst.getIndex()<<1, src.getParent().arr, src.getIndex()<<1, 1);
		m_invTransformer.actOn(dst);
	}
	public void normalizedToScreenPoints(final ComplexArray src, final ComplexArray dst) {
		dst.copyFrom(src).apply(m_fwdTransformer);
		m_squareToScreenMat.mapPoints(dst.arr, 0, dst.arr, 0, dst.size);
	}

}

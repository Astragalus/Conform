package org.mtc.conform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;


public class BitmapperView extends ImageView {

	public static final String TAG = "BitmapperView";
	
	private Bitmap m_srcImage;
	
	private Bitmap m_destImage = null;
	
	private int m_destWidth = 256;
	private int m_destHeight = 256;
	
	private Matrix m_screenToUnitSquareMatrix = new Matrix();
	
	private float[] pos = new float[] {0.5f, 0.5f};
	
	public BitmapperView(Context context, AttributeSet attrs) {
		super(context, attrs);
		final Drawable viewDrawable = getDrawable();
		Bitmap viewBitmap = null;
		if (viewDrawable != null && viewDrawable instanceof BitmapDrawable) {
			viewBitmap = ((BitmapDrawable)viewDrawable).getBitmap();
		}
		setSourceBitmap(viewBitmap != null ? Bitmap.createBitmap(viewBitmap) : BitmapFactory.decodeResource(getResources(), R.drawable.celtic));
		m_destImage = Bitmap.createBitmap(m_destWidth, m_destHeight, Config.ARGB_8888);
		setImageBitmap(m_destImage);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		m_destImage.eraseColor(0);
		ConformLib.get().pullbackBitmaps(m_srcImage, m_destImage, pos[0], pos[1]);
		canvas.drawBitmap(m_destImage, getImageMatrix(), null);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		m_screenToUnitSquareMatrix.reset();
	}
	
	public void setSourceBitmap(final Bitmap sourceBitmap) {
		m_srcImage = sourceBitmap;
		m_screenToUnitSquareMatrix.reset();
		pos[0] = 0.5f;
		pos[1] = 0.5f;
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
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			pos[0] = event.getX();
			pos[1] = event.getY();
			getScreenToUnitSquareMatrix().mapPoints(pos);
			invalidate();
			return true;
		}
		return super.onTouchEvent(event);
	}

}

package org.mtc.conform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice.MotionRange;
import android.view.MotionEvent;
import android.widget.ImageView;


public class BitmapperView extends ImageView {

	public static final String TAG = "BitmapperView";
	
	final private Bitmap m_srcImage;
	
	private Bitmap m_destImage = null;
	
	private volatile float m_x = 0.5f;
	private volatile float m_y = 0.5f;
	
	public BitmapperView(Context context, AttributeSet attrs) {
		super(context, attrs);
		final Drawable viewDrawable = getDrawable();
		Bitmap viewBitmap = null;
		if (viewDrawable != null && viewDrawable instanceof BitmapDrawable) {
			viewBitmap = ((BitmapDrawable)viewDrawable).getBitmap();
		}
		m_srcImage = viewBitmap != null ? Bitmap.createBitmap(viewBitmap) : BitmapFactory.decodeResource(getResources(), R.drawable.celtic);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		setImageBitmap(map(getDestBitmap(256, 256)));
		super.onDraw(canvas);
	}
	
	protected synchronized Bitmap getDestBitmap(final int w, final int h) {
		if (m_destImage != null) {
			if (m_destImage.getWidth() != w || m_destImage.getHeight() != h) {
				m_destImage.recycle();
				m_destImage = null;
			}
		}
		if (m_destImage == null) {
			m_destImage = Bitmap.createBitmap(w, h, Config.ARGB_8888);
		}
		return m_destImage;
	}
	
	protected synchronized Bitmap map(final Bitmap destImage) {
		ConformLib.get().pullbackBitmaps(m_srcImage, destImage, m_x, m_y);
		return destImage;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			final MotionRange xMotionRange = event.getDevice().getMotionRange(MotionEvent.AXIS_X);
			m_x = (event.getX() - xMotionRange.getMin())/xMotionRange.getRange();
			final MotionRange yMotionRange = event.getDevice().getMotionRange(MotionEvent.AXIS_Y);
			m_y = (event.getY() - yMotionRange.getMin())/yMotionRange.getRange();
			this.invalidate();
			Log.i(TAG,"Touch event, x="+m_x+",y="+m_y);
			return true;
		}
		return super.onTouchEvent(event);
	}

}

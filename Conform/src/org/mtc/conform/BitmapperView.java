package org.mtc.conform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;


public class BitmapperView extends ImageView {

	final private Bitmap m_srcImage;
	
	private Bitmap m_destImage = null;
	
	public BitmapperView(Context context, AttributeSet attrs) {
		super(context, attrs);
		final Drawable viewDrawable = getDrawable();
		Bitmap viewBitmap = null;
		if (viewDrawable != null && viewDrawable instanceof BitmapDrawable) {
			viewBitmap = ((BitmapDrawable)viewDrawable).getBitmap();
		}
		m_srcImage = viewBitmap != null ? Bitmap.createBitmap(viewBitmap) : BitmapFactory.decodeResource(getResources(), R.drawable.celtic);
		setImageBitmap(map(getDestBitmap(m_srcImage.getWidth()/2, m_srcImage.getHeight()/2)));
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
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
		ConformLib.get().pullbackBitmaps(m_srcImage, destImage);
		return destImage;
	}

}

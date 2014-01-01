package org.mtc.conform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 */
public class PlaneView extends SurfaceView implements SurfaceHolder.Callback{
	private Bitmap m_sourceBitmap;
	private Bitmap m_drawnBitmap;
	private final int m_defaultBitmapId;
	class PlaneThread extends Thread {

		private final SurfaceHolder m_holder;
		private int m_width = 1;
		private int m_height = 1;
		private boolean m_running = false;
		private final Object m_runLock = new Object();

		public PlaneThread(SurfaceHolder holder, Context context, AttributeSet attrs) {
			m_holder = holder;
		}

		public void setRunning(final boolean running) {
			synchronized(m_runLock) {
				m_running = running;
			}
		}
		public void setSurfaceSize(int width, int height) {
			synchronized(m_holder) {
				if (m_width != width || m_height != height) {
					m_width = width;
					m_height = height;
					if (m_drawnBitmap != null) {
						m_drawnBitmap.recycle();
					}
					m_drawnBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
					
				}
			}
		}

		
		private void doDraw(Canvas c) {
			ConformLib.pullbackBitmaps(m_sourceBitmap, m_drawnBitmap);
			c.drawBitmap(m_drawnBitmap, getMatrix(), null);
		}
		
		@Override
		public void run() {
			while(m_running) {
				Canvas c = null;
				try {
					c = m_holder.lockCanvas();
					synchronized(m_holder) {
						synchronized(m_runLock) {
							if (m_running) {
								doDraw(c);
							}
						}
					}
				} finally {
					if (c != null) {
						m_holder.unlockCanvasAndPost(c);
					}
				}
			}
		}
	}

	private PlaneThread m_thread; 
	
	public PlaneView(Context context, AttributeSet attrs) {
		super(context, attrs);
		m_defaultBitmapId = attrs.getAttributeResourceValue(R.attr.defaultDrawable, R.drawable.celtic);
		final SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		m_thread = new PlaneThread(holder, context, attrs);
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		m_sourceBitmap = BitmapFactory.decodeResource(getResources(), m_defaultBitmapId);
		final Rect surfRect = holder.getSurfaceFrame();
		m_thread.setRunning(true);
		m_thread.start();
		m_thread.setSurfaceSize(surfRect.width(), surfRect.height());
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		m_thread.setSurfaceSize(width, height);
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        m_thread.setRunning(false);
        while (retry) {
            try {
                m_thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
		m_sourceBitmap.recycle();
		if (m_drawnBitmap != null) {
			m_drawnBitmap.recycle();
			m_drawnBitmap = null;
		}
	}
}

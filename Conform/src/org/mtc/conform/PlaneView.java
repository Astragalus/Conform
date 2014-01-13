package org.mtc.conform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AndroidRuntimeException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 */
public class PlaneView extends SurfaceView implements SurfaceHolder.Callback{
	public final static String TAG = "PlaneView";
	private Bitmap m_sourceBitmap;
	private Bitmap m_drawnBitmap;
	private final int m_defaultBitmapId;
	
	private volatile float m_x = 0.5f;
	private volatile float m_y = 0.5f;
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
			Log.v(TAG,"drawing...");
			ConformLib.get().pullbackBitmaps(m_sourceBitmap, m_drawnBitmap, m_x, m_y);
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
		m_defaultBitmapId = R.drawable.celtic;
		final SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		m_thread = new PlaneThread(holder, context, attrs);
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG,"surfaceCreated called");
		m_sourceBitmap = BitmapFactory.decodeResource(getResources(), m_defaultBitmapId);
		if (m_sourceBitmap == null) {
			Log.e(TAG,"Error decoding source bitmap!");
			throw new AndroidRuntimeException("Error decoding source bitmap!");
		} else {
			Log.i(TAG, "Decoded source bitmap - byte count = " + m_sourceBitmap.getByteCount());
		}
		final Rect surfRect = holder.getSurfaceFrame();
		Log.i(TAG,"surfaceCreated: starting draw thread; initial size = " + surfRect + "");
		m_thread.setRunning(true);
		m_thread.start();
		m_thread.setSurfaceSize(surfRect.width(), surfRect.height());
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.i(TAG,"surfaceChanged called with width = " + width + ", height = " + height);
		m_thread.setSurfaceSize(width, height);
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(TAG,"surfaceDestroyed called");
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
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			m_x = event.getX()/event.getDevice().getMotionRange(MotionEvent.AXIS_X).getRange();
			m_y = event.getY()/event.getDevice().getMotionRange(MotionEvent.AXIS_Y).getRange();
			return true;
		}
		return false;
	}
}

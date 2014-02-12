package org.mtc.conform;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class ConformActivity extends Activity {
	public final static String TAG = "Conform";

	private final static int IMAGE_PICK = 31415;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conform);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	final MenuInflater menuInflater = getMenuInflater();
    	menuInflater.inflate(R.menu.options_menu,  menu);
    	return true;
    }
    
    private boolean setWrapMode(final ConformLib.WrapMode wrapMode) {
		final View planeView = findViewById(R.id.bitmapperView);
		((BitmapperView)planeView).setWrapMode(wrapMode);
		return true;
    }
    
    private boolean setTouchMode(final BitmapperView.TouchMode touchMode) {
    	final View planeView = findViewById(R.id.bitmapperView);
    	((BitmapperView)planeView).setTouchMode(touchMode);
    	return touchMode == BitmapperView.TouchMode.PAN;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {    	
    	boolean handledEvent = false;
    	switch (item.getItemId()) {
    	case R.id.loadImage:
    		final Intent imagePickIntent = new Intent(Intent.ACTION_GET_CONTENT);
    		imagePickIntent.setType("image/*");
    		startActivityForResult(imagePickIntent, IMAGE_PICK);
    		handledEvent = true;
    		break;
    	case R.id.touchMode:
    		item.setChecked((!item.isChecked() && setTouchMode(BitmapperView.TouchMode.PAN)) || setTouchMode(BitmapperView.TouchMode.PARAM));
    		handledEvent = true;
    		break;
    	case R.id.shouldTile:
    		item.setChecked(!item.isChecked() && setWrapMode(ConformLib.WrapMode.TILE));
    		handledEvent = true;
    		break;
    	case R.id.shouldClamp:
    		item.setChecked(!item.isChecked() && setWrapMode(ConformLib.WrapMode.CLAMP));
    		handledEvent = true;
    		break;
    	}
    	return handledEvent || super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	switch(requestCode) {
    	case IMAGE_PICK:
    		if (resultCode == RESULT_OK) {
    			 try {
					final Bitmap requestedBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()));
					final View planeView = findViewById(R.id.bitmapperView);
					((BitmapperView)planeView).setSourceBitmap(requestedBitmap);
				} catch (Exception e) {
					Log.e(TAG, "Error loading image",e);
				}
    			 
    		}
    	}
    }
}

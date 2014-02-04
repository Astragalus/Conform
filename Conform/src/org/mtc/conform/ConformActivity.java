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
	public final static String TAG = "ConformActivity";

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
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.loadImage:
    		final Intent imagePickIntent = new Intent(Intent.ACTION_GET_CONTENT);
    		imagePickIntent.setType("image/*");
    		startActivityForResult(imagePickIntent, IMAGE_PICK);
    		return true;
    	case R.id.shouldTile:
    		if (!item.isChecked()) {
				final View planeView = findViewById(R.id.planeView);
				((BitmapperView)planeView).setWrapMode(ConformLib.TILE);
    		}
			return true;
    	case R.id.shouldClamp:
    		if (!item.isChecked()) {
    			final View planeView = findViewById(R.id.planeView);
    			((BitmapperView)planeView).setWrapMode(ConformLib.CLAMP);
    		}
    		return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	switch(requestCode) {
    	case IMAGE_PICK:
    		if (resultCode == RESULT_OK) {
    			 try {
					final Bitmap requestedBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()));
					final View planeView = findViewById(R.id.planeView);
					((BitmapperView)planeView).setSourceBitmap(requestedBitmap);
				} catch (Exception e) {
					Log.e(TAG, "Error loading image",e);
				}
    			 
    		}
    	}
    }
}

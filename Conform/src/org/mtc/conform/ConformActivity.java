/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.mtc.conform;

import org.mtc.conform.BitmapperView.BitmapperMode.Mode;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class ConformActivity extends Activity {
	public final static String TAG = "Conform";

	private final static int IMAGE_PICK = 31415;
	
	private static int outputFileUid = 1;
	
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
    
    private BitmapperView getBitmapperView() {
		return (BitmapperView) findViewById(R.id.bitmapperView);
    }
    
    private boolean setWrapMode(final ConformLib.WrapMode wrapMode) {
		getBitmapperView().setWrapMode(wrapMode);
		return true;
    }
    
    private boolean setTouchMode(final Mode touchMode) {
    	getBitmapperView().setTouchMode(touchMode);
    	return touchMode == Mode.CODOMAIN;
    }
    
    private boolean addParam() {
    	getBitmapperView().addParam();
    	return true;
    }

    private boolean removeParam() {
    	getBitmapperView().removeParam();
    	return true;
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
    	case R.id.saveImage:
    		savePullbackImage(getBitmapperView().getImageBitmap());
    		handledEvent = true;
    		break;
    	case R.id.touchMode:
    		item.setChecked((!item.isChecked() && setTouchMode(Mode.CODOMAIN)) || setTouchMode(Mode.DOMAIN));
    		handledEvent = true;
    		break;
    	case R.id.addParamMenuItem:
    		addParam();
    		handledEvent = true;
    		break;
    	case R.id.removeParamMenuItem:
    		removeParam();
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
    
    private static String getSavedImageName() {
    	return "conform-saved-" + outputFileUid++;
    }
    
    private void savePullbackImage(final Bitmap bmp) {
    	final String path = Images.Media.insertImage(getContentResolver(), bmp, getSavedImageName(), null);
    	final Uri bmpUri = Uri.parse(path);
		final Intent bmpSaveIntent = new Intent(Intent.ACTION_SEND);
		bmpSaveIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		bmpSaveIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
		bmpSaveIntent.setType("image/png");
		startActivity(Intent.createChooser(bmpSaveIntent, "Save Pullback Image"));
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

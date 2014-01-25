package org.mtc.conform;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

public class ConformActivity extends Activity {
	public final static String TAG = "ConformActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conform);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	return super.onCreateOptionsMenu(menu);
    }
}

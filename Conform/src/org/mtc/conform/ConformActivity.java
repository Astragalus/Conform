package org.mtc.conform;

import android.app.Activity;
import android.os.Bundle;

public class ConformActivity extends Activity {
	public final static String TAG = "ConformActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conform);
    }
}

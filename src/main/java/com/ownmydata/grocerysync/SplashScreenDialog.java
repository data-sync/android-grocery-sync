package com.ownmydata.grocerysync;

import android.app.Dialog;
import android.content.Context;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.data.R;

public class SplashScreenDialog extends Dialog {

	protected ProgressBar splashProgressBar;
	protected TextView splashProgressMessage;

	public SplashScreenDialog(Context context) {
		super(context, R.style.SplashScreenStyle);

		setContentView(R.layout.splashscreen);
		setCancelable(false);
	}

}

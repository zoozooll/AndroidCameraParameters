package com.aaron.cameraparams;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

public class MyTexture extends TextureView {

    private static final String TAG = "MyTexture";

    public MyTexture(Context context) {
        super(context);
    }

    public MyTexture(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyTexture(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(TAG, "onAttachedToWindow");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d(TAG, "onDetachedFromWindow");

    }


}

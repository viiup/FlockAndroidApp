package com.viiup.android.flock.application;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.jni.bitmap_operations.JniBitmapHolder;


/**
 * Created by AbdullahMoyeen on 4/11/16.
 */
public class StartupFragment4 extends Fragment {


    public StartupFragment4() {
    }

    public static StartupFragment4 newInstance() {
        return new StartupFragment4();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.startup_fragment_4, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        Bitmap startupImageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_startup_4);
        final JniBitmapHolder bitmapHolder = new JniBitmapHolder(startupImageBitmap);
        startupImageBitmap.recycle();

        ImageView startupImage = (ImageView) getView().findViewById(R.id.startup_image);
        startupImage.setImageBitmap(bitmapHolder.getBitmapAndFree());
    }
}

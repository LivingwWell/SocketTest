package com.example.sockettest;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class H264Activity extends AppCompatActivity {
    private static String TAG = "H264Activity";
    private SurfaceView h264surface;
    private SurfaceHolder holder;
    private MediaCodecUtil codecUtil;
    private MediaCodec264Thread thread = null;

    private String path = Environment.getExternalStorageDirectory().toString() + "/sintel.h264";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h264);
        h264surface = findViewById(R.id.h264surface);
        initSurface();
    }

    private void initSurface() {
        holder = h264surface.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                    if (codecUtil == null) {
                        codecUtil = new MediaCodecUtil(holder);
                        codecUtil.startCodec();
                    }
                    if (thread == null) {
                        thread = new MediaCodec264Thread(codecUtil, path);
                        thread.start();
                    }
                    Log.d(TAG, "surfaceCreated: " + thread.getState());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged: " + thread.getState());
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (codecUtil != null) {
                    codecUtil.stopCodec();
                    codecUtil = null;
                }
                if (thread != null && thread.isAlive()) {
                    thread.stopThread();
                    thread = null;
                }
                Log.d(TAG, "surfaceDestroyed: " + thread.getState());
            }
        });
    }
}

package com.example.sockettest;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class H265Activity extends AppCompatActivity {
    private SurfaceView h265surface;
    private PlayerThread playerThread = null;
    private static String TAG = "H265Activity";
    private SurfaceHolder holder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h265);
        h265surface = findViewById(R.id.h265surface);
        holder=h265surface.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (playerThread == null) {
                    playerThread = new PlayerThread(holder.getSurface());
                    playerThread.start();
                }
                Log.d(TAG, "surfaceCreated: " + playerThread.getState());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (playerThread != null) {
                    playerThread.interrupt();
                }
                Log.d(TAG, "surfaceDestroyed: " + playerThread.getState());
            }
        });
    }
}

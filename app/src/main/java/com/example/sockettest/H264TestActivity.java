package com.example.sockettest;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceView;

import java.io.IOException;

public class H264TestActivity extends AppCompatActivity {
    private SurfaceView surfaceView;
    private MediaCodec mediaCodec;
    private MediaFormat mediaFormat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_h264);
        surfaceView=findViewById(R.id.h264surface);
        try {
            mediaCodec=MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

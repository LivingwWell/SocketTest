package com.example.sockettest;

import android.content.Context;
import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketException;

import deep.com.deepudp.DeepUdp;
import deep.com.deepudp.interfaces.UDPCallback;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button button,button2,button3;
    DeepUdp deepUdp;
    private String TAG = "MainActivity";

    //   public static final String FILE_NAME="myFile.txt";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkMediaDecoder();
        button = findViewById(R.id.button);
        button2=findViewById(R.id.button2);
        button3=findViewById(R.id.button3);
        button.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
            }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void checkMediaDecoder() {
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] codecInfos=      mediaCodecList.getCodecInfos();
        for (MediaCodecInfo codecInfo : codecInfos) {
            Log.i("TAG", "codecInfo =" + codecInfo.getName());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button:
                Intent intent=new Intent(MainActivity.this,UdpActiivity.class);
                startActivity(intent);
                break;
            case R.id.button2:
                Intent intent1=new Intent(MainActivity.this,H264Activity.class);
                startActivity(intent1);
                break;
            case R.id.button3:
                Intent intent2=new Intent(MainActivity.this,H265FileDecodeActivity.class);
                startActivity(intent2);
                break;
        }
    }
}

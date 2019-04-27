package com.example.sockettest;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import deep.com.deepudp.DeepUdp;
import deep.com.deepudp.interfaces.UDPCallback;

public class UdpActiivity extends AppCompatActivity implements View.OnClickListener {
    private Button button4,button5;
    DeepUdp deepUdp;
    private static final String TAG="UdpActivity";
    private DatagramSocket datagramSocket;
    private DatagramPacket datagramPacket;
    private boolean isBegin;
    private DatagramSocket socket;
    private static final int PhonePort = 6220;//手机端口号
    private DatagramPacket packet;
    private volatile boolean stopReceiver;
    String filePath = "/sdcard/AIUI/devices.txt";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udp);
        button4=findViewById(R.id.button4);
        button5=findViewById(R.id.button5);
        button4.setOnClickListener(this);
        button5.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button4:
//                new Thread() {
//                    public void run() {
//                        try {
//                            socket = new DatagramSocket(PhonePort);
//                        } catch (SocketException e) {
//                            e.printStackTrace();
//                        }
//                        byte[] receBuf = new byte[1024];
//                        packet = new DatagramPacket(receBuf, receBuf.length);
//                        while (!stopReceiver) {
//                            try {
//                                socket.receive(packet);
//                                String receive = new String(packet.getData(), 0, packet.getLength(), "utf-8");
//                                Log.e("zziafyc", "收到的内容为：" + receive);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//                }.start();
                try {
                    deepUdp = DeepUdp.getInstance().init("192.168.9.204", 6220, 1024);
                } catch (SocketException e) {
                    e.printStackTrace();
                }
                deepUdp.beginReceive(new UDPCallback() {
                    @Override
                    public void callback(String info) {
                        Log.d(TAG, "info: " + info.toString());
                        try {
                            File file = new File(Environment.getExternalStorageDirectory(),
                                    "test.txt");
                            FileOutputStream fos = new FileOutputStream(file);
                            //  String info = "I am a chinanese!";
                            fos.write(info.getBytes());
                            fos.close();
                            System.out.println("写入成功："+info);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                deepUdp.send("test");
                break;
            case R.id.button5:
              //  deepUdp.close();
               //deepUdp.end();
               // isBegin = false;
                //datagramSocket.close();
            //    datagramSocket=null;

                Toast.makeText(UdpActiivity.this,"连接已关闭",Toast.LENGTH_SHORT).show();
                break;
        }
    }

}

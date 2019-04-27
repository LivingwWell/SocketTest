package com.example.sockettest;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class H265FileDecodeActivity extends Activity implements SurfaceHolder.Callback, View.OnClickListener, TextureView.SurfaceTextureListener, SurfaceTexture.OnFrameAvailableListener {

    private SurfaceView testSurfaceView;

    private TextureView textureView;

    private SurfaceHolder holder;
    //文件路径
    private String path0 = Environment.getExternalStorageDirectory() + "/1080p.h265";
    private String path = Environment.getExternalStorageDirectory() + "/mediacodec_1.264";

    private String TAG = "H264FileDecodeActivity";
    private int width, height;
    //解码器
    private MediaCodec mCodec0;
    private boolean isFirst = true;
    private boolean isFirst0 = true;

    // 需要解码的类型
    private final static String MIME_TYPE = "video/avc"; // H264TestActivity.264 Advanced Video
    private final static int TIME_INTERNAL = 5;

    //文件读取完成标识
    private boolean isFinish = false;
    private boolean isFinish0 = false;
    //这个值用于找到第一个帧头后，继续寻找第二个帧头，如果解码失败可以尝试缩小这个值
    private int FRAME_MIN_LEN = 8; //1024;
    //一般H264帧大小不超过200k,如果解码失败可以尝试增大这个值
    private static final int FRAME_MAX_LEN = 300 * 1024;
    //根据帧率获取的解码每帧需要休眠的时间,根据实际帧率进行操作
    private int PRE_FRAME_TIME = 900 / 30;

    //保存完整数据帧
    byte[] frame = new byte[FRAME_MAX_LEN];
    //每次从文件读取的数据
    byte[] readData = new byte[10 * 1024];
    //当前帧长度
    int frameLen = 0;
    int headFirstIndex;
    int headSecondIndex;
    int frameNum;

    //读取文件解码线程
    UpdateDecoder updateDecoderT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.testh264);

        testSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        holder = testSurfaceView.getHolder();
        holder.addCallback(this);

        textureView = (TextureView) findViewById(R.id.textureview);
        textureView.setSurfaceTextureListener(this);

        Button btn = (Button) findViewById(R.id.takePhoto);
        btn.setOnClickListener(this);

        nalu = new NaluUnit();
        isFinish = false;
        isFinish0 = false;
    }

    Handler myHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    byte[] sendBuf = mQueue0.poll();
                    onFrame(sendBuf, 0, sendBuf.length);
                    break;
                case 1:
                    Log.d(TAG,"  onFrame 1  ");
                    onFrame((byte[])msg.obj, 0, msg.arg1);
                    break;
                case 0:
                    Log.d(TAG,"  onFrame 0  ");
                    onFrame((byte[])msg.obj, 0, msg.arg1);
                    break;
                default :
                    break;
            }
            super.handleMessage(msg);
        }
    };


    private  LinkedBlockingQueue<ByteBuffer> mQueue;
    private LinkedBlockingQueue<byte[]> mQueue0;

    int waitC = 0;
    class UpdateDecoder extends Thread{
        private boolean runFlag;
        public void init(){
            //mList = new ArrayList<ByteBuffer>(); //new ArrayList<ByteBuffer>(8)
            mQueue = new LinkedBlockingQueue<ByteBuffer>();
            mQueue0 = new LinkedBlockingQueue<byte[]>();
        }

        public void pushBuf(byte[] buf, int offset, int len)
        {
            //ByteBuffer buffer = ByteBuffer.allocate(len);
            //buffer.put(buf,offset,len);
            //mQueue.offer(buffer);
            byte[] frameD = new byte[len];
            System.arraycopy(frameD, 0, buf, offset, len);
            mQueue0.offer(frameD);
        }

        public void updataStop()
        {
            runFlag = false;
            mQueue0.clear();
        }

        @Override
        public void run() {
            runFlag = true;

            Log.i("UpdateDecoder", " UpdateDecoder  mQueue.size() = "+mQueue0.size());
            while(runFlag){
                if(mQueue0.size() <= 0){
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                while(mQueue0.size() > 0){
                    //ByteBuffer sendBuf = mQueue.poll();
                    //onFrame(sendBuf.array(), 0, sendBuf.capacity());
                    //byte[] sendBuf = mQueue0.poll();
                    try {
                        //onFrame(sendBuf, 0, sendBuf.length);
                        Message message = new Message();
                        message.what = 2;
                        myHandler.sendMessage(message);
                        try {
                            Thread.sleep(40);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        this.width  = width;
        this.height = height;
        Log.i(TAG, "onSurfaceTextureAvailable:  width = " + width + ", height = " + height);
        //startCodec0();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.i(TAG, "onSurfaceTextureSizeChanged: width = " + width + ", height = " + height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        //stopCodec0();
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        width  = holder.getSurfaceFrame().width();
        height = holder.getSurfaceFrame().height();
        Log.i(TAG, "surfaceCreated:  width = " + width + ", height = " + height);
        startCodec0();

        //updateDecoderT = new UpdateDecoder();
        //updateDecoderT.init();
        //updateDecoderT.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCodec0();

        //updateDecoderT.updataStop();
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if(!isFinish0) {
            doDecoder();
            //doDecodec();
        }
    }

    public void startCodec0() {
        if (isFirst0) {
            initDecoder0();
        }
    }

    private class DecoderCallback extends MediaCodec.Callback{
        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            if(nalu.size != 0) {
                ByteBuffer inputBuffer = mCodec0.getInputBuffer(index);
                long timestamp = mCount0++ * 1000000 / 25;
                //Log.i(TAG," nalu type = "+ nalu.type+", nalu.size = "+nalu.size);
                inputBuffer.clear();
                inputBuffer.put(nalu.data, 0, nalu.size);
                mCodec0.queueInputBuffer(index, 0, nalu.size, timestamp, 0);
            }
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
            //ByteBuffer encodedData = codec.getOutputBuffer(index);
            //encodedData.position(info.offset);
            //encodedData.limit(info.offset + info.size);
            codec.releaseOutputBuffer(index, true);
        }

        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
            Log.d(TAG, "Error: " + e);
        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            Log.d(TAG, "encoder output format changed: " + format);
        }
    }

    //h265的nal unit header有两个字节构成
    //hevc的nal包结构与h264有明显的不同，hevc加入了nal所在的时间层的ＩＤ，取去除了nal_ref_idc，此信息合并到了naltype中，
    //通常情况下F为0，layerid为0,  TID为1
    //| F(1bit) | Type(6bits) | LayerId(6bits) | TID (3bits)|
    //H265的NALU类型
    //00 00 00 01 40 01  的nuh_unit_type的值为 32， 语义为视频参数集        VPS
    //00 00 00 01 42 01  的nuh_unit_type的值为 33， 语义为序列参数集         SPS
    //00 00 00 01 44 01  的nuh_unit_type的值为 34， 语义为图像参数集         PPS
    //00 00 00 01 4E 01  的nuh_unit_type的值为 39， 语义为补充增强信息       SEI
    //00 00 00 01 26 01  的nuh_unit_type的值为 19， 语义为可能有RADL图像的IDR图像的SS编码数据   IDR
    //00 00 00 01 02 01  的nuh_unit_type的值为1， 语义为被参考的后置图像，且非TSA、非STSA的SS编码数据

    //常用Nalu Type的定义
    //NAL_UNIT_CODED_SLICE_TRAIL_N= 0,// 0
    //NAL_UNIT_CODED_SLICE_TRAIL_R,   // 1
    //NAL_UNIT_CODED_SLICE_TSA_N,     // 2
    //NAL_UNIT_CODED_SLICE_TLA,       // 3   Current name in the spec: TSA_R
    //NAL_UNIT_CODED_SLICE_STSA_N,    // 4
    //NAL_UNIT_CODED_SLICE_STSA_R,    // 5
    //NAL_UNIT_CODED_SLICE_RADL_N,    // 6
    //NAL_UNIT_CODED_SLICE_DLP,       // 7   Current name in the spec: RADL_R
    //NAL_UNIT_CODED_SLICE_RASL_N,    // 8
    //NAL_UNIT_CODED_SLICE_TFD,       // 9   Current name in the spec: RASL_R
    //NAL_UNIT_CODED_SLICE_BLA,       // 16  Current name in the spec: BLA_W_LP
    //NAL_UNIT_CODED_SLICE_BLANT,     // 17  Current name in the spec: BLA_W_DLP
    //NAL_UNIT_CODED_SLICE_BLA_N_LP,  // 18
    //NAL_UNIT_CODED_SLICE_IDR,       // 19  Current name in the spec: IDR_W_DLP
    //NAL_UNIT_CODED_SLICE_IDR_N_LP,  // 20
    //NAL_UNIT_CODED_SLICE_CRA,       // 21

    //以下NAL_BLA_N_LP到NAL_IDR_W_RADL都算I帧
    //NAL_BLA_N_LP:
    //NAL_BLA_W_LP:
    //NAL_BLA_W_RADL:
    //NAL_CRA_NUT:
    //NAL_IDR_N_LP:
    //NAL_IDR_W_RADL

    /*
    ffmpeg 中 hevc.h定义：
    NAL_TRAIL_N = 0,
    NAL_TRAIL_R = 1,
    NAL_TSA_N = 2,
    NAL_TSA_R = 3,
    NAL_STSA_N = 4,
    NAL_STSA_R = 5,
    NAL_RADL_N = 6,
    NAL_RADL_R = 7,
    NAL_RASL_N = 8,
    NAL_RASL_R = 9,
    NAL_BLA_W_LP = 16,
    NAL_BLA_W_RADL = 17,
    NAL_BLA_N_LP = 18,
    NAL_IDR_W_RADL = 19,
    NAL_IDR_N_LP = 20,
    NAL_CRA_NUT = 21,
    NAL_VPS = 32,
    NAL_SPS = 33,
    NAL_PPS = 34,
    NAL_AUD = 35,
    NAL_EOS_NUT = 36,
    NAL_EOB_NUT = 37,
    NAL_FD_NUT = 38,
    NAL_SEI_PREFIX = 39,
    NAL_SEI_SUFFIX = 40,
    */

    //NALU类型为vps, sps, pps, 或者解码顺序为第一个AU的第一个NALU， 起始码前面再加一个0x00

    private static final int NAL_VPS = 32; // 码流中对应字节值 0x40
    private static final int NAL_SPS = 33; // 0x42
    private static final int NAL_PPS = 34; // 0x44

    private static byte[] getvps_sps_pps(byte[] data, int offset, int length) {
        int i = 0;
        int vps = -1, sps = -1, pps = -1;
        do {
            if (vps == -1) {
                for (i = offset; i < length - 4; i++) {
                    if ((0x00 == data[i]) && (0x00 == data[i + 1]) && (0x00 == data[i + 2])) {
                        byte nal_spec = data[i + 3];
                        //int type = (code & 0x7E)>>1;
                        int nal_type = (nal_spec >> 1) & 0x03f;
                        if (nal_type == NAL_VPS) {
                            // vps found.
                            if (data[i - 1] == 0x00) {  // vps sps pps start with 00 00 00 01 ordinary
                                vps = i - 1;
                            } else {                    // start with 00 00 01
                                vps = i;
                            }
                            break;
                        }
                    }
                }
            }
            if (sps == -1) {
                for (i = vps; i < length - 4; i++) {
                    if ((0x00 == data[i]) && (0x00 == data[i + 1]) && (0x01 == data[i + 2])) {
                        byte nal_spec = data[i + 3];
                        int nal_type = (nal_spec >> 1) & 0x03f;
                        if (nal_type == NAL_SPS) {
                            // vps found.
                            if (data[i - 1] == 0x00) {  // start with 00 00 00 01
                                sps = i - 1;
                            } else {                      // start with 00 00 01
                                sps = i;
                            }
                            break;
                        }
                    }
                }
            }
            if (pps == -1) {
                for (i = sps; i < length - 4; i++) {
                    if ((0x00 == data[i]) && (0x00 == data[i + 1]) && (0x01 == data[i + 2])) {
                        byte nal_spec = data[i + 3];
                        int nal_type = (nal_spec >> 1) & 0x03f;
                        if (nal_type == NAL_PPS) {
                            // vps found.
                            if (data[i - 1] == 0x00) {  // start with 00 00 00 01
                                pps = i - 1;
                            } else {                    // start with 00 00 01
                                pps = i;
                            }
                            break;
                        }
                    }
                }
            }
        } while (vps == -1 || sps == -1 || pps == -1);
        if (vps == -1 || sps == -1 || pps == -1) {// 没有获取成功。
            return null;
        }
        // 计算csd buffer的长度。即从vps的开始到pps的结束的一段数据
        int begin = vps;
        int end = -1;
        for (i = pps; i < length - 4; i++) {
            if ((0x00 == data[i]) && (0x00 == data[i + 1]) && (0x01 == data[i + 2])) {
                if (data[i - 1] == 0x00) {  // start with 00 00 00 01
                    end = i - 1;
                } else {                    // start with 00 00 01
                    end = i;
                }
                break;
            }
        }
        if (end == -1 || end < begin) {
            return null;
        }
        // 拷贝并返回
        byte[] buf = new byte[end - begin];
        System.arraycopy(data, begin, buf, 0, buf.length);
        return buf;
    }

    private void initDecoder0() {
        try {
            //根据需要解码的类型创建解码器
            mCodec0 = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC); //"video/hevc"
        } catch (IOException e) {
            e.printStackTrace();
        }
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height);

        //CSD Codec-specific Data -- 是指跟特定编码算法相关的一些参数，比如AAC的ADTS、H264TestActivity.264的SPS/PPS等
        //H264TestActivity.265，CSD只需要“csd-0”参数，把VPS、SPS、PPS拼接到一起即可   IDR CRA
        //mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);

        /*
        MediaFormat mediaFormat = new MediaFormat();

        int rlen = 0;
        File file = new File(path0);
        if(file.exists()){
            FileInputStream fis = null;
            try {
               fis = new FileInputStream(file);
                rlen = fis.read(frame, 0, frame.length);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        byte[] csd0 = getvps_sps_pps(frame, 0, Math.min(rlen, 200));//Math.min(length, 200));
        if (csd0 == null) {
            try {
                throw new IOException("parse vps sps pps error...");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        ByteBuffer csd0bf = ByteBuffer.allocate(csd0.length);
        csd0bf.put(csd0);
        csd0bf.clear();
        mediaFormat.setByteBuffer("csd-0", csd0bf);
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, width);
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, height);
        mediaFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_HEVC);
        */

        //SurfaceView
        mCodec0.configure(mediaFormat, holder.getSurface(), null, 0); //直接解码送surface显示

        //开始解码
        mCodec0.start();
        isFirst0 = false;
    }

    public void stopCodec0() {
        try {
            mCodec0.stop();
            mCodec0.release();
            mCodec0 = null;
            isFirst0  = true;
            isFinish0 = true;
        } catch (Exception e) {
            e.printStackTrace();
            mCodec0 = null;
        }
    }

    int mCount0 = 0;
    public void onFrame(byte[] buf, int offset, int length) {
        //-1表示一直等待；0表示不等待；其他大于0的参数表示等待毫秒数
        //Log.e(TAG,"        onFrame start       ");
        int inputBufferIndex = mCodec0.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer =  mCodec0.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(buf, offset, length);
            //解码
            long timestamp = mCount0 * 1000000 / 60;
            mCodec0.queueInputBuffer(inputBufferIndex, 0, length,  timestamp, 0);
            mCount0++;
        }
        //Log.e(TAG,"        onFrame middle      ");
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mCodec0.dequeueOutputBuffer(bufferInfo, 0); //10
        //循环解码，直到数据全部解码完成
        while (outputBufferIndex >= 0) {
            //logger.d("outputBufferIndex = " + outputBufferIndex);
            mCodec0.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mCodec0.dequeueOutputBuffer(bufferInfo, 0);
        }
    }

    NaluUnit nalu;
    public class NaluUnit {
        byte[] data;
        int size;
        int type;

        public NaluUnit() {
            data = new byte[20*1024];
            size = 0;
        }
    }

    public  void  doDecoder()
    {
        final File file = new File(path0);
        if(!file.exists() || !file.canRead()){
            Log.e(TAG,"failed to open h264 file.");
            return;
        }
        Log.e(TAG," path0 = "+path0);
        Log.e(TAG,"        readH264FromFile       ");

        new Thread()
        {
            public void run() {
                int readlen = 0;
                int writelen = 0;
                int i = 0;
                int pos = 0;
                int index = 0;
                int index0 = 0;
                boolean findFlag = false;
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                } catch (FileNotFoundException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                //每次从文件读取的数据
                frameNum = 0;
                long startTime = System.currentTimeMillis();
                while (!isFinish0)
                {
                    try {
                        if(fis.available() > 0) {
                            readlen = fis.read(frame, pos, frame.length-pos);
                            if(readlen<=0) {
                                break;
                            }
                            readlen += pos;

                            i   = 0;
                            pos = 0;
                            writelen = readlen;

                            //while(i < readlen-4) {
                            for (i=0; i <readlen-4; i++) {
                                findFlag = false;
                                index  = 0;
                                index0 = 0;
                                if(frame[i] == 0x00 && frame[i+1] == 0x00 && frame[i+2] == 0x01)
                                {
                                    pos = i;
                                    if(i>0) {
                                        if(frame[i-1] == 0x00) { //start with 0x00 0x00 0x00 0x01
                                            index = 1;
                                        }
                                    }
                                    //while (pos < readlen-4) {
                                    for (pos=i+3; pos<readlen-4; pos++) {
                                        if(frame[pos] == 0x00 && frame[pos+1] == 0x00 && frame[pos+2] == 0x01) {
                                            findFlag = true;
                                            if(frame[pos-1] == 0x00) {//start with 0x00 0x00 0x00 0x01
                                                index0 = 1;
                                            }
                                            break;
                                        }
                                    }
                                    if(findFlag){
                                        nalu.type = (frame[i+3]&0x7E)>>1;
                                        if(index==1) {
                                            i = i - 1;
                                        }
                                        if(index0==1) {
                                            pos = pos - 1;
                                        }

                                        onFrame(frame, i, pos-i); // start code + nalu 送解码器
                                        i = pos;
                                        writelen = i;

                                        Log.i(TAG," nalu type = "+ nalu.type+", nalu.size = "+i);

                                        frameNum++;
                                        //Log.i(TAG," frameNum = "+frameNum);
                                        long time = PRE_FRAME_TIME ;//- (System.currentTimeMillis() - startTime);
                                        if (time > 0) {
//                                            try {
//                                                Thread.sleep(time);
//                                            } catch (InterruptedException e) {
//                                                e.printStackTrace();
//                                            }
                                        }
                                        startTime = System.currentTimeMillis();
                                    }else {
                                        writelen = i;
                                        break;
                                    }
                                }
                            }

                            if(writelen>0 && writelen<readlen) {
                                System.arraycopy(frame, writelen, frame, 0, readlen-writelen);
                                //Log.i(TAG, " readlen = "+readlen+", writelen = "+writelen);
                            }

                            pos = readlen-writelen;
                        }else {
                            isFinish0 = true;
                        }
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        Log.e(TAG, "  error =  "+e.getMessage());
                        e.printStackTrace();
                    }
                }
                isFinish0 = false;
                Log.i(TAG, "         frameNum     "+frameNum);
            }
        }.start();
    }

}

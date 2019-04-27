package com.example.sockettest;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaCodecUtil {
    private String TAG = "MediaCodecUtil";
    //解码后显示的surface及其宽高
    private SurfaceHolder holder;
    private int width, height;
    //解码器
    private MediaCodec mCodec;
    private boolean isFirst = true;
    // 需要解码的类型
    private final static String MIME_TYPE = "video/avc"; // H264TestActivity.264 Advanced Video
    private final static int TIME_INTERNAL = 5;

    /**
     * 初始化解码器
     *
     * @param holder 用于显示视频的surface
     * @param width  surface宽
     * @param height surface高
     */
    public MediaCodecUtil(SurfaceHolder holder, int width, int height) {
//        logger.d("MediaCodecUtil() called with: " + "holder = [" + holder + "], " +
//                "width = [" + width + "], height = [" + height + "]");
        this.holder = holder;
        this.width = width;
        this.height = height;
    }

    public MediaCodecUtil(SurfaceHolder holder) {
        this(holder, holder.getSurfaceFrame().width(), holder.getSurfaceFrame().height());
    }

    public void startCodec() {
        if (isFirst) {
            //第一次打开则初始化解码器
            initDecoder();
        }
    }

    private void initDecoder() {
        try {
            //根据需要解码的类型创建解码器
            mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //初始化MediaFormat
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,
                width, height);
        //配置MediaFormat以及需要显示的surface
        mCodec.configure(mediaFormat, holder.getSurface(), null, 0);
        //开始解码
        mCodec.start();
        isFirst = false;
    }

    int mCount = 0;


    public boolean onFrame(byte[] buf, int offset, int length) {
        // 获取输入buffer index
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        //-1表示一直等待；0表示不等待；其他大于0的参数表示等待毫秒数
        int inputBufferIndex = mCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            //清空buffer
            inputBuffer.clear();
            //put需要解码的数据
            inputBuffer.put(buf, offset, length);
            //解码
            mCodec.queueInputBuffer(inputBufferIndex, 0, length, mCount * TIME_INTERNAL, 0);
            mCount++;

        } else {
            return false;
        }
        // 获取输出buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 100);
        //循环解码，直到数据全部解码完成
        while (outputBufferIndex >= 0) {
            //logger.d("outputBufferIndex = " + outputBufferIndex);
            //true : 将解码的数据显示到surface上
            mCodec.releaseOutputBuffer(outputBufferIndex, true);
            outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
        }
        if (outputBufferIndex < 0) {
            //logger.e("outputBufferIndex = " + outputBufferIndex);
        }
        return true;
    }

    /**
     *停止解码，释放解码器
     */
    public void stopCodec() {

        try {
            mCodec.stop();
            mCodec.release();
            mCodec = null;
            isFirst = true;
        } catch (Exception e) {
            e.printStackTrace();
            mCodec = null;
        }
    }
}

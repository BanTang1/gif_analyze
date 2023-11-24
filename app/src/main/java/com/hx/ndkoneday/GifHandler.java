package com.hx.ndkoneday;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * GIF实际处理
 */
public class GifHandler {

    static {
        System.loadLibrary("ndkoneday");
    }

    // C层指针
    private static long gifNativePtr;

    /**
     * 每帧的间隔时间，单位：毫秒   ， Java 方式与C方式都是使用这个间隔时间
     * 注意：自定义GifView（实际上是Movie），在解析图片的时候，更加上层，Android 的 UI 刷新机制以及系统的调度策略都会影响到实际的帧率。
     * 因此无法设置间隔时间为0导致并不是0的效果，而giblib则不存在这个问腿，因为更加底层。
     */
    public static final int FRAME_DURATION = 200;

    public static int totalFrame;     //  C层赋值
    public static int currentFrame; // C层处理， 当前帧数 , C层反射赋值
    private static boolean isOne = true;     // 解析完一遍之后，第二次不再显示解析LOG

    public static void load(String path) {
        gifNativePtr = loadGif(path);
    }

    public static int getWidth(){
        return getWidth(gifNativePtr);
    }

    public static int getHeight(){
        return getHeight(gifNativePtr);
    }

    public static void updateFrame(Bitmap bitmap){
        boolean success = updateFrame(gifNativePtr,bitmap);
        if (success) {
            if (isOne) {
                Log.i("GIF_GIF", "updateFrame: 第"+ currentFrame + "解析成功，共" + totalFrame + "帧");
            }
            if (currentFrame >= totalFrame) {
                isOne = false;
            }
        } else {
            Log.i("GIF_GIF", "updateFrame: 第"+ currentFrame + "解析失败，共" + totalFrame + "帧");
        }
    }

    /**
     * 加载GIF
     * @param path  文件路径
     * @return      指针
     */
    private static native long loadGif(String path);

    /**
     * 获取宽
     * @param gifNativePtr  指针
     * @return    GIF宽度
     */
    private static native int getWidth(long gifNativePtr);

    /**
     * 获取高
     * @param gifNativePtr  指针
     * @return  GIF 高度
     */
    private static native int getHeight(long gifNativePtr);

    /**
     * 渲染图片
     * @param gifNativePtr  指针
     * @param bitmap        Bitmap组件，用于显示
     * @return         是否解析成功
     */
    private static native boolean updateFrame(long gifNativePtr, Bitmap bitmap);


}

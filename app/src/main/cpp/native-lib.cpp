#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/bitmap.h>
extern "C" {
#include "giflib/gif_lib.h"
}

#define TAG "GIF_GIF"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)


// 自定义GifBeen结构体
struct GifBean {
    int current_frame;
    int total_frame;
};

// 存储方式为ABGR ，因此需要将对应的颜色分量放置到对应的位置
#define  argb(a, r, g, b) ( ((a) & 0xff) << 24 ) | ( ((b) & 0xff) << 16 ) | ( ((g) & 0xff) << 8 ) | ((r) & 0xff)

extern "C"
JNIEXPORT jlong JNICALL
Java_com_hx_ndkoneday_GifHandler_loadGif(JNIEnv *env, jclass clazz, jstring path) {
    // 将Java字符串转换为C层字符串
    const char *c_path = env->GetStringUTFChars(path, JNI_FALSE);
    // 打开GIF文件， GifFileType结构体中填充一些基本信息
    int error = 0;
    GifFileType *gifFileType = DGifOpenFileName(c_path, &error);
    // 读取文件中的图像数据，填充到GifFileType中，如：SavedImages 、ImageCount
    DGifSlurp(gifFileType);
    // 开辟内 GifBeen 内存空间
    GifBean *gifBean = static_cast<GifBean *>(malloc(sizeof(GifBean)));
    memset(gifBean, 0, sizeof(GifBean));
    // 添加自定义的额外数据，便于使用
    gifFileType->UserData = gifBean;
    // 当前帧
    gifBean->current_frame = 0;
    // 总帧
    gifBean->total_frame = gifFileType->ImageCount;
    // 释放字符串
    env->ReleaseStringUTFChars(path, c_path);
    return (jlong) (gifFileType);
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_hx_ndkoneday_GifHandler_getWidth(JNIEnv *env, jclass clazz, jlong gif_native_ptr) {
    auto *gifFileType = reinterpret_cast<GifFileType *>(gif_native_ptr);
    return gifFileType->SWidth;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_hx_ndkoneday_GifHandler_getHeight(JNIEnv *env, jclass clazz, jlong gif_native_ptr) {
    auto *gifFileType = reinterpret_cast<GifFileType *>(gif_native_ptr);
    return gifFileType->SHeight;
}


/*
 * 索引相关解释：
 * 假设图像的左上角坐标是 (0, 0)，右下角坐标是 (2, 2)，则图像如下：
        (0,0) | (1,0) | (2,0)
        ------+-------+------
        (0,1) | (1,1) | (2,1)
        ------+-------+------
        (0,2) | (1,2) | (2,2)
假设当前遍历到 (1,1) 这个坐标，根据计算公式：
 pointPixel  = (y-top) * Width + (x-left) = (1 - 0) * 3 + (1 - 0) = 4
 */



void drawFrame(GifFileType *gifFileType, AndroidBitmapInfo info, void *pixels) {
    // 拿到之前自定义 GifBean结构体
    auto *gifBean = static_cast<GifBean *>(gifFileType->UserData);
    // 从所有图像帧中获取到当前帧的图像帧，当前帧里面包括宽度、高度、图像数据等
    SavedImage savedImage = gifFileType->SavedImages[gifBean->current_frame];
    // 图像帧中有两个部分，一部分是描述， 一部分是像素
    GifImageDesc frameInfo = savedImage.ImageDesc;

    // 颜色表
    ColorMapObject *colorMapObject;
    colorMapObject = frameInfo.ColorMap;
    // 检查是否有全局颜色表
    if (gifFileType->SColorMap != nullptr) {
        // 使用全局颜色表
        colorMapObject = gifFileType->SColorMap;
    } else {
        // 如果没有全局颜色表，检查当前帧是否有颜色映射表
        colorMapObject = frameInfo.ColorMap;

        // 如果当前帧也没有颜色映射表，返回
        if (colorMapObject == nullptr) {
            LOGI("ERROR, NOT COLOR MAP");
            return; // 或者采取适当的处理措施
        }
    }

    // 记录每一行的首地址
    int *px = static_cast<int *>(pixels);
    // 临时 索引
    int *line;
    // 实际索引
    int pointPixel;
    // GIF 图像中的字节数据,颜色映射表中的位置,
    // 在 GIF 图像中，图像的像素值通常是索引，而不是直接表示颜色。为了获取实际的颜色信息，需要使用颜色映射表。
    GifByteType gifByteType;
    // RGB 数据
    GifColorType gifColorType;
    //  X为横坐标， Y为纵坐标， 排除图像的偏移量，就是实际的图片横纵坐标
    for (int y = frameInfo.Top; y < frameInfo.Top + frameInfo.Height; ++y) {
        // 每一行的首地址
        line = px;
        // 该行中的每一列,  也就是一行一行的去填充像素
        for (int x = frameInfo.Left; x < frameInfo.Left + frameInfo.Width; ++x) {
            // 定位像素  索引, 上方有相关解释； 计算公式： (y-frameInfo.Top) * frameInfo.Width + (x-frameInfo.Left)
            pointPixel = (y - frameInfo.Top) * frameInfo.Width + (x - frameInfo.Left);
            // savedImage.RasterBits 是一个指向图像数据的指针，它指向一个包含图像像素数据的一维数组
            // pointPixel 是通过之前提到的计算方式计算得到的当前像素在一维数组中的索引。
            // 因此可以在一维数组中得到指定位置的像素值(索引)， 也就是 GifByteType 结构体
            gifByteType = savedImage.RasterBits[pointPixel];
            // 根据索引去映射表中找到相应的颜色
            gifColorType = colorMapObject->Colors[gifByteType];
            // 在行中，对每一个像素点进行颜色的添加
            // 颜色排列方式为： ABGR，因此需要将对应的颜色分量放置在对应的位置上
            line[x] = argb(255, gifColorType.Red, gifColorType.Green, gifColorType.Blue);
        }
        // 跳转到下一行,  AndroidBitmapInfo->stride 中存储了一行在内存中所占的字节数
        px = (int *) ((char *) px + info.stride);
    }
}


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_hx_ndkoneday_GifHandler_updateFrame(JNIEnv *env, jclass clazz, jlong gif_native_ptr, jobject bitmap) {
    auto *gifFileType = reinterpret_cast<GifFileType *>(gif_native_ptr);
    // 获取Bitmap元数据信息，  Java层的Bitmap 在C层本质上就是一个像素数组
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);
    // 像素指针，用于进行像素级的操作
    void *pixels = nullptr;
    // 加锁，保证资源安全
    AndroidBitmap_lockPixels(env, bitmap, &pixels);
    // 绘制
    drawFrame(gifFileType, info, pixels);
    // 解锁
    AndroidBitmap_unlockPixels(env, bitmap);

    // 同步修改自定义GifBeen结构体
    auto *gifBean = static_cast<GifBean *>(gifFileType->UserData);

    // 反射修改GifHandler中的字段值， 便于Java层展示LOG
    jfieldID totalFrameField = env->GetStaticFieldID(clazz, "totalFrame", "I");
    jfieldID currentFrameFiled = env->GetStaticFieldID(clazz,"currentFrame","I");
    if (totalFrameField != nullptr && currentFrameFiled != nullptr) {
        // 设置字段的值
        env->SetStaticIntField(clazz, totalFrameField, gifBean->total_frame);
        env->SetStaticIntField(clazz, currentFrameFiled, gifBean->current_frame + 1); // 从第0帧开始的 , 便于Java层打log
    }

    // 当前帧解析完毕，指向下一帧
    gifBean->current_frame++;
    // 当前帧不能超过总帧数
    if (gifBean->current_frame > gifBean->total_frame - 1) {
        gifBean->current_frame = 0;
    }
    return 1;
}
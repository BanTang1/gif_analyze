package com.hx.ndkoneday;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class GifView extends View {

    private Movie mMovie;
    private long mStartTime;
    private Handler mHandler;

    public GifView(Context context) {
        super(context);
        init(context);
    }

    public GifView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public GifView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 从资源文件中获取GIF的输入流
        File file = new File(context.getExternalFilesDir(null),"demo.gif");
        if (file.exists()) {
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file.getAbsolutePath());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            mMovie = Movie.decodeStream(inputStream);
        }
        // 初始化Handler
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                // 重新绘制视图, 触发onDraw方法
                invalidate();
            }
        };
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now = android.os.SystemClock.uptimeMillis();

        if (mStartTime == 0) {
            mStartTime = now;
        }

        if (mMovie != null) {
            int duration = mMovie.duration();
            int relTime = (int) ((now - mStartTime) % duration);
            mMovie.setTime(relTime);

            // 将GIF渲染到Canvas上  ;一帧
            mMovie.draw(canvas, getWidth() - mMovie.width(), getHeight() - mMovie.height());
        }
        // 下一帧
        mHandler.sendEmptyMessageDelayed(0, GifHandler.FRAME_DURATION);
    }
}


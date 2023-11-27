package com.hx.ndkoneday;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hx.ndkoneday.databinding.ActivityGifBinding;
import com.hx.ndkoneday.databinding.ActivityMainBinding;

import java.io.File;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ActivityGifBinding binding;
    private final String TAG = "GIF_GIF";
    private final int REQUEST_CODE = 1;
    private Bitmap mBitmap;

    // Android 13中，废弃原有的读写权限，引入了新的权限， 此处获取权限请求结果。
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    if (isGranted) {
                        // User allowed the permission.
                    } else {
                        // User denied the permission.
                    }
                }
            });

    // 渲染GIF的后续帧
    private final Handler mHandler = new Handler(Objects.requireNonNull(Looper.myLooper())) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == 1) {
                GifHandler.updateFrame(mBitmap);
                binding.image.setImageBitmap(mBitmap);
                sendEmptyMessageDelayed(1,GifHandler.FRAME_DURATION);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermissions();

        // 实际上不需要任何权限
        String destinationPath = FileUtil.getAppPrivateDir(this);
        FileUtil.copyRawResourceToFile(this, R.raw.demo, destinationPath);

        binding = ActivityGifBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    /**
     * 请求权限， 为了验证高版本中不同权限的区别，
     * 实际上将res/raw/ 目录下的文件拷贝到应用程序的私有目录不需要任何权限，
     * 可直接在onCreate() 中拷贝
     */
    private void checkPermissions(){

        if (Build.VERSION.SDK_INT >= 33) {
            // Android 13 中，也废弃了写入权限， 取而代之的是 MediaStore API 以及  SAF（Storage Access Framework）
            // 使用这个方式请求权限比之前要简洁，不错
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            // 经典请求权限方式
            String[] PERMISSIONS_STORAGE ={
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,PERMISSIONS_STORAGE,REQUEST_CODE);
            } else {
                String destinationPath = FileUtil.getAppPrivateDir(this);
                FileUtil.copyRawResourceToFile(this, R.raw.demo, destinationPath);
            }
        }
    }


    /**
     *
     * 经典权限请求结果接收方式
     * @param requestCode The request code passed in {@link #checkPermissions()}
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            // 检查每个权限的授予情况
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        // 权限被授予
                        // 在这里执行相关操作
                        String destinationPath = FileUtil.getAppPrivateDir(this);
                        FileUtil.copyRawResourceToFile(this, R.raw.demo, destinationPath);
                    } else {
                        finish();
                    }
                }
                // 处理其他权限（如果有的话）
            }
        }
    }


    public void ndkLoadGif(View view){
        // 加载资源
        File file = new File(getExternalFilesDir(null),"demo.gif");
        Log.i(TAG, "ndkLoadGif: file = " + file.getAbsolutePath());
        if (!file.exists()) return;
        GifHandler.load(file.getAbsolutePath());

        // 获取宽高
        int width = GifHandler.getWidth();
        int height = GifHandler.getHeight();
        Log.i(TAG, "ndkLoadGif: width = " + width + ", height = " + height);

        // 开始渲染第一帧
        mBitmap = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        GifHandler.updateFrame(mBitmap);
        binding.image.setImageBitmap(mBitmap);

        // 通过Handle不断渲染后续帧
        mHandler.sendEmptyMessageDelayed(1,GifHandler.FRAME_DURATION);
    }

    public void javaLoadGif(View view) {
        if (binding.activityMain.getChildAt(3) == null) {
            GifView gifView = new GifView(this);
            // 设置布局参数
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            gifView.setLayoutParams(layoutParams);

            // 将 GifView 添加到父布局
            binding.activityMain.addView(gifView);
        } else {
            binding.activityMain.removeViewAt(3);
        }
    }

}
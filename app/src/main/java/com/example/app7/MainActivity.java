package com.example.app7;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Used to load the 'native-lib' and opencv library on application startup.
    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    private static final String TAG = "ETLD";//Eye Tracking Liveness Detection
    
    //UI变量
    private TextView textView;
    private JavaCameraView cameraView;
    private Button start_button;

    //相机用变量
    private Mat rgba;//相机获取的图像
    private int camera_index = 1;//用于切换前后摄像头，0为后置，1为前置
    private Handler handler;//传递信息

    //视线追踪用变量
    private Tracking eye_track;//视线追踪
    private boolean detect = false;//是否在检测
    private double mid_h_pos, mid_v_pos;//直视视线的垂直和水平位置


    //检测流程用变量
    private int step = 0;//检测流程1-6
    private int act;//检测动作1-5
    private long start_time, now_time;//记录时间
    private final boolean[] pre_action = new boolean[5];//之前做过的动作

    private int getCameraCount() {
        return Camera.getNumberOfCameras();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.CAMERA};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                }
            }
        }

        //设置UI
        setContentView(R.layout.activity_main);
        cameraView = findViewById(R.id.camera);
        textView = findViewById(R.id.textView);
        start_button = findViewById(R.id.button);
        ImageButton change_camera = findViewById(R.id.imageButton);


        //设置相机
        cameraView.setMaxFrameSize(640, 480);
        cameraView.disableFpsMeter();
        cameraView.setCvCameraViewListener(this);
        cameraView.setCameraIndex(1);//打开前置相机

        //初始化特征点识别
        faceinit(getAssets());
        
        //切换前后置相机按钮
        change_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.disableView();
                camera_index = (camera_index + 1) % getCameraCount();
                cameraView.setCameraIndex(camera_index);
                cameraView.enableView();
            }
        });

        //开始检测按钮
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start_button.setEnabled(false);//禁用按钮
                eye_track = new Tracking();//初始化视线追踪
                liveness_detection();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            cameraView.enableView();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraView);
    }

    public void onCameraViewStarted(int width, int height) {
        rgba = new Mat(width, height, CvType.CV_8UC3);//初始化rgba
    }

    public void onCameraViewStopped() {
        rgba.release();//释放rgba
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //获取相机的图像
        rgba = inputFrame.rgba();

        //前置相机需要镜像
        if (camera_index == 1) {
            Core.flip(rgba, rgba, 0);
        }

        if (detect) {
            //调用dlib进行人脸特征点识别
            long addr = rgba.getNativeObjAddr();
            List<Point> landmarks = findface(addr);//返回特征点列表

            TrackingResult res;//视线追踪结果

            if (landmarks.size() > 0) {
                eye_track.refresh(rgba, landmarks);//刷新视线追踪
                res = new TrackingResult(true, eye_track.horizontal_ratio(),
                        eye_track.vertical_ratio(), eye_track.is_blinking());

                //画左右巩膜
                if (eye_track.pupil_left_coords() != null)
                    Imgproc.circle(rgba, eye_track.pupil_left_coords(), 3, new Scalar(255, 0, 0), -1);
                if (eye_track.pupil_right_coords() != null)
                    Imgproc.circle(rgba, eye_track.pupil_right_coords(), 3, new Scalar(255, 0, 0), -1);
            } else {
                res = new TrackingResult(false);//没有识别到人脸
            }

            //用handler发送消息给主进程
            if (handler != null) {
                Message msg = Message.obtain();
                msg.obj = res;//视线追踪的结果
                handler.sendMessage(msg);
            }
        }

        return rgba;
    }

    @SuppressLint("HandlerLeak")
    public void liveness_detection() {
        //开始检测
        detect = true;
        Arrays.fill(pre_action, Boolean.FALSE);//所有动作都初始化为false

        //step1：直视屏幕2s，获取中间视线位置
        step = 1;
        textView.setTextColor(Color.parseColor("#3F51B5"));
        textView.setText("请直视屏幕");

        //将视线位置加入列表，计算均值
        List<Double> mid_h_list = new ArrayList<>();
        List<Double> mid_v_list = new ArrayList<>();

        start_time = System.currentTimeMillis();//开始时间

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                TrackingResult res = (TrackingResult) msg.obj;

                //没有检测到人脸，直接失败
                if (!res.face) {
                    Fail("没有检测到人脸");
                    return;
                }

                switch (step) {
                    case 1: {
                        now_time = System.currentTimeMillis();

                        if (now_time - start_time <= 2000) {//在2s时间内
                            //添加到列表中
                            if (res.horizontal_ratio > 0 && res.vertical_ratio > 0) {
                                mid_h_list.add(res.horizontal_ratio);
                                mid_v_list.add(res.vertical_ratio);
                            }
                        } else {//2s时间到
                            //计算平均值
                            if (mid_h_list.size() > 0 && mid_v_list.size() > 0) {
                                mid_h_pos = Util.avg(mid_h_list);
                                mid_v_pos = Util.avg(mid_v_list);
                                //Log.d(TAG, "handleMessage: "+mid_v_pos);
                            } else {
                                Fail("视线获取失败");
                                return;
                            }

                            //进入step2
                            step = 2;
                            next_action(); //随机第一次检测动作
                            start_time = System.currentTimeMillis(); //更新开始时间
                        }
                        break;
                    }
                    case 2: {
                        now_time = System.currentTimeMillis();
                        if (now_time - start_time > 5000) {
                            Fail("超时！检测失败！");
                            return;
                        }

                        if (judge_action(res)) {//检测成功，进入step3
                            action_success();
                            step = 3;
                            start_time = System.currentTimeMillis();
                        }
                        break;
                    }
                    case 3: {
                        now_time = System.currentTimeMillis();
                        if (now_time - start_time > 1000) {//等待1s
                            step = 4;
                            next_action(); //随机第二次检测动作
                            start_time = System.currentTimeMillis();
                        }
                        break;
                    }
                    case 4: {
                        now_time = System.currentTimeMillis();
                        if (now_time - start_time > 5000) {
                            Fail("超时！检测失败！");
                            return;
                        }

                        if (judge_action(res)) {//检测成功，进入step5
                            action_success();
                            step = 5;
                            start_time = System.currentTimeMillis();
                        }
                        break;
                    }
                    case 5: {
                        now_time = System.currentTimeMillis();
                        if (now_time - start_time > 1000) {//等待1s
                            step = 6;
                            next_action(); //随机第三次检测动作
                            start_time = System.currentTimeMillis();
                        }
                        break;
                    }
                    case 6: {
                        now_time = System.currentTimeMillis();
                        if (now_time - start_time > 5000) {
                            Fail("超时！检测失败！");
                            return;
                        }

                        if (judge_action(res)) {
                            Success();
                        }
                    }
                }
            }
        };
    }

    public void Fail(String text) {
        textView.setTextColor(Color.parseColor("#E91E63"));
        textView.setText(text);
        detect = false;
        start_button.setEnabled(true);//启用按钮
    }

    public void Success() {
        textView.setTextColor(Color.GREEN);
        textView.setText("成功!!!");
        detect = false;
        start_button.setEnabled(true);//启用按钮
    }

    public void action_success() {
        textView.setTextColor(Color.GREEN);
        textView.setText("动作正确");
    }

    public void next_action() {
        //随机下一个动作，跟已经做过的不重复
        double d;
        do {
            d = Math.random();
            act = (int) (d * 5);
        } while (pre_action[act]);
        pre_action[act] = true;
        //Log.d(TAG, "next_action: " + act);
        act=3;

        textView.setTextColor(Color.parseColor("#3F51B5"));
        switch (act) {
            case 0:
                textView.setText("请向左看");
                break;
            case 1:
                textView.setText("请向右看");
                break;
            case 2:
                textView.setText("请向上看");
                break;
            case 3:
                textView.setText("请向下看");
                break;
            case 4:
                textView.setText("请眨眼");
                break;
        }
    }

    public boolean judge_action(TrackingResult res) {
        switch (act) {
            case 0:
                return (res.horizontal_ratio > 0 && (mid_h_pos - res.horizontal_ratio > 0.2));
            case 1:
                return (res.horizontal_ratio > 0 && (res.horizontal_ratio - mid_h_pos > 0.2));
            case 2:
                //Log.d(TAG, "judge_action: "+(mid_v_pos - res.vertical_ratio));
                return (res.vertical_ratio > 0 && (mid_v_pos - res.vertical_ratio > 0.10));
            case 3:
                //Log.d(TAG, "judge_action: "+(res.vertical_ratio - mid_v_pos));
                return (res.vertical_ratio > 0 && (res.vertical_ratio - mid_v_pos > 0.12));
            case 4:
                return res.blinking;
        }
        return false;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public static native List<Point> findface(long jrgba);

    public static native void faceinit(AssetManager assetManager);
}
package com.pd.trackeye;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    VideoView videoView;
    TextView textView;
    Context context;

    long startTime;


    //For looking logs
    ArrayAdapter adapter;
    ArrayList<String> list = new ArrayList<>();

    // blink detection
    boolean isStaring = false; // 是否在注视
    long lastClose = 0;
    long lastOpen = 0;
    long lastBlink = 0;


    CameraSource cameraSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = MainActivity.this;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            //Toast.makeText(this, "Grant Permission and restart app", Toast.LENGTH_SHORT).show();
        }
        else {
            videoView = findViewById(R.id.videoView);
            textView = findViewById(R.id.textView);
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
            videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.videoplayback));
            videoView.start();
            createCameraSource();
            MyThread thread = new MyThread();
            thread.start();
            startTime = System.currentTimeMillis();


        }
    }


    class MyThread extends Thread {
        @Override
        public void run() {
            super.run();
            while(true){
                MediaMetadataRetriever rev = new MediaMetadataRetriever();

                rev.setDataSource(MainActivity.this, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.videoplayback)); //这里第一个参数需要Context，传this指针
                long currentTime = System.currentTimeMillis() - startTime;

                if (currentTime >= 20000 && currentTime <= 20015 || currentTime >= 40000 && currentTime <= 40015 || currentTime >= 60000 && currentTime <= 60015){
                    Time time = new Time("GMT+8");     //这里求出了手机系统当前的时间，用来给截出的图片作为名字。否则名字相同，就只会产生一个图片，要想产生多个图片，便需要每个                                                 图片的名字不同，我就用最水的办法，用系统时间来命名了
                    time.setToNow();
                    int hour = time.hour + 8;
                    int second = time.second;
                    int minute = time.minute;
                    Log.i("INFOOOOOOOOOOOOOOO", Integer.toString(hour) +"."+  Integer.toString(minute) +"."+  Integer.toString(second));
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Please take a screenshot!!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }
    }

    public void after_blink() {
        Log.i("INFOOOOOOOOOOOOOOO","a blink !!!");
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "Screenshot!", Toast.LENGTH_SHORT).show();
            }
        });


        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(context, notification);
        r.play();

        GetandSaveCurrentImage();
    }


    //This class will use google vision api to detect eyes
    private class EyesTracker extends Tracker<Face> {

        private final float THRESHOLD = 0.01f;
        private final float THRESHOLD_STARE = 0.8f;


        public EyesTracker() {
            //Toast.makeText(context, "tracker", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onUpdate(Detector.Detections<Face> detections, Face face) {
            //Log.i("INFOOOOOOOOOOOOOOO","left : " + face.getIsLeftEyeOpenProbability() + " right: " + face.getIsRightEyeOpenProbability());
            if (face.getIsLeftEyeOpenProbability() > THRESHOLD && face.getIsRightEyeOpenProbability() > THRESHOLD) { // open
                //Log.i(TAG, "onUpdate: Eyes Detected");
                showStatus("Eyes Detected and open");
                //if (!videoView.isPlaying()) videoView.start();
                lastOpen = System.currentTimeMillis();
                if (System.currentTimeMillis() - lastClose < 100) {
                    after_blink();
//                    if (System.currentTimeMillis() - lastBlink < 500) {
//                        after_blink();
//                        lastBlink = 0;
//                    }
//                    else {
//                        lastBlink = System.currentTimeMillis();
//                    }
                }
            } else {
                //if (videoView.isPlaying()) videoView.pause();
                if (isStaring) {
                    if ((face.getIsLeftEyeOpenProbability() < THRESHOLD && face.getIsRightEyeOpenProbability() > THRESHOLD_STARE) ||  (face.getIsLeftEyeOpenProbability() > THRESHOLD_STARE && face.getIsRightEyeOpenProbability() < THRESHOLD)) {
                        lastClose = System.currentTimeMillis();
                    }
                }
                showStatus("Eyes Detected and closed");
            }
            if (face.getIsLeftEyeOpenProbability() > THRESHOLD_STARE && face.getIsRightEyeOpenProbability() > THRESHOLD_STARE) {
                isStaring = true;
            }
        }

        @Override
        public void onMissing(Detector.Detections<Face> detections) {
            super.onMissing(detections);
            showStatus("Face Not Detected yet!");
        }

        @Override
        public void onDone() {
            super.onDone();
        }
    }











    private class FaceTrackerFactory implements MultiProcessor.Factory<Face> {

        private FaceTrackerFactory() {

        }

        @Override
        public Tracker<Face> create(Face face) {
            return new EyesTracker();
        }
    }

    public void createCameraSource() {
        FaceDetector detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(true)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.FAST_MODE)
                .build();
        detector.setProcessor(new MultiProcessor.Builder(new FaceTrackerFactory()).build());

        cameraSource = new CameraSource.Builder(this, detector)
                .setRequestedPreviewSize(1024, 1500)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cameraSource.start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraSource != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                cameraSource.start();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraSource!=null) {
            cameraSource.stop();
        }
        //if (videoView.isPlaying()) {
            //videoView.pause();
        //}
    }

    public void showStatus(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(message);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSource!=null) {
            cameraSource.release();
        }
    }


    /**
     * 获取和保存当前屏幕的截图
     */
    public void GetandSaveCurrentImage()
    {
        MediaMetadataRetriever rev = new MediaMetadataRetriever();

        rev.setDataSource(this, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.videoplayback)); //这里第一个参数需要Context，传this指针

        Bitmap bitmap = rev.getFrameAtTime(videoView.getCurrentPosition() * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);


        //构建Bitmap
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        int w = display.getWidth();
        int h = display.getHeight();
        Bitmap Bmp = Bitmap.createBitmap( w, h, Bitmap.Config.ARGB_8888 );
        //获取屏幕
        View decorview = this.getWindow().getDecorView();
        decorview.setDrawingCacheEnabled(true);
        Bmp = decorview.getDrawingCache();

        Bmp = bitmap;
        //图片存储路径
        String SavePath = getSDCardPath()+"/Pictures/Screenshots";  //这里是截图保存的路径
        //保存Bitmap
        try {
            PermisionUtils.verifyStoragePermissions(this);
            File path = new File(SavePath);
            Time time = new Time("GMT+8");     //这里求出了手机系统当前的时间，用来给截出的图片作为名字。否则名字相同，就只会产生一个图片，要想产生多个图片，便需要每个                                                 图片的名字不同，我就用最水的办法，用系统时间来命名了
            time.setToNow();
            int year = time.year;
            int month = time.month;
            int day = time.monthDay;
            int minute = time.minute;
            int hour = time.hour;
            int sec = time.second;
            //文件
            String filepath = SavePath+"/" + year+month+day+minute+sec+ "::" + Long.toString(System.currentTimeMillis() - startTime) + ".png";   //这里给图片命名
            File file = new File(filepath);
            if(!path.exists()){   //判断路径是否存在
                path.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = null;
            fos = new FileOutputStream(file);
            if (null != fos) {
                Bmp.compress(Bitmap.CompressFormat.PNG, 90, fos);
                fos.flush();
                fos.close();
                //Toast.makeText(getApplicationContext(), "截屏文件已保存至SDCard/qxbf/ScreenImages/目录下",Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 获取SDCard的目录路径功能
     * @return
     */
    public String getSDCardPath() {
        File sdcardDir = null;
        //判断SDCard是否存在
        boolean sdcardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        if (sdcardExist) {
            sdcardDir = Environment.getExternalStorageDirectory();
        }
        return sdcardDir.toString();
    }

}


class PermisionUtils {

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    /**
     * Checks if the app has permission to write to device storage
     * If the app does not has permission then the user will be prompted to
     * grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }
}
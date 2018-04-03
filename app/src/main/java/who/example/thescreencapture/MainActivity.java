package who.example.thescreencapture;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "bfl";
    private static final int PERMISSION_CODE = 1;
    private int mScreenDensity;
    private static final String BASENAME="capture.mp4";

    private MediaRecorder recorder;
    private MediaProjectionManager projectionManager;
    private MediaProjectionCallback projectionCallback;
    private VirtualDisplay virtualDisplay;

    private static final int DISPLAY_WIDTH = 480;
    private static final int DISPLAY_HEIGHT = 640;

    private TextView statusTv;
    private Button playBtn,startBtn, stopBtn;
    private MediaProjection projection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTv = (TextView)findViewById(R.id.statusTxt);
        startBtn = (Button)findViewById(R.id.startBtn);
        stopBtn = (Button)findViewById(R.id.stopBtn);
        playBtn = (Button)findViewById(R.id.playBtn);


        startBtn.setEnabled(false);

        if (isRecordableScreen()) {
            startBtn.setEnabled(true);
        } else {
            Log.i(TAG, "tableta nu e lollipop");
        }
    }

    private boolean isRecordableScreen(){
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
    }


    private void initAndPrepareRecorder() {
        initRecorder();
        prepareRecorder();
        Log.i(TAG, "sunt pe tableta lollipop");
    }

    private void initRecorder() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setVideoEncodingBitRate(512 * 1000);
        recorder.setVideoFrameRate(30);
        recorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        final File fileName = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                BASENAME);
        recorder.setOutputFile(fileName.getAbsolutePath());
    }

    private void prepareRecorder() {
        try {
            recorder.prepare();
        } catch (IllegalStateException e) {
            Log.e(TAG, "preparing recorder IllegalStateException", e);
        } catch (IOException e) {
            Log.e(TAG, "preparing recorder IOException", e);
        }

        projectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        projectionCallback = new MediaProjectionCallback();
    }

    public void startPressed(View view) {
        initAndPrepareRecorder();
        recordScreen();
    }

    private void recordScreen() {
        if(projection == null){
            startActivityForResult(projectionManager.createScreenCaptureIntent(),
                    PERMISSION_CODE);
            return;
        }
        virtualDisplay = createVirtualDisplay();
        recorder.start();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        projection = projectionManager.getMediaProjection(resultCode, data);
        projection.registerCallback(projectionCallback, null);
        virtualDisplay = createVirtualDisplay();
        recorder.start();
        setStatusMessage("start pressed");
    }

    private VirtualDisplay createVirtualDisplay() {
        getScreenDensity();

        return projection.createVirtualDisplay("MainActivity", DISPLAY_WIDTH, DISPLAY_HEIGHT,
                mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, recorder.getSurface(),
                null, null);
    }

    private void getScreenDensity() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
    }


    public void stopPressed(View view) {
        setStatusMessage("stop pressed");
        stopScreenRecording();
    }


    private void stopScreenRecording() {
        recorder.stop();
        recorder.reset();
        if(virtualDisplay == null){
            return;
        }
        virtualDisplay.release();
    }


    public void playPressed(View view) {
        setStatusMessage("play pressed");
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            //stop recording
            recorder.stop();
            recorder.reset();
            //stop projection
            projection = null;
            stopScreenRecording();
        }
    }


    private void setStatusMessage(String message) {
        statusTv.setText(message);
        Log.i(TAG, "info: " + message);
    }
}

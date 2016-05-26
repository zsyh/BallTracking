package com.zaizai1.balltracking;


        import org.opencv.android.BaseLoaderCallback;
        import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
        import org.opencv.android.LoaderCallbackInterface;
        import org.opencv.android.OpenCVLoader;
        import org.opencv.core.Mat;
        import org.opencv.android.CameraBridgeViewBase;
        import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

        import android.app.Activity;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.MenuItem;
        import android.view.SurfaceView;
        import android.view.WindowManager;
        import android.widget.RadioButton;
        import android.widget.RadioGroup;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;


    private RadioGroup radioGroup;
    private RadioButton radioButtonDoNothing,radioButtonLeft,radioButtonRight,radioButtonBall;

    private boolean isSelected = false;

    private static final int TOUCH_DONOTHING=0;
    private static final int TOUCH_LEFT=1;
    private static final int TOUCH_RIGHT=2;
    private static final int TOUCH_BALL=3;
    private int touchMode=TOUCH_DONOTHING;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.zaizai1_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.zaizai1_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);


        radioGroup=(RadioGroup)findViewById(R.id.radioGroup);
        radioButtonDoNothing=(RadioButton)findViewById(R.id.radioButtonDoNothing);
        radioButtonLeft=(RadioButton)findViewById(R.id.radioButtonLeft);
        radioButtonRight=(RadioButton)findViewById(R.id.radioButtonRight);
        radioButtonBall=(RadioButton)findViewById(R.id.radioButtonBall);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId==radioButtonDoNothing.getId()) {
                    touchMode=TOUCH_DONOTHING;
                }

                else if(checkedId==radioButtonLeft.getId()){
                    touchMode=TOUCH_LEFT;
                }
                else if(checkedId==radioButtonRight.getId()){
                    touchMode=TOUCH_RIGHT;
                }
                else if(checkedId==radioButtonBall.getId()){
                    touchMode=TOUCH_BALL;
                }
                else
                {
                    Log.e("BallTracking","不存在该checkedId,在radioGroup.setOnCheckedChangeListener()");
                }
            }
        });




    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {

    }

    public void onCameraViewStopped() {

    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        return inputFrame.rgba();
    }
}

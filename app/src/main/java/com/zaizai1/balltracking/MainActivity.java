package com.zaizai1.balltracking;


        import org.opencv.android.BaseLoaderCallback;
        import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
        import org.opencv.android.LoaderCallbackInterface;
        import org.opencv.android.OpenCVLoader;
        import org.opencv.core.Core;
        import org.opencv.core.Mat;
        import org.opencv.android.CameraBridgeViewBase;
        import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
        import org.opencv.core.Point;
        import org.opencv.core.Rect;
        import org.opencv.core.Scalar;
        import org.opencv.imgproc.Imgproc;
        import org.opencv.imgproc.Moments;

        import android.app.Activity;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.MenuItem;
        import android.view.MotionEvent;
        import android.view.SurfaceView;
        import android.view.View;
        import android.view.WindowManager;
        import android.widget.RadioButton;
        import android.widget.RadioGroup;

public class MainActivity extends Activity implements CvCameraViewListener2, View.OnTouchListener {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;

    private Mat mRgba;
    private Mat mSelected;
    private Mat mHSVMatLeft;
    private Mat mHSVMatRight;
    private Mat mHSVMatBall;

    private Mat mBinaryLeft;
    private Mat mBinary2Left;
    private Mat mBinaryRight;
    private Mat mBinary2Right;
    private Mat mBinaryBall;
    private Mat mBinary2Ball;

    private Scalar mBlobColorHsvLeft ;
    private Scalar mBlobColorHsvRight ;
    private Scalar mBlobColorHsvBall ;

    private Point leftLeadRail=new Point();
    private Point rightLeadRail=new Point();
    private Point ball = new Point();

    private Rect leftTrackZone = new Rect();
    private Rect rightTrackZone = new Rect();
    private Rect ballTrackZone = new Rect();

    private RadioGroup radioGroup;
    private RadioButton radioButtonDoNothing,radioButtonLeft,radioButtonRight,radioButtonBall;


    private boolean isSelected = false;
    private boolean isLeftSelected =false;
    private boolean isRightSelected =false;
    private boolean isBallSelected =false;

    private static final int TOUCH_DONOTHING=0;
    private static final int TOUCH_LEFT=1;
    private static final int TOUCH_RIGHT=2;
    private static final int TOUCH_BALL=3;
    private int touchMode=TOUCH_DONOTHING;

    private static final int RECTHALFLENGTH=150;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
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


    @Override
    public boolean onTouch(View v, MotionEvent event) {

        if(touchMode==TOUCH_DONOTHING) return false;

        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.e(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);



        if(touchMode==TOUCH_LEFT){

            // Calculate average color of touched region
            mBlobColorHsvLeft = Core.sumElems(touchedRegionHsv);
            int pointCount = touchedRect.width*touchedRect.height;
            for (int i = 0; i < mBlobColorHsvLeft.val.length; i++)
                mBlobColorHsvLeft.val[i] /= pointCount;

            Log.e(TAG, "left Touched HSV color: (" + mBlobColorHsvLeft.val[0] + ", " + mBlobColorHsvLeft.val[1] +
                    ", " + mBlobColorHsvLeft.val[2] + ", " + mBlobColorHsvLeft.val[3] + ")");

            leftLeadRail.x=x;
            leftLeadRail.y=y;
            isLeftSelected=true;

        }
        else if (touchMode==TOUCH_RIGHT){
            // Calculate average color of touched region
            mBlobColorHsvRight = Core.sumElems(touchedRegionHsv);
            int pointCount = touchedRect.width*touchedRect.height;
            for (int i = 0; i < mBlobColorHsvRight.val.length; i++)
                mBlobColorHsvRight.val[i] /= pointCount;

            Log.e(TAG, "right Touched HSV color: (" + mBlobColorHsvRight.val[0] + ", " + mBlobColorHsvRight.val[1] +
                    ", " + mBlobColorHsvRight.val[2] + ", " + mBlobColorHsvRight.val[3] + ")");
            rightLeadRail.x=x;
            rightLeadRail.y=y;
            isRightSelected=true;
        }
        else if (touchMode==TOUCH_BALL) {
            // Calculate average color of touched region
            mBlobColorHsvBall = Core.sumElems(touchedRegionHsv);
            int pointCount = touchedRect.width*touchedRect.height;
            for (int i = 0; i < mBlobColorHsvBall.val.length; i++)
                mBlobColorHsvBall.val[i] /= pointCount;

            Log.e(TAG, "ball Touched HSV color: (" + mBlobColorHsvBall.val[0] + ", " + mBlobColorHsvBall.val[1] +
                    ", " + mBlobColorHsvBall.val[2] + ", " + mBlobColorHsvBall.val[3] + ")");
            ball.x=x;
            ball.y=y;
            isBallSelected=true;
        }



        isSelected=true;

        return false;

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
        mBinaryLeft=new Mat();
        mBinary2Left=new Mat();
        mBinaryRight=new Mat();
        mBinary2Right=new Mat();
        mBinaryBall=new Mat();
        mBinary2Ball=new Mat();
        mHSVMatLeft=new Mat();
        mHSVMatRight=new Mat();
        mHSVMatBall=new Mat();

    }

    public void onCameraViewStopped() {
        mBinaryLeft.release();
        mBinary2Left.release();
        mBinaryRight.release();
        mBinary2Right.release();
        mBinaryBall.release();
        mBinary2Ball.release();
        mHSVMatLeft.release();
        mHSVMatRight.release();
        mHSVMatBall.release();


    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba=inputFrame.rgba();//mRgba是给回调函数onTouch传递数据的全局变量
        Mat mRgbaTemp = inputFrame.rgba();
        if(isSelected){

            if(leftTrackZone.width != 0 && leftTrackZone.height!= 0) {
                mSelected = mRgbaTemp.submat(leftTrackZone);
                Imgproc.cvtColor(mSelected, mHSVMatLeft, Imgproc.COLOR_RGB2HSV_FULL);


            }
            if(rightTrackZone.width != 0 && rightTrackZone.height!= 0) {
                mSelected = mRgbaTemp.submat(leftTrackZone);
                Imgproc.cvtColor(mSelected, mHSVMatRight, Imgproc.COLOR_RGB2HSV_FULL);
            }
            if(ballTrackZone.width != 0 && ballTrackZone.height!= 0) {
                mSelected = mRgbaTemp.submat(leftTrackZone);
                Imgproc.cvtColor(mSelected, mHSVMatBall, Imgproc.COLOR_RGB2HSV_FULL);
            }


            if (isLeftSelected){
                if(leftTrackZone.width != 0 && leftTrackZone.height!= 0) {
                    myInRangeByBolbColorAndHSV(mBlobColorHsvLeft, TOUCH_LEFT);
                    setLeadRailByThre(leftTrackZone, leftLeadRail, TOUCH_LEFT);
                }
                setTrackZoneByLeadRail(mRgbaTemp,leftTrackZone,leftLeadRail,TOUCH_LEFT);
            }
            if (isRightSelected){
                if(rightTrackZone.width != 0 && rightTrackZone.height!= 0) {
                    myInRangeByBolbColorAndHSV(mBlobColorHsvRight, TOUCH_RIGHT);
                    setLeadRailByThre(rightTrackZone, rightLeadRail, TOUCH_RIGHT);
                }
                setTrackZoneByLeadRail(mRgbaTemp,rightTrackZone,rightLeadRail,TOUCH_RIGHT);
            }
            if(isBallSelected){
                if(ballTrackZone.width != 0 && ballTrackZone.height!= 0) {
                    myInRangeByBolbColorAndHSV(mBlobColorHsvBall, TOUCH_BALL);
                    setLeadRailByThre(ballTrackZone, ball, TOUCH_BALL);
                }
                setTrackZoneByLeadRail(mRgbaTemp,ballTrackZone,ball,TOUCH_BALL);
            }

        }
        return mRgbaTemp;
    }


    private void myInRangeByBolbColorAndHSV(Scalar hsvColor,int mode) {

        Scalar mLowerBound = new Scalar(0);
        Scalar mUpperBound = new Scalar(0);
        Scalar mColorRadius= new Scalar(25,50,50,0);

        double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0]-mColorRadius.val[0] : 0;
        double maxH = (hsvColor.val[0]+mColorRadius.val[0] <= 255) ? hsvColor.val[0]+mColorRadius.val[0] : 255;

        mLowerBound.val[0] = minH;
        mUpperBound.val[0] = maxH;

        mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
        mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

        mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
        mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;

        if(mode==TOUCH_LEFT){
            Core.inRange(mHSVMatLeft, mLowerBound, mUpperBound, mBinaryLeft);
            Imgproc.dilate(mBinaryLeft, mBinary2Left, new Mat());
        }
        else if (mode==TOUCH_RIGHT){
            Core.inRange(mHSVMatRight, mLowerBound, mUpperBound, mBinaryRight);
            Imgproc.dilate(mBinaryRight, mBinary2Right, new Mat());
        }
        else  {//else if (touchMode==TOUCH_BALL)
            Core.inRange(mHSVMatBall, mLowerBound, mUpperBound, mBinaryBall);
            Imgproc.dilate(mBinaryBall, mBinary2Ball, new Mat());
        }




    }

    private void setLeadRailByThre(Rect trackZone,Point leadRail,int mode){



        Moments moments;
        if(mode==TOUCH_LEFT){

            moments=Imgproc.moments(mBinary2Left);
        }
        else if (mode==TOUCH_RIGHT){

            moments =Imgproc.moments(mBinary2Right);
        }
        else  {//else if (touchMode==TOUCH_BALL)

            moments =Imgproc.moments(mBinary2Ball);
        }




        double m00=moments.get_m00();
        double m10=moments.get_m10();
        double m01=moments.get_m01();



        if(m00!=0)
        {
            int xAvr=(int)(m10/m00);
            int yAvr=(int)(m01/m00);
            xAvr += trackZone.x;
            yAvr += trackZone.y;
            if(xAvr > mRgba.cols() || yAvr> mRgba.rows())
            {
                Log.e("HelloOpenCV","自动跟踪溢出，维持原来的点"+"   mode:" + mode);
            }
            else {
                leadRail.x = xAvr;
                leadRail.y = yAvr;
            }

            leadRail.x = xAvr;
            leadRail.y = yAvr;

            Log.e("HelloOpenCV","自动跟踪:" + leadRail.x + " " +leadRail.y+"   mode:" + mode);
        }
        else {
            Log.e("HelloOpenCV","自动跟踪错误,m00==0 , mode:" + mode);
        }


    }
    private void setTrackZoneByLeadRail(Mat mRgbaTemp,Rect trackZone,Point leadRail,int mode) {

        Scalar color;
        if(mode==TOUCH_LEFT){
            color = new Scalar(255,0,0);
        }
        else if (mode==TOUCH_RIGHT){
            color=new Scalar(0,255,0);
        }
        else  {//else if (touchMode==TOUCH_BALL)
            color=new Scalar(0,0,0);
        }

        double offsetedx =( mOpenCvCameraView.getWidth() + mRgba.cols())/2;
        double offsetedy = (mOpenCvCameraView.getHeight() + mRgba.rows())/2;

        Point leftTop = new Point((leadRail.x - RECTHALFLENGTH < 0) ? 0 : leadRail.x - RECTHALFLENGTH, (leadRail.y - RECTHALFLENGTH < 0) ? 0 : leadRail.y - RECTHALFLENGTH);
        Point rightBottom = new Point((leadRail.x + RECTHALFLENGTH > offsetedx) ? offsetedx : leadRail.x + RECTHALFLENGTH, (leadRail.y + RECTHALFLENGTH > offsetedy) ? offsetedy : leadRail.y + RECTHALFLENGTH);

        //Log.e("HelloOpenCV",leftTop.x+" "+leftTop.y+" "+rightBottom.x + " " + rightBottom.y);

        Imgproc.rectangle(mRgbaTemp, leftTop, rightBottom, color, 5);


        trackZone.x=(int)leftTop.x;
        trackZone.y=(int)leftTop.y;
        trackZone.width=(int)rightBottom.x - (int)leftTop.x;
        trackZone.height=(int)rightBottom.y - (int)leftTop.y;
    }


}

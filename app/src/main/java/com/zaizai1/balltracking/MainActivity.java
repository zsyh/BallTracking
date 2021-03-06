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
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothSocket;
        import android.content.SharedPreferences;
        import android.media.MediaPlayer;
        import android.os.Bundle;
        import android.os.Handler;
        import android.os.Looper;
        import android.os.Message;
        import android.util.Log;
        import android.view.KeyEvent;
        import android.view.LayoutInflater;
        import android.view.MenuItem;
        import android.view.MotionEvent;
        import android.view.SurfaceView;
        import android.view.View;
        import android.view.WindowManager;
        import android.widget.Button;
        import android.widget.CheckBox;
        import android.widget.CompoundButton;
        import android.widget.EditText;
        import android.widget.LinearLayout;
        import android.widget.MediaController;
        import android.widget.RadioButton;
        import android.widget.RadioGroup;
        import android.widget.TextView;
        import android.widget.Toast;

        import java.io.BufferedReader;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.InputStreamReader;
        import java.io.OutputStream;
        import java.text.SimpleDateFormat;
        import java.util.Queue;
        import java.util.UUID;
        import java.util.concurrent.LinkedBlockingQueue;
        import java.util.regex.Pattern;
        import java.util.zip.CheckedInputStream;

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

    //主界面
    private TextView textViewLeft,textViewRight,textViewBall,textViewPosition,textViewInformation;
    private Button buttonSetRange,buttonBlueToothConnect,buttonSetPID,buttonDataTransControl;
    private CheckBox checkBoxWideAngleErrorFix;
    //SetRange
    private RadioGroup radioGroup;
    private RadioButton radioButtonDoNothing,radioButtonLeft,radioButtonRight,radioButtonBall;
    private CheckBox checkBoxPickColor;
    private TextView textViewH,textViewS,textViewV;
    private Button buttonSetRangeReturn,buttonUseLastHSV;
    //setPID
    private EditText editTextP,editTextI,editTextD,editTextIThreshold;
    private Button buttonSendP,buttonSendI,buttonSendD,buttonSendIThreshold,buttonSetPIDReturn;
    //dataTransControl
    private EditText editTextTargetPosition,editTextStep,editTextInstruction,editTextData;
    private Button buttonStartSendPosition,buttonStopSendPosition,buttonSetTargetPosition,buttonForeward,buttonClear,buttonReverse,buttonEmergencySend,buttonDataTransControlReturn;

    private BluetoothAdapter defaultAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outPutStream;
    private InputStream inPutStream;
    private BufferedReader bufferedReader;
    private boolean isConnected=false;
    private boolean isPositionSending=false;
    private Handler sendHandler;
    private Handler recvHandler;

    private MediaPlayer mediaPlayer;
    
    private boolean isSelected = false;
    private boolean isLeftSelected =false;
    private boolean isRightSelected =false;
    private boolean isBallSelected =false;
    private boolean isPickColor=true;

    private boolean leftTouchLock=false;
    private boolean rightTouchLock=false;
    private boolean ballTouchLock=false;


    private static final int TOUCH_DONOTHING=0;
    private static final int TOUCH_LEFT=1;
    private static final int TOUCH_RIGHT=2;
    private static final int TOUCH_BALL=3;
    private int touchMode=TOUCH_DONOTHING;

    private static final int RECTHALFLENGTH=80;
    private int leftEdgePosition = 11,rightEdgePosition = 586;


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


    private long mExitTime;
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();

            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
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



        Rect  touchedRect = new Rect();
        Mat touchedRegionRgba;//这个不能在此直接初始化,大坑
        Mat touchedRegionHsv = new Mat();
        if(isPickColor) {

            touchedRect.x = (x > 4) ? x - 4 : 0;
            touchedRect.y = (y > 4) ? y - 4 : 0;

            touchedRect.width = (x + 4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
            touchedRect.height = (y + 4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

            touchedRegionRgba = mRgba.submat(touchedRect);


            Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);
        }


        if(touchMode==TOUCH_LEFT){

            if(isPickColor) {
                // Calculate average color of touched region
                mBlobColorHsvLeft = Core.sumElems(touchedRegionHsv);
                int pointCount = touchedRect.width * touchedRect.height;
                for (int i = 0; i < mBlobColorHsvLeft.val.length; i++)
                    mBlobColorHsvLeft.val[i] /= pointCount;

                Log.e(TAG, "left Touched HSV color: (" + mBlobColorHsvLeft.val[0] + ", " + mBlobColorHsvLeft.val[1] +
                        ", " + mBlobColorHsvLeft.val[2] + ", " + mBlobColorHsvLeft.val[3] + ")");

                //把成功选定的颜色写入文件,并显示在相应文本框
                SharedPreferences user = getSharedPreferences("HSV_info",0);
                SharedPreferences.Editor editor=user.edit();
                String Htemp= String.format(Double.toString(mBlobColorHsvLeft.val[0]), "%.2f");
                String Stemp=String.format(Double.toString(mBlobColorHsvLeft.val[1]), "%.2f");
                String Vtemp=String.format(Double.toString(mBlobColorHsvLeft.val[2]), "%.2f");

                editor.putString("leftH",Htemp );
                editor.putString("leftS",Stemp );
                editor.putString("leftV",Vtemp );
                editor.commit();

                textViewH.setText(Htemp);
                textViewS.setText(Stemp);
                textViewV.setText(Vtemp);

                buttonUseLastHSV.setVisibility(View.INVISIBLE);
            }

            leftLeadRail.x=x;
            leftLeadRail.y=y;
            isLeftSelected=true;
            leftTouchLock=true;

        }
        else if (touchMode==TOUCH_RIGHT){
            if(isPickColor) {
                // Calculate average color of touched region
                mBlobColorHsvRight = Core.sumElems(touchedRegionHsv);
                int pointCount = touchedRect.width * touchedRect.height;
                for (int i = 0; i < mBlobColorHsvRight.val.length; i++)
                    mBlobColorHsvRight.val[i] /= pointCount;

                Log.e(TAG, "right Touched HSV color: (" + mBlobColorHsvRight.val[0] + ", " + mBlobColorHsvRight.val[1] +
                        ", " + mBlobColorHsvRight.val[2] + ", " + mBlobColorHsvRight.val[3] + ")");

                //把成功选定的颜色写入文件,并显示在相应文本框
                SharedPreferences user = getSharedPreferences("HSV_info",0);
                SharedPreferences.Editor editor=user.edit();
                String Htemp= String.format(Double.toString(mBlobColorHsvRight.val[0]), "%.2f");
                String Stemp=String.format(Double.toString(mBlobColorHsvRight.val[1]), "%.2f");
                String Vtemp=String.format(Double.toString(mBlobColorHsvRight.val[2]), "%.2f");
                

                editor.putString("rightH",Htemp );
                editor.putString("rightS",Stemp );
                editor.putString("rightV",Vtemp );
                editor.commit();

                textViewH.setText(Htemp);
                textViewS.setText(Stemp);
                textViewV.setText(Vtemp);
                buttonUseLastHSV.setVisibility(View.INVISIBLE);

            }


            rightLeadRail.x=x;
            rightLeadRail.y=y;
            isRightSelected=true;
            rightTouchLock=true;
        }
        else if (touchMode==TOUCH_BALL) {
            if(isPickColor) {
                // Calculate average color of touched region
                mBlobColorHsvBall = Core.sumElems(touchedRegionHsv);
                int pointCount = touchedRect.width * touchedRect.height;
                for (int i = 0; i < mBlobColorHsvBall.val.length; i++)
                    mBlobColorHsvBall.val[i] /= pointCount;

                Log.e(TAG, "ball Touched HSV color: (" + mBlobColorHsvBall.val[0] + ", " + mBlobColorHsvBall.val[1] +
                        ", " + mBlobColorHsvBall.val[2] + ", " + mBlobColorHsvBall.val[3] + ")");

                //把成功选定的颜色写入文件,并显示在相应文本框
                SharedPreferences user = getSharedPreferences("HSV_info",0);
                SharedPreferences.Editor editor=user.edit();
                String Htemp= String.format(Double.toString(mBlobColorHsvBall.val[0]), "%.2f");
                String Stemp=String.format(Double.toString(mBlobColorHsvBall.val[1]), "%.2f");
                String Vtemp=String.format(Double.toString(mBlobColorHsvBall.val[2]), "%.2f");

                editor.putString("ballH",Htemp );
                editor.putString("ballS",Stemp );
                editor.putString("ballV",Vtemp );
                editor.commit();

                textViewH.setText(Htemp);
                textViewS.setText(Stemp);
                textViewV.setText(Vtemp);
                buttonUseLastHSV.setVisibility(View.INVISIBLE);

            }


            ball.x=x;
            ball.y=y;
            isBallSelected=true;
            ballTouchLock=true;
        }


        if( isBallSelected && isLeftSelected && isRightSelected){

            checkBoxPickColor.setEnabled(true);
        }

        isSelected=true;

        return false;

    }

    private LayoutInflater inflater;
    private LinearLayout linearLayout;
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


        defaultAdapter=BluetoothAdapter.getDefaultAdapter();

        if(defaultAdapter==null)
        {
            Log.e("BluetoothTest","未找到蓝牙设备！");
        }
        else {
            Log.e("BluetoothTest", "找到蓝牙设备！");
        }



        //动态加载
        inflater = LayoutInflater.from(this);
        linearLayout = (LinearLayout) findViewById(R.id.linearLayout);//主窗口上的


        final View viewSetRange = inflater.inflate(R.layout.setrange, null);
        final View viewSetPID = inflater.inflate(R.layout.setpid, null);
        final View viewDataTransControl = inflater.inflate(R.layout.datatranscontrol, null);

        final LinearLayout linearLayoutSetRange = (LinearLayout) viewSetRange.findViewById(R.id.linearLayoutSetRange);
        final LinearLayout linearLayoutSetPID = (LinearLayout) viewSetPID.findViewById(R.id.linearLayoutSetPID);
        final LinearLayout linearLayoutDataTransControl = (LinearLayout) viewDataTransControl.findViewById(R.id.linearLayoutDataTransControl);


        //主窗口
        buttonSetRange=(Button)findViewById(R.id.buttonSetRange);
        buttonBlueToothConnect=(Button)findViewById(R.id.buttonBlueToothConnect);
        buttonSetPID=(Button)findViewById(R.id.buttonSetPID);
        buttonDataTransControl=(Button)findViewById(R.id.buttonDataTransControl);
        textViewLeft=(TextView)findViewById(R.id.textViewLeft);
        textViewRight=(TextView)findViewById(R.id.textViewRight);
        textViewBall=(TextView)findViewById(R.id.textViewBall);
        textViewPosition=(TextView)findViewById(R.id.textViewPosition);
        textViewInformation=(TextView)findViewById(R.id.textViewInformation);
        checkBoxWideAngleErrorFix=(CheckBox)findViewById(R.id.checkBoxWideAngleErrorFix);

        buttonSetRange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                linearLayout.removeAllViews();
                linearLayout.addView(linearLayoutSetRange);
                buttonSetRange.setVisibility(View.INVISIBLE);
                buttonBlueToothConnect.setVisibility(View.INVISIBLE);
                buttonSetPID.setVisibility(View.INVISIBLE);
                buttonDataTransControl.setVisibility(View.INVISIBLE);
                checkBoxWideAngleErrorFix.setVisibility(View.INVISIBLE);

            }
        });

        buttonSetPID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                linearLayout.removeAllViews();
                linearLayout.addView(linearLayoutSetPID);
                buttonSetRange.setVisibility(View.INVISIBLE);
                buttonBlueToothConnect.setVisibility(View.INVISIBLE);
                buttonSetPID.setVisibility(View.INVISIBLE);
                buttonDataTransControl.setVisibility(View.INVISIBLE);
                checkBoxWideAngleErrorFix.setVisibility(View.INVISIBLE);

            }
        });

        buttonDataTransControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                linearLayout.removeAllViews();
                linearLayout.addView(linearLayoutDataTransControl);
                buttonSetRange.setVisibility(View.INVISIBLE);
                buttonBlueToothConnect.setVisibility(View.INVISIBLE);
                buttonSetPID.setVisibility(View.INVISIBLE);
                buttonDataTransControl.setVisibility(View.INVISIBLE);
                checkBoxWideAngleErrorFix.setVisibility(View.INVISIBLE);
            }
        });



        buttonBlueToothConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(defaultAdapter==null)
                {
                    return;
                }
                final BluetoothDevice bluetoothDevice=defaultAdapter.getRemoteDevice("98:D3:35:00:9B:F1");//MAC地址

                try {
                    bluetoothSocket=bluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                } catch (IOException e) {

                    Log.e("BluetoothTest","socket初始化时IOException:"+e);//一般不会有
                }

                Log.e("BluetoothTest","socket初始成功！");//一般都是成功
                if(bluetoothSocket==null)
                {
                    Log.e("BluetoothTest","socket未初始化！");
                    return;
                }


                Thread connectingThread = new ConnectingThread();
                connectingThread.start();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        connectStartUI();

                    }
                });


                //onClickEND
            }
            //setEND
        });



        ReturnOnClickListener returnOnClickListener = new ReturnOnClickListener();

        //SetRange
        radioGroup=(RadioGroup)viewSetRange.findViewById(R.id.radioGroup);
        radioButtonDoNothing=(RadioButton)viewSetRange.findViewById(R.id.radioButtonDoNothing);
        radioButtonLeft=(RadioButton)viewSetRange.findViewById(R.id.radioButtonLeft);
        radioButtonRight=(RadioButton)viewSetRange.findViewById(R.id.radioButtonRight);
        radioButtonBall=(RadioButton)viewSetRange.findViewById(R.id.radioButtonBall);
        checkBoxPickColor=(CheckBox)viewSetRange.findViewById(R.id.checkBoxPickColor);
        buttonUseLastHSV=(Button)viewSetRange.findViewById(R.id.buttonUseLastHSV);
        buttonSetRangeReturn=(Button) viewSetRange.findViewById(R.id.buttonSetRangeReturn);
        textViewH=(TextView) viewSetRange.findViewById(R.id.textViewH);
        textViewS=(TextView) viewSetRange.findViewById(R.id.textViewS);
        textViewV=(TextView) viewSetRange.findViewById(R.id.textViewV);


        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId==radioButtonDoNothing.getId()) {
                    touchMode=TOUCH_DONOTHING;
                    buttonUseLastHSV.setVisibility(View.INVISIBLE);
                }

                else if(checkedId==radioButtonLeft.getId()){
                    touchMode=TOUCH_LEFT;
                    //从文件中读取HSV
                    SharedPreferences sp = getSharedPreferences("HSV_info",0);
                    String Htemp = sp.getString("leftH","");
                    String Stemp = sp.getString("leftS","");
                    String Vtemp = sp.getString("leftV","");

                    textViewH.setText(Htemp);
                    textViewS.setText(Stemp);
                    textViewV.setText(Vtemp);

                    if(isLeftSelected) {
                        buttonUseLastHSV.setVisibility(View.INVISIBLE);
                    }
                    else
                    {
                        if(!(Htemp.equals("") && Stemp.equals("") && Htemp.equals("")))
                            buttonUseLastHSV.setVisibility(View.VISIBLE);
                    }


                }
                else if(checkedId==radioButtonRight.getId()){
                    touchMode=TOUCH_RIGHT;
                    //从文件中读取HSV
                    SharedPreferences sp = getSharedPreferences("HSV_info",0);
                    String Htemp = sp.getString("rightH","");
                    String Stemp = sp.getString("rightS","");
                    String Vtemp = sp.getString("rightV","");

                    textViewH.setText(Htemp);
                    textViewS.setText(Stemp);
                    textViewV.setText(Vtemp);

                    if(isRightSelected) {
                        buttonUseLastHSV.setVisibility(View.INVISIBLE);
                    }
                    else
                    {
                        if(!(Htemp.equals("") && Stemp.equals("") && Htemp.equals("")))
                        buttonUseLastHSV.setVisibility(View.VISIBLE);
                    }
                }
                else if(checkedId==radioButtonBall.getId()){
                    touchMode=TOUCH_BALL;
                    //从文件中读取HSV
                    SharedPreferences sp = getSharedPreferences("HSV_info",0);
                    String Htemp = sp.getString("ballH","");
                    String Stemp = sp.getString("ballS","");
                    String Vtemp = sp.getString("ballV","");

                    textViewH.setText(Htemp);
                    textViewS.setText(Stemp);
                    textViewV.setText(Vtemp);

                    if(isBallSelected) {
                        buttonUseLastHSV.setVisibility(View.INVISIBLE);
                    }
                    else
                    {
                        if(!(Htemp.equals("") && Stemp.equals("") && Htemp.equals("")))
                        buttonUseLastHSV.setVisibility(View.VISIBLE);
                    }

                }
                else
                {
                    Log.e("BallTracking","不存在该checkedId,在radioGroup.setOnCheckedChangeListener()");
                }
            }
        });


        checkBoxPickColor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                isPickColor=isChecked;

            }
        });

        buttonUseLastHSV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                if(touchMode==TOUCH_LEFT){
                    mBlobColorHsvLeft=new Scalar(
                            Double.parseDouble(textViewH.getText().toString()),
                            Double.parseDouble(textViewS.getText().toString()),
                            Double.parseDouble(textViewV.getText().toString()));
                    leftLeadRail.x=200;
                    leftLeadRail.y=200;
                    isLeftSelected=true;
                    isSelected=true;

                }
                else if(touchMode==TOUCH_RIGHT){
                    mBlobColorHsvRight=new Scalar(
                            Double.parseDouble(textViewH.getText().toString()),
                            Double.parseDouble(textViewS.getText().toString()),
                            Double.parseDouble(textViewV.getText().toString()));
                    rightLeadRail.x=400;
                    rightLeadRail.y=200;
                    isRightSelected=true;
                    isSelected=true;

                }
                else if(touchMode==TOUCH_BALL){
                    mBlobColorHsvBall=new Scalar(
                            Double.parseDouble(textViewH.getText().toString()),
                            Double.parseDouble(textViewS.getText().toString()),
                            Double.parseDouble(textViewV.getText().toString()));
                    ball.x=300;
                    ball.y=200;
                    isBallSelected=true;
                    isSelected=true;

                }
                else
                {
                    return;
                }
                checkBoxPickColor.setChecked(false);
                buttonUseLastHSV.setVisibility(View.INVISIBLE);

            }
        });

        buttonSetRangeReturn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                linearLayout.removeAllViews();
                buttonSetRange.setVisibility(View.VISIBLE);
                buttonBlueToothConnect.setVisibility(View.VISIBLE);
                buttonSetPID.setVisibility(View.VISIBLE);
                buttonDataTransControl.setVisibility(View.VISIBLE);
                checkBoxWideAngleErrorFix.setVisibility(View.VISIBLE);
                radioButtonDoNothing.setChecked(true);

            }
        });





        //SetPID
        editTextP=(EditText)viewSetPID.findViewById(R.id.editTextP);
        editTextI=(EditText)viewSetPID.findViewById(R.id.editTextI);
        editTextD=(EditText)viewSetPID.findViewById(R.id.editTextD);
        editTextIThreshold=(EditText)viewSetPID.findViewById(R.id.editTextIThreshold);

        buttonSendP=(Button)viewSetPID.findViewById(R.id.buttonSendP);
        buttonSendI=(Button)viewSetPID.findViewById(R.id.buttonSendI);
        buttonSendD=(Button)viewSetPID.findViewById(R.id.buttonSendD);
        buttonSendIThreshold=(Button)viewSetPID.findViewById(R.id.buttonSendIThreshold);
        buttonSetPIDReturn=(Button)viewSetPID.findViewById(R.id.buttonSetPIDReturn);

        buttonSendP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String text = editTextP.getText().toString();
                if(text==""){
                    Toast.makeText(getApplicationContext(),"发送中止：输入为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                Pattern pattern = Pattern.compile("[0-9]*");
                if(!pattern.matcher(text).matches()){//判断是否为数字

                    Toast.makeText(getApplicationContext(),"发送中止：输入的不是数字", Toast.LENGTH_SHORT).show();
                    return;
                }

                Message msg = sendHandler.obtainMessage();
                msg.what=1;
                Bundle data = new Bundle();
                data.putString("data","*P"+text+ "P"+ text + "#" );
                msg.setData(data);
                sendHandler.sendMessage(msg);

            }
        });


        buttonSendI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String text = editTextI.getText().toString();
                if(text==""){
                    Toast.makeText(getApplicationContext(),"发送中止：输入为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                Pattern pattern = Pattern.compile("[0-9]*");
                if(!pattern.matcher(text).matches()){//判断是否为数字

                    Toast.makeText(getApplicationContext(),"发送中止：输入的不是数字", Toast.LENGTH_SHORT).show();
                    return;
                }

                Message msg = sendHandler.obtainMessage();
                msg.what=1;
                Bundle data = new Bundle();
                data.putString("data","*I"+text+ "I"+ text + "#" );
                msg.setData(data);
                sendHandler.sendMessage(msg);


            }
        });

        buttonSendD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String text = editTextD.getText().toString();
                if(text==""){
                    Toast.makeText(getApplicationContext(),"发送中止：输入为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                Pattern pattern = Pattern.compile("[0-9]*");
                if(!pattern.matcher(text).matches()){//判断是否为数字

                    Toast.makeText(getApplicationContext(),"发送中止：输入的不是数字", Toast.LENGTH_SHORT).show();
                    return;
                }

                Message msg = sendHandler.obtainMessage();
                msg.what=1;
                Bundle data = new Bundle();
                data.putString("data","*D"+text+ "D"+ text + "#" );
                msg.setData(data);
                sendHandler.sendMessage(msg);


            }
        });

        buttonSendIThreshold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String text = editTextIThreshold.getText().toString();
                if(text==""){
                    Toast.makeText(getApplicationContext(),"发送中止：输入为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                Pattern pattern = Pattern.compile("[0-9]*");
                if(!pattern.matcher(text).matches()){//判断是否为数字

                    Toast.makeText(getApplicationContext(),"发送中止：输入的不是数字", Toast.LENGTH_SHORT).show();
                    return;
                }




                Message msg = sendHandler.obtainMessage();
                msg.what=1;
                Bundle data = new Bundle();
                data.putString("data","*T"+text+ "T"+ text + "#" );
                msg.setData(data);
                sendHandler.sendMessage(msg);


            }
        });


        buttonSetPIDReturn.setOnClickListener(returnOnClickListener);


        //dataTransControl
        editTextTargetPosition=(EditText)viewDataTransControl.findViewById(R.id.editTextTargetPosition);
        editTextStep=(EditText)viewDataTransControl.findViewById(R.id.editTextStep);
        editTextInstruction=(EditText)viewDataTransControl.findViewById(R.id.editTextInstruction);
        editTextData=(EditText)viewDataTransControl.findViewById(R.id.editTextData);

        buttonStartSendPosition=(Button) viewDataTransControl.findViewById(R.id.buttonStartSendPosition);
        buttonStopSendPosition=(Button) viewDataTransControl.findViewById(R.id.buttonStopSendPosition);
        buttonSetTargetPosition=(Button) viewDataTransControl.findViewById(R.id.buttonSetTargetPosition);
        buttonForeward=(Button) viewDataTransControl.findViewById(R.id.buttonForeward);
        buttonClear=(Button) viewDataTransControl.findViewById(R.id.buttonClear);
        buttonReverse=(Button) viewDataTransControl.findViewById(R.id.buttonReverse);
        buttonEmergencySend=(Button) viewDataTransControl.findViewById(R.id.buttonEmergencySend);
        buttonDataTransControlReturn=(Button) viewDataTransControl.findViewById(R.id.buttonDataTransControlReturn);

        buttonStartSendPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startSendPositionUI();


            }
        });

        buttonStopSendPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stopSendPositionUI();

            }
        });

        buttonSetTargetPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String text = editTextTargetPosition.getText().toString();
                if(text==""){
                    Toast.makeText(getApplicationContext(),"发送中止：输入为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                Pattern pattern = Pattern.compile("[0-9]*");
                if(!pattern.matcher(text).matches()){//判断是否为数字

                    Toast.makeText(getApplicationContext(),"发送中止：输入的不是数字", Toast.LENGTH_SHORT).show();
                    return;
                }


                Message msg = sendHandler.obtainMessage();
                msg.what=1;
                Bundle data = new Bundle();
                data.putString("data","*G"+text+ "G"+ text + "#" );
                msg.setData(data);
                sendHandler.sendMessage(msg);

            }
        });

        buttonForeward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String text = editTextStep.getText().toString();
                if(text==""){
                    Toast.makeText(getApplicationContext(),"发送中止：输入为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                Pattern pattern = Pattern.compile("[0-9]*");
                if(!pattern.matcher(text).matches()){//判断是否为数字

                    Toast.makeText(getApplicationContext(),"发送中止：输入的不是数字", Toast.LENGTH_SHORT).show();
                    return;
                }


                Message msg = sendHandler.obtainMessage();
                msg.what=1;
                Bundle data = new Bundle();
                data.putString("data","*A"+text+ "A"+ text + "#" );
                msg.setData(data);
                sendHandler.sendMessage(msg);


            }
        });

        buttonReverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String text = editTextStep.getText().toString();
                if(text==""){
                    Toast.makeText(getApplicationContext(),"发送中止：输入为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                Pattern pattern = Pattern.compile("[0-9]*");
                if(!pattern.matcher(text).matches()){//判断是否为数字

                    Toast.makeText(getApplicationContext(),"发送中止：输入的不是数字", Toast.LENGTH_SHORT).show();
                    return;
                }


                Message msg = sendHandler.obtainMessage();
                msg.what=1;
                Bundle data = new Bundle();
                data.putString("data","*B"+text+ "B"+ text + "#" );
                msg.setData(data);
                sendHandler.sendMessage(msg);

            }
        });


        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                Message msg = sendHandler.obtainMessage();
                msg.what=1;
                Bundle data = new Bundle();
                data.putString("data","*C0C0#" );
                msg.setData(data);
                sendHandler.sendMessage(msg);

            }
        });



        buttonEmergencySend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String instruction=editTextInstruction.getText().toString();
                String dt=editTextData.getText().toString();
                if (dt.equals("")) {
                    dt="0";
                }

                Pattern pattern = Pattern.compile("[a-zA-Z]+");
                if(!pattern.matcher(instruction).matches()){//判断是否为字母

                    Toast.makeText(getApplicationContext(),"指令格式错误", Toast.LENGTH_SHORT).show();
                    return;
                }

                Message msg = sendHandler.obtainMessage();
                msg.what=1;
                Bundle data = new Bundle();
                data.putString("data","*" +instruction.toUpperCase()+dt+instruction.toUpperCase()+dt+ "#" );
                msg.setData(data);
                sendHandler.sendMessage(msg);

            }
        });

        buttonDataTransControlReturn.setOnClickListener(returnOnClickListener);


    }


    private void startSendPositionUI(){

        isPositionSending=true;
        //editTextTargetPosition.setEnabled(false);
        //editTextStep.setEnabled(false);
        //buttonSetTargetPosition.setEnabled(false);
        //buttonForeward.setEnabled(false);
        buttonClear.setEnabled(false);
        //buttonReverse.setEnabled(false);
        //buttonSetPID.setEnabled(false);

        buttonStartSendPosition.setEnabled(false);
        buttonStopSendPosition.setEnabled(true);

    }

    private void stopSendPositionUI(){

        isPositionSending=false;
        //editTextTargetPosition.setEnabled(true);
        //editTextStep.setEnabled(true);
        //buttonSetTargetPosition.setEnabled(true);
        //buttonForeward.setEnabled(true);
        buttonClear.setEnabled(true);
        //buttonReverse.setEnabled(true);
        //buttonSetPID.setEnabled(true);

        buttonStartSendPosition.setEnabled(true);
        buttonStopSendPosition.setEnabled(false);
    }

    private void connectStartUI(){

        buttonBlueToothConnect.setEnabled(false);

    }

    private void connectFailUI() {

        buttonBlueToothConnect.setEnabled(true);

    }

    private void connectionEstablishUI(){
        //主界面
        buttonSetPID.setEnabled(true);
        buttonDataTransControl.setEnabled(true);

        //setPID
        editTextP.setEnabled(true);
        editTextI.setEnabled(true);
        editTextD.setEnabled(true);
        editTextIThreshold.setEnabled(true);
        buttonSendP.setEnabled(true);
        buttonSendI.setEnabled(true);
        buttonSendD.setEnabled(true);
        buttonSendIThreshold.setEnabled(true);

        //dataTransControl
        editTextTargetPosition.setEnabled(true);
        editTextStep.setEnabled(true);
        buttonStartSendPosition.setEnabled(true);
        buttonStopSendPosition.setEnabled(false);
        buttonSetTargetPosition.setEnabled(true);
        buttonForeward.setEnabled(true);
        buttonClear.setEnabled(true);
        buttonReverse.setEnabled(true);
        buttonEmergencySend.setEnabled(true);

    }

    private void connectionLoseUI(){

        buttonSetPID.setEnabled(false);
        buttonDataTransControl.setEnabled(false);
        buttonBlueToothConnect.setEnabled(true);

        //setPID
        editTextP.setEnabled(false);
        editTextI.setEnabled(false);
        editTextD.setEnabled(false);
        editTextIThreshold.setEnabled(false);
        buttonSendP.setEnabled(false);
        buttonSendI.setEnabled(false);
        buttonSendD.setEnabled(false);
        buttonSendIThreshold.setEnabled(false);

        //dataTransControl
        editTextTargetPosition.setEnabled(false);
        editTextStep.setEnabled(false);
        buttonStartSendPosition.setEnabled(false);
        buttonStopSendPosition.setEnabled(false);
        buttonSetTargetPosition.setEnabled(false);
        buttonForeward.setEnabled(false);
        buttonClear.setEnabled(false);
        buttonReverse.setEnabled(false);
        buttonEmergencySend.setEnabled(false);

    }

    class ReturnOnClickListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            linearLayout.removeAllViews();
            buttonSetRange.setVisibility(View.VISIBLE);
            buttonBlueToothConnect.setVisibility(View.VISIBLE);
            buttonSetPID.setVisibility(View.VISIBLE);
            buttonDataTransControl.setVisibility(View.VISIBLE);
            checkBoxWideAngleErrorFix.setVisibility(View.VISIBLE);

        }
    }



    class ConnectingThread extends Thread{

        @Override
        public void run() {
            try {
                bluetoothSocket.connect();
                SendThread sendThread = new SendThread();
                sendThread.start();
                RecvThread recvThread = new RecvThread();
                recvThread.start();

                //连接成功
                isConnected=true;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        connectionEstablishUI();

                    }
                });



                Log.e("BluetoothTest","socket连接！");

            } catch (IOException e) {

                Log.e("BluetoothTest", "connecting IOException:"+ e);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
                String time = sdf.format(new java.util.Date());
                String str=time + " bluetooth:" + "蓝牙连接失败" + '\n';
                queue.offer(str);

                //更新信息条
                runOnUiThread(new UpdateInformationBarRunnable());



                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        //连接失败
                        connectFailUI();

                    }
                });

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    //一般不会有的异常
                    Log.e("BluetoothTest", "unable to close() "+
                            "socket during connection failure", e1);
                }


            }

        }

    }




    class SendThread extends Thread{
        @Override
        public void run() {

            Log.e("BluetoothTest","sendThread started!");
            Looper.prepare();

            try {
                outPutStream = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                //一般不会有的异常
                Log.e("BluetoothTest","socket getOutputStream()时 IOException:"+e);

            }

            // byte [] sendBuffer = new byte[1024];

            //     for(int i=0;i<10;i++)
            //     {
            //         byte j = (byte)((i+48) & 0xFF);
            //         sendBuffer[i]=j;
            //     }


            sendHandler= new Handler(){

                @Override
                public void handleMessage(Message msg) {


                    if(msg.what==1) {

                        Bundle receiveBundle= msg.getData();
                        String s=receiveBundle.getString("data");

                        try {
                            //     outPutStream.write(sendBuffer,0,10);
                            outPutStream.write(s.getBytes());
                            Log.e("BluetoothTest","发送:" +s);
                        } catch (IOException e) {

                            //连接中断
                            isConnected=false;

                            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
                            String time = sdf.format(new java.util.Date());
                            String str=time + " bluetooth:" + "蓝牙发送错误 服务器断开连接" + '\n';
                            queue.offer(str);

                            //更新信息条
                            runOnUiThread(new UpdateInformationBarRunnable());

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    connectionLoseUI();

                                }
                            });


                            Log.e("BluetoothTest","socket send时 IOException:"+e);

                        }


                    }

                }
            };
            Looper.loop();


        }
    }



    Queue<String> queue = new LinkedBlockingQueue<>();


    class RecvThread extends Thread{
        @Override
        public void run() {

            Log.e("BluetoothTest","recvThread started!");


            try {
                bufferedReader= new BufferedReader(new InputStreamReader(bluetoothSocket.getInputStream()));
            } catch (IOException e) {
                Log.e("BluetoothTest","socket getInputStream()时 IOException:"+e);
            }


            try {
                String str;
                while((str=bufferedReader.readLine())!=null)
                {
                    Log.e("BluetoothTest","recv成功，内容："+str);
                    //str!=null才有效！
                    str=str.trim();

                    if(str.equals("beep")){
                        playSound(R.raw.reachtarget);
                    }
                    else {

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
                        String time = sdf.format(new java.util.Date());

                        str = time + " recv:" + str + '\n';
                        queue.offer(str);

                        runOnUiThread(new UpdateInformationBarRunnable());//更新信息条
                    }

                }

            } catch (IOException e) {

                //连接中断
                isConnected=false;


                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
                String time = sdf.format(new java.util.Date());
                String str=time + " bluetooth:" + "蓝牙连接丢失" + '\n';
                queue.offer(str);

                //更新信息条
                runOnUiThread(new UpdateInformationBarRunnable());

                //更新UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        connectionLoseUI();

                    }
                });


                Log.e("BluetoothTest","socket recv时 IOException:"+e);
            }


        }
    }

    class UpdateInformationBarRunnable implements Runnable {

        @Override
        public void run() {


            int queueI=0;
            String strAll="";
            for(String x :queue)
            {
                queueI++;
                strAll+=x;
            }

            textViewInformation.setText(strAll);
            if(queueI>=8)
            {
                queue.poll();
                queueI--;
            }

        }
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

    private void playSound(int resid){
        if(mediaPlayer!=null){
            if(!mediaPlayer.isPlaying()) {
                mediaPlayer.release();
                //高通处理器不支持mediaPlayer=new MediaPlayer();
                mediaPlayer=MediaPlayer.create(MainActivity.this,resid);

                mediaPlayer.start();

            }
        }
        else
        {
            mediaPlayer=MediaPlayer.create(MainActivity.this,resid);

            mediaPlayer.start();
        }
    }


    private int mod = 0;

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba=inputFrame.rgba();//mRgba是给回调函数onTouch传递数据的全局变量
        Mat mRgbaTemp = inputFrame.rgba();
        if(isSelected){

            if(leftTrackZone.width != 0 && leftTrackZone.height!= 0) {
                mSelected = mRgbaTemp.submat(leftTrackZone);
                Imgproc.cvtColor(mSelected, mHSVMatLeft, Imgproc.COLOR_RGB2HSV_FULL);


            }
            if(rightTrackZone.width != 0 && rightTrackZone.height!= 0) {
                mSelected = mRgbaTemp.submat(rightTrackZone);
                Imgproc.cvtColor(mSelected, mHSVMatRight, Imgproc.COLOR_RGB2HSV_FULL);
            }
            if(ballTrackZone.width != 0 && ballTrackZone.height!= 0) {
                mSelected = mRgbaTemp.submat(ballTrackZone);
                Imgproc.cvtColor(mSelected, mHSVMatBall, Imgproc.COLOR_RGB2HSV_FULL);
            }


            if (isLeftSelected){
                if(leftTrackZone.width != 0 && leftTrackZone.height!= 0 ) {
                    if(leftTouchLock) {
                        leftTouchLock=false;
                    }
                    else {
                        myInRangeByBolbColorAndHSV(mBlobColorHsvLeft, TOUCH_LEFT);
                        setLeadRailByThre(leftTrackZone, leftLeadRail, TOUCH_LEFT);
                    }
                }
                    setTrackZoneByLeadRail(mRgbaTemp, leftTrackZone, leftLeadRail, TOUCH_LEFT);


            }
            if (isRightSelected){
                if(rightTrackZone.width != 0 && rightTrackZone.height!= 0) {
                    if(rightTouchLock){
                        rightTouchLock=false;
                    }
                    else {
                        myInRangeByBolbColorAndHSV(mBlobColorHsvRight, TOUCH_RIGHT);
                        setLeadRailByThre(rightTrackZone, rightLeadRail, TOUCH_RIGHT);
                    }
                }

                    setTrackZoneByLeadRail(mRgbaTemp, rightTrackZone, rightLeadRail, TOUCH_RIGHT);

            }
            if(isBallSelected){
                if(ballTrackZone.width != 0 && ballTrackZone.height!= 0) {
                    if(ballTouchLock){
                        ballTouchLock=false;
                    }
                    else {
                        myInRangeByBolbColorAndHSV(mBlobColorHsvBall, TOUCH_BALL);
                        setLeadRailByThre(ballTrackZone, ball, TOUCH_BALL);
                    }
                }

                    setTrackZoneByLeadRail(mRgbaTemp, ballTrackZone, ball, TOUCH_BALL);

            }

        }

        //处理小球位置信息

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if(isLeftSelected){
                    textViewLeft.setText("导轨左:("+leftLeadRail.x  + ","+leftLeadRail.y + ")");
                }
                if(isRightSelected){
                    textViewRight.setText("导轨右:("+rightLeadRail.x  + ","+rightLeadRail.y + ")");
                }
                if(isBallSelected){
                    textViewBall.setText("小球:("+ball.x  + ","+ball.y + ")");
                }


                if(isLeftSelected && isRightSelected && isBallSelected){
                    double leftToBally;
                    double leftToRightx;
                    double leftToRighty;
                    double k;
                    double k_1;
                    double modifiedx;
                    double position;
                    if(rightLeadRail.y-leftLeadRail.y==0) {
                        position=(ball.x-leftLeadRail.x)/(rightLeadRail.x-leftLeadRail.x);
                    }
                    else{
                        leftToBally=ball.y-leftLeadRail.y;
                        leftToRightx=rightLeadRail.x-leftLeadRail.x;
                        leftToRighty=rightLeadRail.y-leftLeadRail.y;
                        k= leftToRighty/leftToRightx;
                        k_1 = leftToRightx/leftToRighty;
                        modifiedx=(leftToBally+ k_1 * ball.x + k *leftLeadRail.x)/(k + k_1);
                        position=(modifiedx-leftLeadRail.x)/(rightLeadRail.x-leftLeadRail.x);
                    }


                    position*=600;//转换成毫米

                    double correctedPosition;
                    if(checkBoxWideAngleErrorFix.isChecked()){
                        correctedPosition=(double)576/564*(position-16)+10;
                    }
                    else
                    {
                        correctedPosition=position;
                    }

                    if(correctedPosition<leftEdgePosition || correctedPosition > rightEdgePosition){
                        playSound(R.raw.reachedge);
                    }
                    
                    textViewPosition.setText("位置:" + correctedPosition);

                    mod++;
                    if(mod > 3) mod=1;
                    if(isConnected && isPositionSending && mod %3==0) {


                        String text = Integer.toString((int) correctedPosition);

                        Message msg = sendHandler.obtainMessage();
                        msg.what = 1;
                        Bundle data = new Bundle();
                        data.putString("data", "*L" + text + "L" + text + "#");
                        msg.setData(data);
                        sendHandler.sendMessage(msg);
                    }


                }

            }
        });
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

            //playSound(R.raw.tracklose);

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

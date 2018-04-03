package com.subodhjena.objectcounter;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";

    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;

    private CameraBridgeViewBase mOpenCvCameraView;

    String p = Manifest.permission.CAMERA;
    Canvas canvas;
    Paint paint = null, onPausePaint = null, cursorPaint = null,
            eraserPaint = null, delEraserPaint = null, paintCircle = null;
    ImageView imageView;
    Bitmap bitmap;
    boolean isFirstLaunch = true, onPause = false, eraserMode = false;
    float prevx = 0, prevy = 0, lineWidth = 5, cursorLastX, cursorLastY;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setAlpha(0);
                    mOpenCvCameraView.setMaxFrameSize(1280, 720);
                    //mOpenCvCameraView.setOnTouchListener(MainActivity.this);
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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        checkPermissions();

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        int width = 1280;
        int height = 720;

        //bitmap = BitmapFactory.decodeResource(getResources(),
                //R.drawable.cleartype);

        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        canvas = new Canvas(bitmap);

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(lineWidth);

        paintCircle = new Paint();
        paintCircle.setStyle(Paint.Style.FILL);
        paintCircle.setColor(Color.BLACK);

        onPausePaint = new Paint();
        onPausePaint.setStrokeWidth(1);
        onPausePaint.setStyle(Paint.Style.STROKE);
        onPausePaint.setColor(Color.WHITE);

        delEraserPaint = new Paint();
        delEraserPaint.setStrokeWidth(1);
        delEraserPaint.setStyle(Paint.Style.FILL);
        delEraserPaint.setColor(Color.WHITE);

        cursorPaint = new Paint();
        cursorPaint.setStrokeWidth(1);
        cursorPaint.setStyle(Paint.Style.STROKE);
        cursorPaint.setColor(Color.RED);

        eraserPaint = new Paint();
        eraserPaint.setStyle(Paint.Style.FILL);
        eraserPaint.setColor(Color.GRAY);

        imageView = (ImageView) findViewById(R.id.imageView);

        Button pauseButton = (Button) findViewById(R.id.pauseButton);
        pauseButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    onPause = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    onPause = false;
                    canvas.drawCircle(cursorLastX, cursorLastY, 10, onPausePaint);
                }
                return true;
            }
        });

        Button eraserButton = (Button) findViewById(R.id.eraserButton);
        eraserButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                eraserMode = !eraserMode;
                canvas.drawCircle(prevx, prevy, 25, delEraserPaint);
            }
        });

        Button clearButton = (Button) findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                canvas.drawColor(Color.WHITE);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                    }
                });
            }
        });

        Button incWidth = (Button) findViewById(R.id.incWidth);
        incWidth.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                lineWidth++;
                paint.setStrokeWidth(lineWidth);
            }
        });

        Button decWidth = (Button) findViewById(R.id.decWidth);
        decWidth.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                lineWidth--;
                paint.setStrokeWidth(lineWidth);
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
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);

        mDetector.setHsvColor(new Scalar(36.5, 210, 210));

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            //Log.e(TAG, "Contours count: " + contours.size());

            if (contours.size() == 1) {
                //Log.e(TAG, "Coordinates x: " + contours.get(0).toList().get(0).x
                //        + " y: " + contours.get(0).toList().get(0).y);
                //canvas.drawCircle((float) contours.get(0).toList().get(0).x, (float) contours.get(0).toList().get(0).y, 5, paint);
                float tempx = (float) contours.get(0).toList().get(0).x, tempy = (float) contours.get(0).toList().get(0).y;

                //if (!)

                if (!onPause && !eraserMode) {

                    canvas.drawLine(prevx, prevy, tempx, tempy, paint);
                    canvas.drawCircle(tempx, tempy, lineWidth / 2, paintCircle);
                    prevx = tempx;
                    prevy = tempy;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                        }
                    });
                } else if (onPause) {

                    canvas.drawCircle(prevx, prevy, 10, onPausePaint);
                    canvas.drawCircle(tempx, tempy, 10, cursorPaint);
                    prevx = tempx;
                    prevy = tempy;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                        }
                    });

                    cursorLastX = tempx;
                    cursorLastY = tempy;
                } else if (eraserMode) {
                    canvas.drawCircle(prevx, prevy, 25, delEraserPaint);
                    canvas.drawCircle(tempx, tempy, 25, eraserPaint);

                    prevx = tempx;
                    prevy = tempy;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                        }
                    });
                }
            }

//        if (isFirstLaunch) {
//            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
//
//            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
//            colorLabel.setTo(mBlobColorRgba);
//
//            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
//            mSpectrum.copyTo(spectrumLabel);
//
//            isFirstLaunch = false;
//        }

        //Core.flip(mRgba, mRgba, 1);

        return mRgba;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        result = ContextCompat.checkSelfPermission(this, p);
        if (result != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(p);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            return false;
        }
        return true;
    }
}

package com.subodhjena.objectcounter;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class DrawActivity extends Activity implements CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";

    // переменные библиотеки OpenCV
    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;

    private CameraBridgeViewBase mOpenCvCameraView;


    // наши переменные
    String p = Manifest.permission.CAMERA;
    Canvas canvas;
    Paint
            // переменные для пера
            paint = null,
            paintCircle = null,
            // переменные для курсора
            cursorPaint = null,
            delCursorPaint = null,
            // переменные для ластика
            eraserPaint = null,
            delEraserPaint = null;
    ImageView imageView;
    Bitmap bitmap;
    boolean onPause = false, eraserModeON = false;
    float prevx = 0, prevy = 0, lineWidth = 5, cursorPrevX, cursorPrevY;

    // запуск OpenCV
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
                    //mOpenCvCameraView.setOnTouchListener(DrawActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public DrawActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_draw);
        // запрос необходимых прав доступа
        checkPermissions();

        mOpenCvCameraView =
                (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        int width = 1280;
        int height = 720;
        // создание изображения и холста
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        canvas = new Canvas(bitmap);

        // инициализация кистей
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(lineWidth);
        paintCircle = new Paint(); // исправление бага с "разрезанными" линиями при большой толщине
        paintCircle.setStyle(Paint.Style.FILL);
        paintCircle.setColor(Color.BLACK);

        // инициализация курсора
        cursorPaint = new Paint();
        cursorPaint.setStrokeWidth(1);
        cursorPaint.setStyle(Paint.Style.STROKE);
        cursorPaint.setColor(Color.RED);
        delCursorPaint = new Paint(); // инициализация "стирателя" курсора
        delCursorPaint.setStrokeWidth(1);
        delCursorPaint.setStyle(Paint.Style.STROKE);
        delCursorPaint.setColor(Color.WHITE);

        // инициализация ластика
        eraserPaint = new Paint();
        eraserPaint.setStyle(Paint.Style.FILL);
        eraserPaint.setColor(Color.GRAY);
        delEraserPaint = new Paint(); // инициализация "стирателя" ластика
        delEraserPaint.setStrokeWidth(1);
        delEraserPaint.setStyle(Paint.Style.FILL);
        delEraserPaint.setColor(Color.WHITE);

        imageView = (ImageView) findViewById(R.id.imageView);

        // кнопка паузы / переключение на курсор <-- новое*
        Button pauseButton = (Button) findViewById(R.id.pauseButton);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onPause = !onPause;
                // стирание курсора
                canvas.drawCircle(cursorPrevX, cursorPrevY, 10, delCursorPaint);
            }
        });

        // кнопка ластика
        Button eraserButton = (Button) findViewById(R.id.eraserButton);
        eraserButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        eraserModeON = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        eraserModeON = false;
                        canvas.drawCircle(prevx, prevy, 25, delEraserPaint);
                        break;
                }
                return false;
            }
        });
        /*
        eraserButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                eraserModeON = !eraserModeON;
                // стирание ластика при нажатии на кнопку
                canvas.drawCircle(prevx, prevy, 25, delEraserPaint);
            }
        });
        */
        // кнопка очистки
        Button clearButton = (Button) findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                canvas.drawColor(Color.WHITE);
                // обновление картинки, отображаемой в ImageVIew
                // в потоке UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                    }
                });
            }
        });

        // кнопка увеличения толщины пера
        Button incWidth = (Button) findViewById(R.id.incWidth);
        incWidth.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                lineWidth++;
                paint.setStrokeWidth(lineWidth);
            }
        });

        // кнопка уменьшения тошщины пера
        Button decWidth = (Button) findViewById(R.id.decWidth);
        decWidth.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                lineWidth--;
                paint.setStrokeWidth(lineWidth);
            }
        });
    }

    // выключение камеры при сворачивании программы
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    // проверка OpenCV и включение камеры при разворачивании программы
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

    // отключаем камеру при выключении
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    // инициализация детектора при включении камеры
    public void onCameraViewStarted(int width, int height) {
        // матрица, содержащая информацию о цвете каждого пикселя текущего кадра
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

    // освобождение памяти при выключении камеры
    public void onCameraViewStopped() {
        mRgba.release();
    }

    // выполняется при снятии каждого кадра
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        // обработка кадра (?)
        mDetector.process(mRgba);

        List<MatOfPoint> contours = mDetector.getContours();
        //Log.e(TAG, "Contours count: " + contours.size());

        // основное рисование (рисуется только тогда, когда в кадре 1 контур)
        if (contours.size() == 1) {
            //Log.e(TAG, "Coordinates x: " + contours.get(0).toList().get(0).x
            //        + " y: " + contours.get(0).toList().get(0).y);

            // присваивание значений координат текущей точки
            float   tempx = (float) contours.get(0).toList().get(0).x,
                    tempy = (float) contours.get(0).toList().get(0).y;

            // рисование пером (используется рисование линии между текущей и предыдущей точкой)
            if (!onPause && !eraserModeON) {

                canvas.drawLine(prevx, prevy, tempx, tempy, paint);
                canvas.drawCircle(tempx, tempy, lineWidth / 2, paintCircle);
                // запись предыдущих значений координат пера
                prevx = tempx;
                prevy = tempy;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                    }});
            // переключение на курсор
            } else if (onPause) {

                canvas.drawCircle(prevx, prevy, 10, delCursorPaint);
                canvas.drawCircle(tempx, tempy, 10, cursorPaint);
                prevx = tempx;
                prevy = tempy;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                    }
                });

                // запись предыдущих значений координат курсора
                cursorPrevX = tempx;
                cursorPrevY = tempy;
            // переключение на режим ластика
            } else if (eraserModeON) {

                canvas.drawCircle(prevx, prevy, 25, delEraserPaint);
                canvas.drawCircle(tempx, tempy, 25, eraserPaint);
                // запись предыдущих значений координат ластика
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

    /*
    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
    */

    // проверка наличия разрешения на доступ к камере - если нет, то попросить разрешение
    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        result = ContextCompat.checkSelfPermission(this, p);
        if (result != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(p);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                            listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                            100);
            return false;
        }
        return true;
    }
}

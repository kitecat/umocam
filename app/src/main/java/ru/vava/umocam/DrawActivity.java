package ru.vava.umocam;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.gson.Gson;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DrawActivity extends CameraActivity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    // переменные библиотеки OpenCV
    boolean mIsColorSelected = false;
    private Mat mRgba;
    Scalar mBlobColorRgba;
    Scalar mBlobColorHsv;
    private ColorBlobDetector mDetector;
    Mat mSpectrum;
    Size SPECTRUM_SIZE;
    Scalar CONTOUR_COLOR;

    private CameraBridgeViewBase mOpenCvCameraView;

    // наши переменные
    String[] permissionsNeed = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    Canvas canvas;

    // переменные для пера
    Paint paint = null;
    Paint paintCircle = null;
    ImageView imageView;
    Bitmap bitmap;
    boolean onPause = true, eraserModeON = false;
    float prevx = 0, prevy = 0, lineWidth = 5;


    // переменная для загрузки выбранного цвета
    Scalar selectedColor = null;

    public DrawActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        // запуск OpenCV
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_draw);

        // запрос необходимых прав доступа
        checkPermissions();

        mOpenCvCameraView = findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        SharedPreferences mSettings = getSharedPreferences("appPrefs", Context.MODE_PRIVATE);
        if (mSettings.contains("selectedColorJson")) {
            Gson gson = new Gson();
            String json = mSettings.getString("selectedColorJson", "");
            if (!json.equals(""))
                selectedColor = gson.fromJson(json, Scalar.class);
        }

        int width = 1280;
        int height = 720;

        // создание изображения и холста
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.TRANSPARENT);
        canvas = new Canvas(bitmap);

        // инициализация кистей
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(scalarToPaintColor(selectedColor));
        paint.setStrokeWidth(lineWidth);
        paintCircle = new Paint(); // исправление бага с "разрезанными" линиями при большой толщине
        paintCircle.setStyle(Paint.Style.FILL);
        paintCircle.setColor(scalarToPaintColor(selectedColor));

        imageView = findViewById(R.id.imageView);

        // кнопка паузы / переключение на курсор
        final Button pauseButton = findViewById(R.id.pauseButton);
        pauseButton.setText(getString(R.string.start));
        pauseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onPause = !onPause;

                if (onPause) {
                    pauseButton.setText(R.string.start);
                } else {
                    pauseButton.setText(R.string.pause);
                }
            }
        });

        // кнопка очистки
        Button clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                canvas.drawColor(Color.TRANSPARENT);
//                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.eraseColor(Color.TRANSPARENT);
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

        // ползунок ширины линии
        SeekBar widthBar = findViewById(R.id.widthBar);
        widthBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                lineWidth = i;
                paint.setStrokeWidth(lineWidth);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        /*
        // кнопка включения вспышки
        Button flashButton = (Button) findViewById(R.id.flashButton);
        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        */
        // кнопка сохранить
        Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveToInternalStorage(bitmap);
            }
        });
    }

    // Convert OpenCV Scalar (BGR) to Android Paint color (ARGB)
    public static int scalarToPaintColor(Scalar hsvScalar) {
        // OpenCV HSV range: H [0,179], S [0,255], V [0,255]
        float h = (float) (hsvScalar.val[0] * 2);  // Convert H to 0-360
        float s = (float) (hsvScalar.val[1] / 255.0);  // Normalize S to [0,1]
        float v = (float) (hsvScalar.val[2] / 255.0);  // Normalize V to [0,1]

        // Convert HSV to RGB (Android handles alpha automatically)
        return Color.HSVToColor(new float[]{h, s, v});
    }

    // выключение камеры при сворачивании программы
    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    // проверка OpenCV и включение камеры при разворачивании программы
    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initLocal()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initLocal();
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
        }
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.enableView();
        }
    }

    // отключаем камеру при выключении
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
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
        CONTOUR_COLOR = new Scalar(255, 0, 0, 255);

        // присваиваем цвет который надо определять
        if (selectedColor != null) {
            mDetector.setHsvColor(selectedColor);
        } else {
            mDetector.setHsvColor(new Scalar(36.5, 210, 210));
        }

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
            float tempx = (float) contours.get(0).toList().get(0).x,
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
                    }
                });
                // переключение на курсор
            } else if (onPause) {
                prevx = tempx;
                prevy = tempy;
            }
        }

        return mRgba;
    }

    // Метод для сохранения рисунков в галерею телефона
    private String saveToInternalStorage(Bitmap bitmapImage) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());

        String root = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/Umo Cam Pics");
        myDir.mkdirs();
        File itogFile = new File(myDir, (System.currentTimeMillis() / 1000) + ".png");

        File directory = cw.getDir("Umo Cam Pics", Context.MODE_PRIVATE);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(itogFile);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
                Toast.makeText(getApplicationContext(), getString(R.string.saved), Toast.LENGTH_SHORT)
                        .show();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory.getAbsolutePath();
    }

    // проверка наличия разрешения на доступ к камере - если нет, то попросить разрешение
    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();

        for (int i = 0; i < permissionsNeed.length; i++) {
            result = ContextCompat.checkSelfPermission(this, permissionsNeed[i]);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permissionsNeed[i]);
            }
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

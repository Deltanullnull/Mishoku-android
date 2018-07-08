package com.deltanullnull.kenshoku;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.os.Trace;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Surface;
import android.view.View;
import android.view.ViewDebug;
import android.webkit.PermissionRequest;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import org.jsoup.Jsoup;


import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements ImageReader.OnImageAvailableListener, Camera.PreviewCallback {

    private static final int PERMISSION_REQUEST = 1;

    private static final String TAG = "MainActivity";

    private boolean useCamera2API;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

    private static final boolean MAINTAIN_ASPECT = true;

    private int previewWidth = 0;
    private int previewHeight = 0;

    private ImageClassifier classifier;

    private Handler handler;
    private HandlerThread handlerThread;

    private int sensorOrientation;

    private Matrix cropToFrameTransform;

    private List<Recognition> mResults;

    private static final String MODEL_FILE = "file:///android_asset/mishoku_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/mishoku_labels.txt";

    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 127;
    private static final int IMAGE_STD = 127;

    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "final_result";

    private int[] rgbBytes;
    private byte[] lastPreviewFrame;
    private byte[][] yuvBytes =  new byte[3][];
    private int yRowStride;

    private Matrix frameToCropTransform;

    private Bitmap rgbFrameBitmap, croppedBitmap;

    private Runnable imageConverter, postInferenceCallback;

    private boolean isProcessingFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Log.d(TAG, "on create");
        if (hasPermission())
        {
            setFragment();
        }
        else
        {
            requestPermission();
        }

        Log.d(TAG, "ok!");

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);

        if (viewPager != null)
        {
            Log.d(TAG, "Setting adapter");
            viewPager.setAdapter(new RecipeAdapter(this));
        }

    }

    @Override
    public void onPreviewFrame(final byte [] bytes, final Camera camera)
    {
        Log.d(TAG, "onPreviewFrame " + this);
        if (isProcessingFrame)
        {
            Log.d(TAG, "Dropping frame");
            return;
        }

        try
        {
            //Log.d(TAG, "on preview frame");
            if (rgbBytes == null)
            {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewHeight * previewWidth];
                onPreviewSizeChosen(new Size(previewWidth, previewHeight), 90);
            }
        }
        catch (final Exception e)
        {
            return;
        }

        isProcessingFrame = true;
        lastPreviewFrame = bytes;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter = new Runnable()
        {
            @Override
            public void run()
            {
                ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
            }
        };

        postInferenceCallback = new Runnable()
        {
            @Override
            public void run()
            {
                camera.addCallbackBuffer(bytes);
                isProcessingFrame = false;
            }
        };

        processImage();

    }

    @Override
    public void onImageAvailable(ImageReader reader)
    {
        Log.d(TAG, "onImageAvailable");
        if (previewWidth == 0 || previewHeight == 0)
            return;

        if (rgbBytes == null)
        {
            rgbBytes = new int[previewHeight * previewWidth];
        }
        try
        {
            final Image image = reader.acquireLatestImage();

            if (image == null)
                return;

            if (isProcessingFrame)
            {
                image.close();
                return;
            }

            isProcessingFrame = true;
            Trace.beginSection("imageAvailable"); // What is it doing?
            final Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter = new Runnable()
            {
                @Override
                public void run()
                {
                    ImageUtils.convertYUV420ToARGB8888(
                            yuvBytes[0],
                            yuvBytes[1],
                            yuvBytes[2],
                            previewWidth,
                            previewHeight,
                            yRowStride,
                            uvRowStride,
                            uvPixelStride,
                            rgbBytes);
                }
            };

            postInferenceCallback = new Runnable() {
                @Override
                public void run() {
                    image.close();
                    isProcessingFrame = false;
                }
            };

            processImage();

        }
        catch (Exception e)
        {
            Log.d(TAG, "Error in ImageAvailable");
            Trace.endSection();
            return;
        }

        Trace.endSection();
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults)
    {
        Log.d(TAG, "onRequestPermissionResult");
        if (requestCode == PERMISSION_REQUEST)
        {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED)
            {
                Log.d(TAG,"permission ok");
                setFragment();
            }
            else
            {
                requestPermission();
            }

        }

    }

    private void readyForNextImage()
    {
        //Log.d(TAG, "next img");
        if (postInferenceCallback != null)
        {
            postInferenceCallback.run();
        }
    }

    private boolean hasPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) // If version higher than M
        {
            // Check, if permission for camera access and write access is granted
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        else
        {
            // By default true in lower versions
            return true;
        }
    }

    private void requestPermission()
    {
        //Log.d(TAG, "requesting permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (shouldShowRequestPermissionRationale(PERMISSION_STORAGE) || shouldShowRequestPermissionRationale(PERMISSION_CAMERA))
            {
                Toast.makeText(MainActivity.this, "Camera AND storage permissions are required", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSION_REQUEST);
        }
    }

    private void fillBytes(final Plane[] planes, final byte[][] yuvBytes)
    {
        for (int i = 0; i < planes.length; i++)
        {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null)
            {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    private String chooseCamera()
    {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try
        {
            for (final String cameraId : manager.getCameraIdList())
            {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT)
                {
                    continue;
                }

                final StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null)
                    continue;

                useCamera2API = (facing == CameraCharacteristics.LENS_FACING_EXTERNAL);

                return cameraId;
            }
        }
        catch (CameraAccessException e)
        {
            // TODO output error
            Log.d(TAG, "Couldn't access camera");
        }

        return null;
    }

    private void setFragment()
    {
        Log.d(TAG, "setting fragment");
        String cameraId = chooseCamera();
        if (cameraId == null)
        {
            Toast.makeText(this, "No camera detected", Toast.LENGTH_LONG).show();
            finish();
        }

        Fragment fragment;

        if (useCamera2API) {
            Log.d(TAG, "using CameraConnectionFragment");
            CameraConnectionFragment camera2Fragment =
                    CameraConnectionFragment.newInstance(
                            new CameraConnectionFragment.ConnectionCallback() {
                                @Override
                                public void onPreviewSizeChosen(final Size size, final int rotation) {
                                    previewHeight = size.getHeight();
                                    previewWidth = size.getWidth();
                                    onPreviewSizeChosen(size, rotation);
                                }
                            },
                            this,
                            getLayoutId(),
                            getDesiredPreviewFrameSize());

            camera2Fragment.setCamera(cameraId);
            fragment = camera2Fragment;
        } else
            {
            Log.d(TAG, "using LegacyFragment");
            fragment = new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
        }
        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    protected int[] getRgbBytes()
    {
        imageConverter.run();
        return rgbBytes;
    }

    private void processImage()
    {
        //Log.d(TAG, "debugging image");
        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        final long startTime = SystemClock.uptimeMillis();
                        mResults = classifier.recognizeImage(rgbFrameBitmap);

                        requestRender();
                        readyForNextImage();
                    }
                }
        );


    }

    private void requestRender()
    {
        final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
        if (overlay != null)
        {
            overlay.postInvalidate();
        }
    }

    private void onPreviewSizeChosen(final Size size, final int rotation)
    {
        final float textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()
        );

        /*classifier = ImageClassifier.create(
                getAssets(),
                MODEL_FILE,
                LABEL_FILE,
                INPUT_SIZE,
                IMAGE_MEAN,
                IMAGE_STD,
                INPUT_NAME,
                OUTPUT_NAME
        );*/

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888);

        frameToCropTransform = ImageUtils.getTransformationMatrix(
            previewWidth,
            previewHeight,
            INPUT_SIZE,
            INPUT_SIZE,
            sensorOrientation,
            MAINTAIN_ASPECT
        );

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(Canvas canvas) {
                        //renderDebug
                    }
                }
        );

    }

    protected synchronized void runInBackground(final Runnable r)
    {
        if (handler != null)
        {
            handler.post(r);
        }
    }

    public void addCallback(final OverlayView.DrawCallback callback)
    {
        final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
    }

    private int getScreenOrientation()
    {
        switch (getWindowManager().getDefaultDisplay().getRotation())
        {
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return 0;
        }
    }

    private void renderDebug(final Canvas canvas)
    {
        /*if (!isDebug())
        {
            return;
        }*/
    }

    private int getLayoutId()
    {
        return R.layout.camera_connection_fragment;
    }

    private Size getDesiredPreviewFrameSize()
    {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public synchronized void onStart()
    {
        Log.d(TAG, "onStart" + this);
        super.onStart();
    }

    @Override
    public synchronized void onResume()
    {
        Log.d(TAG, "onResume" + this);
        super.onResume();
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }


}

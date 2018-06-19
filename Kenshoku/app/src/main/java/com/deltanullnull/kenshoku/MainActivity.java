package com.deltanullnull.kenshoku;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.util.TypedValue;
import android.webkit.PermissionRequest;
import android.widget.Toast;

import java.util.logging.Logger;

public abstract class MainActivity extends AppCompatActivity implements ImageReader.OnImageAvailableListener {

    private static final int PERMISSION_REQUEST = 1;

    private boolean useCamera2API;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private int previewWidth = 0;
    private int previewHeight = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (hasPermission())
        {

        }
        else
        {
            requestPermission();
        }
    }

    @Override
    public void onImageAvailable(ImageReader reader) {

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (shouldShowRequestPermissionRationale(PERMISSION_STORAGE) || shouldShowRequestPermissionRationale(PERMISSION_CAMERA))
            {
                Toast.makeText(MainActivity.this, "Camera AND storage permissions are required", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSION_REQUEST);
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
        }

        return null;
    }

    private void setFragment()
    {
        String cameraId = chooseCamera();
        if (cameraId == null)
        {
            Toast.makeText(this, "No camera detected", Toast.LENGTH_LONG).show();
            finish();
        }

        Fragment fragment;
        if (useCamera2API)
        {
            CameraConnectionFragment camera2Fragment = CameraConnectionFragment.newInstance(
                    new CameraConnectionFragment.ConnectionCallback()
                    {
                        @Override
                        public void onPreviewSizeChosen(final Size size, final int rotation)
                        {
                            previewHeight = size.getHeight();
                            previewWidth = size.getWidth();
                            this.onPreviewSizeChosen(size, rotation);
                        }
                    },
                    this,
                    getLayoutId(),
                    getDesiredPreviewFrameSize()
            );
        }
    }

    private void onPreviewSizeChosen(final Size size, final int rotation)
    {
        final float textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()
        );

        // TODO define classifier

    }
    protected abstract int getLayoutId();
    protected abstract Size getDesiredPreviewFrameSize();
}

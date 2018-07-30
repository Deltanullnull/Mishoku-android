package com.deltanullnull.kenshoku;

import android.app.Fragment;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.List;

public class LegacyCameraConnectionFragment extends Fragment
{
    private static final String TAG = "LegacyCameraConnectionFragment";

    private Camera.PreviewCallback imageListener;
    private int layout;
    private Size desiredSize;
    private Camera camera;

    private AutoFitTextureView textureView;

    public LegacyCameraConnectionFragment(final Camera.PreviewCallback imageListener, final int layout, final Size desiredSize)
    {
        this.imageListener = imageListener;
        this.layout = layout;
        this.desiredSize = desiredSize;
    }

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static
    {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
                {
                    Log.d(TAG, "onSurfaceTextureAvailable " + this);
                    int index = getCameraId();
                    camera = Camera.open(index);

                    try
                    {
                        Camera.Parameters parameters = camera.getParameters();
                        List<String> focusModes = parameters.getSupportedFocusModes();
                        if (focusModes != null && focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                        {
                            parameters.setFocusMode((Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE));
                        }
                        List<Camera.Size> cameraSizes = parameters.getSupportedPreviewSizes();
                        Size[] sizes = new Size[cameraSizes.size()];
                        int i = 0;
                        for (Camera.Size size : cameraSizes)
                        {
                            sizes[i++] = new Size(size.width, size.height);
                        }

                        Size previewSize = CameraConnectionFragment.chooseOptimalSize(sizes, desiredSize.getWidth(), desiredSize.getHeight());
                        parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
                        camera.setDisplayOrientation(90);
                        camera.setParameters(parameters);
                        camera.setPreviewTexture(surface);

                    }
                    catch (IOException e)
                    {
                        camera.release();
                    }

                    camera.setPreviewCallbackWithBuffer(imageListener);
                    Camera.Size s = camera.getParameters().getPreviewSize();
                    camera.addCallbackBuffer(new byte[ImageUtils.getYUVByteSize(s.height, s.width)]);

                    textureView.setAspectRatio(s.height, s.width);

                    camera.startPreview();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
                {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
                {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface)
                {

                }
            };

    private HandlerThread backgroundThread;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState)
    {
        Log.d(TAG, "onCreateView " + this);
        return inflater.inflate(layout, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState)
    {
        Log.d(TAG, "onViewCreated " + this);
        textureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        //super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstance)
    {
        Log.d(TAG, "onActivityCreated " + this);
        super.onActivityCreated(savedInstance);
    }

    @Override
    public void onResume()
    {
        Log.d(TAG, "onResume " + this);
        super.onResume();
        startBackgroundThread();

        if (textureView.isAvailable())
        {
            camera.startPreview();
        }
        else
        {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "onPause " + this);
        stopCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void startBackgroundThread()
    {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
    }

    private void stopBackgroundThread()
    {
        backgroundThread.quitSafely();
        try
        {
            backgroundThread.join();
            backgroundThread = null;
        }
        catch (final InterruptedException e)
        {
            Log.d(TAG, e.getMessage());
        }
    }

    protected void stopCamera()
    {
        if (camera != null)
        {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            camera.release();
            camera = null;
        }
    }

    private int getCameraId()
    {
        Camera.CameraInfo ci = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++)
        {
            Camera.getCameraInfo(i, ci);
            if (ci.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
            {
                return i;
            }
        }

        return -1;
    }
}

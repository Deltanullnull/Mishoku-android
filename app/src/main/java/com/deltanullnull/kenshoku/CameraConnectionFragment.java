package com.deltanullnull.kenshoku;

import android.app.Fragment;
import android.media.ImageReader;
import android.util.Size;

public class CameraConnectionFragment extends Fragment
{
    private final ConnectionCallback cameraConnectionCallback;
    private final ImageReader.OnImageAvailableListener imageListener;
    private final Size inputSize;
    private final int layout;

    private String cameraId;

    private CameraConnectionFragment(
            final ConnectionCallback connectionCallback,
            final ImageReader.OnImageAvailableListener imageListener,
            final int layout,
            final Size inputSize)
    {
        this.cameraConnectionCallback = connectionCallback;
        this.imageListener = imageListener;
        this.layout = layout;
        this.inputSize = inputSize;
    }

    public void setCamera(String cameraId)
    {
        this.cameraId = cameraId;
    }

    public static CameraConnectionFragment newInstance(
            final ConnectionCallback callback,
            final ImageReader.OnImageAvailableListener imageListener,
            final int layout,
            final Size inputSize)
    {
        return new CameraConnectionFragment(callback, imageListener, layout, inputSize);
    }

    public interface ConnectionCallback
    {
        void onPreviewSizeChosen(Size size, int cameraRotation);
    }
}

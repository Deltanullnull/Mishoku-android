package com.deltanullnull.kenshoku;

import android.app.Fragment;
import android.media.ImageReader;
import android.util.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraConnectionFragment extends Fragment
{
    private final ConnectionCallback cameraConnectionCallback;
    private final ImageReader.OnImageAvailableListener imageListener;
    private final Size inputSize;
    private final int layout;

    private String cameraId;

    private static final int MINIMUM_PREVIEW_SIZE = 320;

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

    protected static Size chooseOptimalSize(final Size[] choices, final int width, final int height)
    {
        final int minSize = Math.max(Math.min(width, height), MINIMUM_PREVIEW_SIZE);
        final Size desiredSize = new Size(width, height);

        boolean exactSizeFound = false;
        final List<Size> bigEnough = new ArrayList<>();
        final List<Size> tooSmall = new ArrayList<>();

        for (final Size option : choices)
        {
            if (option.equals(desiredSize))
            {
                exactSizeFound = true;
            }

            if (option.getHeight() >= minSize && option.getWidth() >= minSize)
            {
                bigEnough.add(option);
            }
            else
            {
                tooSmall.add(option);
            }
        }

        if (exactSizeFound)
        {
            return desiredSize;
        }

        if (bigEnough.size() > 0)
        {
            final Size chosenSize = Collections.min(bigEnough, new CompareSizesByArea());
            return chosenSize;
        }
        else
        {
            return choices[0];
        }
    }

    static class CompareSizesByArea implements Comparator<Size>
    {

        @Override
        public int compare(Size o1, Size o2) {
            return Long.signum((long) o1.getWidth() * o1.getHeight() - (long) o2.getWidth() * o2.getHeight());
        }
    }
}

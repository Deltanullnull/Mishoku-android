package com.deltanullnull.kenshoku;

import android.app.Fragment;
import android.hardware.Camera;
import android.util.Size;

public class LegacyCameraConnectionFragment extends Fragment
{
    private Camera.PreviewCallback imageListener;
    private int layout;
    private Size desiredSize;

    public LegacyCameraConnectionFragment(final Camera.PreviewCallback imageListener, final int layout, final Size desiredSize)
    {
        this.imageListener = imageListener;
        this.layout = layout;
        this.desiredSize = desiredSize;
    }
}

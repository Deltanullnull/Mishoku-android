package com.deltanullnull.kenshoku;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class RecipeAdapter extends PagerAdapter
{

    private static final String TAG = "RecipeAdapter";
    private Context mContext;

    public RecipeAdapter(Context context)
    {
        mContext = context;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position)
    {
        Log.d(TAG, "Instantiate item at " + position);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        TestObject object = TestObject.values()[position];

        ViewGroup layout = (ViewGroup) inflater.inflate(object.getLayoutResId(), collection, false);
        collection.addView(layout);

        Button b = new Button(mContext);
        b.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        layout.addView(b);

        return layout;
    }

    @Override
    public int getCount() {
        return TestObject.values().length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }
}

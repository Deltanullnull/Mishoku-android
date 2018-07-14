package com.deltanullnull.kenshoku;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Trace;
import android.util.Log;

import org.tensorflow.Operation;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

public class ImageClassifier
{
    private static final String TAG = "ImageClassifier";

    private static final int MAX_RESULTS = 3;
    private static final float THRESHOLD = 0.3f;

    private String inputName;
    private String outputName;

    private int inputSize;
    private int imageMean;
    private float imageStd;

    private Vector<String> labels = new Vector<>();
    private int[] intValues;
    private float[] floatValues;
    private float[] outputs;
    private String[] outputNames;

    private TensorFlowInferenceInterface inferenceInterface;

    public static ImageClassifier create(
            AssetManager assetManager,
            String modelFileName,
            String labelFileName,
            int inputSize,
            int imageMean,
            float imageStd,
            String inputName,
            String outputName
    )
    {
        ImageClassifier imageClassifier = new ImageClassifier();
        imageClassifier.inputName = inputName;
        imageClassifier.outputName = outputName;

        String actualFileName = labelFileName.split("file:///android_asset/")[1];
        Log.d(TAG, "Reading labels from " + actualFileName);
        BufferedReader br = null;

        try
        {
            br = new BufferedReader(new InputStreamReader(assetManager.open(actualFileName)));
            String line;
            while ((line = br.readLine()) != null)
            {
                imageClassifier.labels.add(line);
            }
            br.close();
        }
        catch (IOException e)
        {
            Log.d(TAG, "Failed to open labelFile");
        }

        imageClassifier.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFileName);

        final Operation operation = imageClassifier.inferenceInterface.graphOperation(outputName);
        final int nClasses = (int) operation.output(0).shape().size(1);

        imageClassifier.inputSize = inputSize;
        imageClassifier.imageMean = imageMean;
        imageClassifier.imageStd = imageStd;

        imageClassifier.outputNames = new String[] {outputName};
        imageClassifier.intValues = new int[inputSize * inputSize];
        imageClassifier.floatValues = new float[inputSize * inputSize * 3];
        imageClassifier.outputs = new float[nClasses];

        return imageClassifier;
    }

    public List<Recognition> recognizeImage(final Bitmap bitmap)
    {
        Trace.beginSection("recognizeImage");

        Log.d(TAG, "recognizing image");

        if (intValues != null) {
            Log.d(TAG, "size: " + bitmap.getWidth() + ", " + bitmap.getHeight() );
            bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            for (int i = 0; i < intValues.length; i++) {
                final int value = intValues[i];

                floatValues[i * 3 + 0] = (((value >> 16) & 0xFF) );
                floatValues[i * 3 + 1] = (((value >> 8) & 0xFF) );
                floatValues[i * 3 + 2] = (((value >> 0) & 0xFF) );
            }

            inferenceInterface.feed(inputName, floatValues, 1, inputSize, inputSize, 3);

            inferenceInterface.run(outputNames, false);

            inferenceInterface.fetch(outputName, outputs);

            PriorityQueue<Recognition> pq = new PriorityQueue<>(
                    3,
                    new Comparator<Recognition>() {
                        @Override
                        public int compare(Recognition o1, Recognition o2) {
                            return Float.compare(o1.getConfidence(), o2.getConfidence());
                        }
                    }
            );

            for (int i = 0; i < outputs.length; i++) {
                if (outputs[i] > THRESHOLD) {
                    pq.add(
                            new Recognition("" + i, i <= labels.size() ? labels.get(i) : "unknown", outputs[i])
                    );
                }
            }

            int recognitionSize = Math.min(pq.size(), MAX_RESULTS);
            final ArrayList<Recognition> recognitions = new ArrayList<>();
            for (int i = 0; i < recognitionSize; i++) {
                recognitions.add(pq.poll());
            }

            Trace.endSection();

            if (recognitions.size() > 0)
                Log.d(TAG, "new recognitions: " + recognitions.get(0).getTitle());

            return recognitions;
        }

        return null;

    }
}

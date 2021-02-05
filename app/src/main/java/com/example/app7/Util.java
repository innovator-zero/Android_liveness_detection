package com.example.app7;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.List;

public class Util {
    //list取均值
    public static double avg(List<Double> list) {
        double ans = 0;
        for (int i = 0; i < list.size(); i++) {
            ans += list.get(i);
        }
        ans /= list.size();
        return ans;
    }
}

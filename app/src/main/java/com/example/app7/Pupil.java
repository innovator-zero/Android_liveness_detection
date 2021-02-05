package com.example.app7;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Pupil {
    private final int threshold;
    public int x;
    public int y;

    Pupil(Mat eye_frame, int thres) {
        threshold = thres;
        detect_iris(eye_frame);
    }

    public static Mat image_processing(Mat eye_frame, int thres) {
        Mat new_frame = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));

        Imgproc.bilateralFilter(eye_frame, new_frame, 10, 15, 15);
        Imgproc.erode(new_frame, new_frame, kernel, new Point(-1, -1), 3);
        Imgproc.threshold(new_frame, new_frame, thres, 255, Imgproc.THRESH_BINARY);

        return new_frame;
    }

    void detect_iris(Mat eye_frame) {
        Mat iris_frame = image_processing(eye_frame, threshold);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(iris_frame, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE);
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            public int compare(MatOfPoint arg0, MatOfPoint arg1) {
                double aera0 = Imgproc.contourArea(arg0);
                double aera1 = Imgproc.contourArea(arg1);
                if (aera0 > aera1) {
                    return 1;
                } else if (aera0 < aera1) {
                    return -1;
                }
                return 0;
            }
        });

        try {
            Moments moments = Imgproc.moments(contours.get(contours.size() - 2));
            x = (int) (moments.get_m10() / moments.get_m00());
            y = (int) (moments.get_m01() / moments.get_m00());
        } catch (Exception e) {
            x = -1;
            y = -1;
        }
    }
}

package com.example.app7;

import android.widget.ImageView;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Eye {
    private Mat s_frame;
    public Point origin, center;
    public Pupil pupil;
    public double blinking;

    Eye(Mat origin_frame, List<Point> landmarks, int side, Calibration calibration) {
        analyze(origin_frame, landmarks, side, calibration);
    }

    void analyze(Mat origin_frame, List<Point> landmarks, int side, Calibration calibration) {
        blinking = blinking_ratio(landmarks, side);
        isolate(origin_frame, landmarks, side);

        if (!calibration.is_complete()) {
            calibration.evaluate(s_frame, side);
        }

        int thres = calibration.threshold(side);
        pupil = new Pupil(s_frame, thres);
    }


    void isolate(Mat frame, List<Point> landmarks, int side) {
        List<Point> region = new ArrayList<>();

        if (side == 0) {
            for (int i = 36; i <= 41; i++) {
                region.add(landmarks.get(i));
            }
        } else {
            for (int i = 42; i <= 47; i++) {
                region.add(landmarks.get(i));
            }
        }

        Mat mask = Mat.zeros(frame.size(), CvType.CV_8UC1);
        mask.setTo(new Scalar(255));

        MatOfPoint Mat_region = new MatOfPoint();
        Mat_region.fromList(region);
        Imgproc.fillConvexPoly(mask, Mat_region, new Scalar(0, 0, 0));

        Mat eye = new Mat();
        Core.add(frame, mask, eye);

        int margin = 5;

        List<Integer> pointx = new ArrayList<>();
        List<Integer> pointy = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            pointx.add((int) region.get(i).x);
            pointy.add((int) region.get(i).y);
        }

        int min_x = Collections.min(pointx) - margin;
        int max_x = Collections.max(pointx) + margin;
        int min_y = Collections.min(pointy) - margin;
        int max_y = Collections.max(pointy) + margin;

        s_frame = new Mat(eye, new Range(min_y, max_y), new Range(min_x, max_x));
        origin = new Point(min_x, min_y);
        center = new Point((int) (s_frame.cols() / 2), (int) (s_frame.rows() / 2));
    }

    double dist(Point a, Point b) {
        return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
    }

    Point middle_point(Point a, Point b) {
        double x = (a.x + b.x) / 2;
        double y = (a.y + b.y) / 2;
        return new Point(x, y);
    }

    double blinking_ratio(List<Point> landmarks, int side) {
        Point left, right, top, bottom;

        if (side == 0) {
            left = landmarks.get(36);
            right = landmarks.get(39);
            top = middle_point(landmarks.get(37),landmarks.get(38));
            bottom = middle_point(landmarks.get(40),landmarks.get(41));
        } else {
            left = landmarks.get(42);
            right = landmarks.get(45);
            top = middle_point(landmarks.get(43),landmarks.get(44));
            bottom = middle_point(landmarks.get(46),landmarks.get(47));
        }

        double eye_width = dist(left, right);
        double eye_height = dist(top, bottom);
        double ratio;

        try {
            ratio = eye_width / eye_height;
        } catch (ArithmeticException ae) {
            ratio = -1;
        }

        return ratio;
    }
}

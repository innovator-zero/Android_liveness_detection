package com.example.app7;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Tracking {
    private static final String TAG = "Tracking";
    private Mat s_frame;
    private List<Point> landmarks;
    public Eye eye_left, eye_right;
    private Calibration calibration;

    public Tracking() {
        //s_frame = new Mat();
        //landmarks = new ArrayList<>();
        calibration = new Calibration();
    }

    public void refresh(Mat f, List<Point> lm) {
        s_frame = f;
        landmarks = lm;
        analyze();
    }

    void analyze() {
        Mat frame = new Mat();
        Imgproc.cvtColor(s_frame, frame, Imgproc.COLOR_BGR2GRAY);
        try {
            eye_left = new Eye(frame, landmarks, 0, calibration);
            eye_right = new Eye(frame, landmarks, 1, calibration);
        } catch (Exception e) {
            eye_left = null;
            eye_right = null;
        }
    }

    boolean pupils_located() {
        try {
            if (eye_left.pupil.x != -1 && eye_left.pupil.y != -1
                    && eye_right.pupil.x != -1 && eye_right.pupil.y != -1) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public Point pupil_left_coords() {
        if (pupils_located()) {
            double x = eye_left.origin.x + eye_left.pupil.x;
            double y = eye_left.origin.y + eye_left.pupil.y;
            return new Point(x, y);
        } else {
            return null;
        }
    }

    public Point pupil_right_coords() {
        try {
            if (pupils_located()) {
                double x = eye_right.origin.x + eye_right.pupil.x;
                double y = eye_right.origin.y + eye_right.pupil.y;
                return new Point(x, y);
            } else {
                return null;
            }
        }catch (Exception e){
            return null;
        }
    }

    public double horizontal_ratio() {
        try {
            if (pupils_located()) {
                double pupil_left = eye_left.pupil.x / (eye_left.center.x * 2 - 10);
                double pupil_right = eye_right.pupil.x / (eye_right.center.x * 2 - 10);
                return (pupil_left + pupil_right) / 2;
            } else {
                return -1;
            }
        } catch (Exception e) {
            Log.d(TAG, "horizontal_ratio: " + e);
            return -1;
        }
    }

    double vertical_ratio() {
        try {
            if (pupils_located()) {
                double pupil_left = eye_left.pupil.y / (eye_left.center.y * 2 - 10);
                double pupil_right = eye_right.pupil.y / (eye_right.center.y * 2 - 10);
                return (pupil_left + pupil_right) / 2;
            } else {
                return -1;
            }
        } catch (Exception e) {
            Log.d(TAG, "vertical_ratio: " + e);
            return -1;
        }
    }

    public boolean is_right() {
        if (pupils_located()) {
            return horizontal_ratio() <= 0.35;
        } else {
            return false;
        }
    }

    public boolean is_left() {
        if (pupils_located()) {
            return horizontal_ratio() >= 0.75;
        } else {
            return false;
        }
    }

    public boolean left_blinking() {
        return (eye_left.blinking > 3.8);
    }

    public boolean right_blinking() {
        return (eye_right.blinking > 3.8);
    }
}

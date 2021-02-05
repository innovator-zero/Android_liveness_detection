package com.example.app7;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Calibration {
    List<Integer> thresholds_left;
    List<Integer> threshold_right;

    Calibration() {
        thresholds_left = new ArrayList<>();
        threshold_right = new ArrayList<>();
    }

    boolean is_complete() {
        int nb_frames = 20;
        return thresholds_left.size() >= nb_frames && threshold_right.size() >= nb_frames;
    }

    int sum(List<Integer> list) {
        int ans = 0;
        for (int i = 0; i < list.size(); i++) {
            ans += list.get(i);
        }
        return ans;
    }

    int threshold(int side) {
        if (side == 0) {
            return (int) (sum(thresholds_left) / thresholds_left.size());
        } else {
            return (int) (sum(threshold_right) / threshold_right.size());
        }
    }

    double iris_size(Mat frame) {
        Mat new_frame = new Mat(frame, new Range(5, frame.rows() - 5), new Range(5, frame.cols() - 5));
        int height = new_frame.rows();
        int width = new_frame.cols();
        int nb_pixels = height * width;
        int nb_blacks = nb_pixels - Core.countNonZero(new_frame);
        return (double) (nb_blacks) / (double) (nb_pixels);
    }

    int find_best_threshold(Mat eye_frame) {
        double average_iris_size = 0.48;
        double[] trials = new double[19];

        for (int i = 0; i < 19; i++) {
            Mat iris_frame = Pupil.image_processing(eye_frame, 5 * (i + 1));
            trials[i] = iris_size(iris_frame);
        }

        //找最接近0.48的阈值
        int best_threshold = 0;
        double diff = Math.abs(trials[0] - average_iris_size);

        for (int i = 1; i < 19; i++) {
            double new_diff = Math.abs(trials[i] - average_iris_size);
            if (new_diff < diff) {
                diff = new_diff;
                best_threshold = i;
            }
        }

        return (best_threshold + 1) * 5;
    }

    void evaluate(Mat eye_frame, int side) {
        int threshold = find_best_threshold(eye_frame);

        if (side == 0) {
            thresholds_left.add(threshold);
        } else {
            threshold_right.add(threshold);
        }
    }
}

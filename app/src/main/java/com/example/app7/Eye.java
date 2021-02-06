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
    private List<Point> region;

    Eye(Mat origin_frame, List<Point> landmarks, int side, Calibration calibration) {
        region = new ArrayList<>();
        if (side == 0) {
            for (int i = 36; i <= 41; i++) {
                region.add(landmarks.get(i));
            }
        } else {
            for (int i = 42; i <= 47; i++) {
                region.add(landmarks.get(i));
            }
        }

        isolate(origin_frame);

        if (!calibration.is_complete()) {
            calibration.evaluate(s_frame, side);
        }

        int thres = calibration.threshold(side);
        pupil = new Pupil(s_frame, thres);
    }


    void isolate(Mat frame) {
        //全白的图片
        Mat mask = Mat.zeros(frame.size(), CvType.CV_8UC1);
        mask.setTo(new Scalar(255));

        //眼睛的部分填充成黑的
        MatOfPoint Mat_region = new MatOfPoint();
        Mat_region.fromList(region);
        Imgproc.fillConvexPoly(mask, Mat_region, new Scalar(0, 0, 0));

        //原图中抠出眼睛，其余为白色
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

        //眼睛的矩形裁剪出来
        s_frame = new Mat(eye, new Range(min_y, max_y), new Range(min_x, max_x));
        //矩形的原点（绝对）
        origin = new Point(min_x, min_y);
        //矩形的中心（相对）
        center = new Point((int) (s_frame.cols() / 2), (int) (s_frame.rows() / 2));
    }

    double dist(Point a, Point b) {
        return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
    }

    public double EAR() {
        double ear;
        try {
            ear = (dist(region.get(1), region.get(5)) + dist(region.get(2), region.get(4)))
                    / (2 * dist(region.get(0), region.get(3)));
        } catch (Exception e) {
            ear = -1;
        }
        return ear;
    }
}

package com.example.app7;

public class TrackingResult {
    public boolean face;
    public double horizontal_ratio;
    public double vertical_ratio;
    public boolean blinking;

    TrackingResult(boolean f, double h_r, double v_r, boolean blink) {
        face = f;
        horizontal_ratio = h_r;
        vertical_ratio = v_r;
        blinking = blink;
    }

    TrackingResult(boolean f) {
        face = false;//没有识别到人脸
        horizontal_ratio = -1;
        vertical_ratio = -1;
        blinking = false;
    }
}

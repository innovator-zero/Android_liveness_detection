package com.example.app7;

public class TrackingResult {
    public boolean face;
    public double horizontal_ratio;
    public double vertical_ratio;
    public boolean blinking;

    TrackingResult(boolean f, double h_r, double v_r, boolean left_blinking, boolean right_blinking) {
        face = f;
        horizontal_ratio = h_r;
        vertical_ratio = v_r;
        blinking = left_blinking || right_blinking;
    }

    TrackingResult(boolean f) {
        face = false;
        horizontal_ratio = -1;
        vertical_ratio = -1;
        blinking = false;
    }
}

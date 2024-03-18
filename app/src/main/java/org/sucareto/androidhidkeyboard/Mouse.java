package org.sucareto.androidhidkeyboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import org.sucareto.androidhidkeyboard.databinding.ActivityMouseBinding;

public class Mouse extends AppCompatActivity {
    HidController hid = new HidController();
    private int moveSpeed = 3;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View root_view = ActivityMouseBinding.inflate(getLayoutInflater()).getRoot();
        setContentView(root_view);

        findViewById(R.id.BtnBack).setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
            startActivity(new Intent(Mouse.this, Keyboard.class));
        });

        KeyOnTouch keyOnTouch = new KeyOnTouch();
        String packageName = getPackageName();
        for (int i = 1; i < 7; i++) {
            int resId = getResources().getIdentifier("mBtn" + i, "id", packageName);
            if (resId != 0) {
                findViewById(resId).setOnTouchListener(keyOnTouch);
            }
        }

        MouseKeyOnTouch mouseKeyOnTouch = new MouseKeyOnTouch();
        findViewById(R.id.mouse_left).setOnTouchListener(mouseKeyOnTouch);
        findViewById(R.id.mouse_middle).setOnTouchListener(mouseKeyOnTouch);
        findViewById(R.id.mouse_right).setOnTouchListener(mouseKeyOnTouch);

        View touchView = findViewById(R.id.touch_area);
        GestureDetectorCompat touchAreaDetector = new GestureDetectorCompat(this, new TouchAreaListener());
        touchView.setLongClickable(true);
        touchView.setOnTouchListener((v, event) -> touchAreaDetector.onTouchEvent(event));

        View scrollView = findViewById(R.id.scroll_area);
        GestureDetectorCompat scrollAreaDetector = new GestureDetectorCompat(this, new ScrollAreaListener());
        scrollView.setLongClickable(true);
        scrollView.setOnTouchListener((v, event) -> scrollAreaDetector.onTouchEvent(event));

        ((SeekBar) findViewById(R.id.sbMouseMoveSpeed)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                moveSpeed = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        hid.UnInit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hid.kInit() | hid.mInit()) {
            Toast.makeText(this, R.string.msg_e_hid, Toast.LENGTH_LONG).show();
        }
    }

    private class KeyOnTouch implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
                    hid.kPress((byte) Integer.parseInt(v.getTag().toString(), 16));
                }
                case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    hid.kRelease((byte) Integer.parseInt(v.getTag().toString(), 16));
                }
            }
            return false;
        }
    }

    private class MouseKeyOnTouch implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
                    hid.mPress((byte) Integer.parseInt(v.getTag().toString(), 16));
                }
                case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    hid.mRelease((byte) Integer.parseInt(v.getTag().toString(), 16));
                }
            }
            return false;
        }
    }

    class TouchAreaListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
            hid.mPress((byte) 1);
            hid.mRelease((byte) 1);
            return true;
        }

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            hid.mPress((byte) 1);
            hid.mRelease((byte) 1);
            hid.mPress((byte) 1);
            hid.mRelease((byte) 1);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            int moveX = (int) (-distanceX * moveSpeed);
            int moveY = (int) (-distanceY * moveSpeed);
            while (moveX != 0 || moveY != 0) {
                if (Math.abs(moveX) > 127) {
                    hid.mCode[1] = (byte) (moveX > 0 ? 127 : -127);
                    moveX -= hid.mCode[1];
                } else {
                    hid.mCode[1] = (byte) moveX;
                    moveX = 0;
                }
                if (Math.abs(moveY) > 127) {
                    hid.mCode[2] = (byte) (moveY > 0 ? 127 : -127);
                    moveY -= hid.mCode[2];
                } else {
                    hid.mCode[2] = (byte) moveY;
                    moveY = 0;
                }
                hid.mSend();
            }
            return true;
        }

        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            hid.mPress((byte) 2);
            hid.mRelease((byte) 2);
        }
    }

    class ScrollAreaListener extends GestureDetector.SimpleOnGestureListener {
        private float ScrollData = 0;

        @Override
        public boolean onScroll(MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            ScrollData += distanceY;
            if (Math.abs(ScrollData) > 20) {
                hid.mCode[3] = (byte) (ScrollData < 0 ? 1 : 255);
                ScrollData = 0;
                hid.mSend();
            }
            return true;
        }
    }
}
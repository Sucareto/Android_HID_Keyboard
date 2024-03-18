package org.sucareto.androidhidkeyboard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.sucareto.androidhidkeyboard.databinding.ActivityKeyboardBinding;


public class Keyboard extends AppCompatActivity {
    HidController hid = new HidController();
    private boolean FnEnable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View root_view = ActivityKeyboardBinding.inflate(getLayoutInflater()).getRoot();
        setContentView(root_view);

        findViewById(R.id.BtnSpace).setOnTouchListener(new SpaceOnTouch());
        findViewById(R.id.BtnEnter).setOnTouchListener(new EnterOnTouch());
        findViewById(R.id.BtnFn).setOnTouchListener(new FnKeyOnTouch());

        String packageName = getPackageName();

        KeyOnTouch keyOnTouch = new KeyOnTouch();
        for (int i = 1; i < 72; i++) {
            int resId = getResources().getIdentifier("Btn" + i, "id", packageName);
            if (resId != 0) {
                findViewById(resId).setOnTouchListener(keyOnTouch);
            }
        }
        CtrlKeyOnTouch ctrlKeyOnTouch = new CtrlKeyOnTouch();
        for (int i = 1; i < 6; i++) {
            int resId = getResources().getIdentifier("CtrlBtn" + i, "id", packageName);
            if (resId != 0) {
                findViewById(resId).setOnTouchListener(ctrlKeyOnTouch);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        hid.UnInit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hid.kInit()) {
            Toast.makeText(this, R.string.msg_e_hid, Toast.LENGTH_LONG).show();
        }
    }

    private class SpaceOnTouch implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
                    if (FnEnable) {
                        startActivity(new Intent(Keyboard.this, Mouse.class));
                        break;
                    }
                    hid.kPress((byte) Integer.parseInt(v.getTag().toString(), 16));
                }
                case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    hid.kRelease((byte) Integer.parseInt(v.getTag().toString(), 16));
                }
            }
            return false;
        }
    }

    private class EnterOnTouch implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
                    if (FnEnable) {
                        hid.kCode = new byte[]{0x05, 0, 0x4c, 0, 0, 0, 0, 0};
                        hid.kSend();
                    } else {
                        hid.kPress((byte) Integer.parseInt(v.getTag().toString(), 16));
                    }
                }
                case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (FnEnable) {
                        hid.kCode = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
                        hid.kSend();
                    } else {
                        hid.kRelease((byte) Integer.parseInt(v.getTag().toString(), 16));
                    }
                }
            }
            return false;
        }
    }

    private class CtrlKeyOnTouch implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
                    hid.kPress_c((byte) Integer.parseInt(v.getTag().toString(), 16));
                }
                case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    hid.kRelease_c((byte) Integer.parseInt(v.getTag().toString(), 16));
                }
            }
            return false;
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

    private class FnKeyOnTouch implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
                    FnEnable = true;
                }
                case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> FnEnable = false;
                default -> {
                    return false;
                }
            }
            ((Button) findViewById(R.id.Btn12)).setText(getResources().getString(FnEnable ? R.string.KeyText78 : R.string.KeyText12));//Insert
            findViewById(R.id.Btn12).setTag(getResources().getString(FnEnable ? R.string.KeyCode78 : R.string.KeyCode12));

            ((Button) findViewById(R.id.Btn13)).setText(getResources().getString(FnEnable ? R.string.KeyText81 : R.string.KeyText13));//Backspace,Delete
            findViewById(R.id.Btn13).setTag(getResources().getString(FnEnable ? R.string.KeyCode81 : R.string.KeyCode13));

            ((Button) findViewById(R.id.Btn65)).setText(getResources().getString(FnEnable ? R.string.KeyText80 : R.string.KeyText84));//Up,PgUp
            findViewById(R.id.Btn65).setTag(getResources().getString(FnEnable ? R.string.KeyCode80 : R.string.KeyCode84));

            ((Button) findViewById(R.id.Btn69)).setText(getResources().getString(FnEnable ? R.string.KeyText79 : R.string.KeyText85));//Left,Home
            findViewById(R.id.Btn69).setTag(getResources().getString(FnEnable ? R.string.KeyCode79 : R.string.KeyCode85));

            ((Button) findViewById(R.id.Btn70)).setText(getResources().getString(FnEnable ? R.string.KeyText83 : R.string.KeyText86));//Down,PgDn
            findViewById(R.id.Btn70).setTag(getResources().getString(FnEnable ? R.string.KeyCode83 : R.string.KeyCode86));

            ((Button) findViewById(R.id.Btn71)).setText(getResources().getString(FnEnable ? R.string.KeyText82 : R.string.KeyText87));//Right,End
            findViewById(R.id.Btn71).setTag(getResources().getString(FnEnable ? R.string.KeyCode82 : R.string.KeyCode87));

            ((Button) findViewById(R.id.BtnSpace)).setText(getResources().getString(FnEnable ? R.string.FnSpace : R.string.KeyText70));//Space
            ((Button) findViewById(R.id.BtnEnter)).setText(getResources().getString(FnEnable ? R.string.FnEnter : R.string.KeyText54));//Enter
            return false;
        }
    }
}
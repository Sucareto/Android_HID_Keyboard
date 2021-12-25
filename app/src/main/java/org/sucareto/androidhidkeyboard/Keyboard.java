package org.sucareto.androidhidkeyboard;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Service;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;

import org.sucareto.androidhidkeyboard.databinding.ActivityKeyboardBinding;


public class Keyboard extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityKeyboardBinding binding = ActivityKeyboardBinding.inflate(getLayoutInflater());
        View mControlsView = binding.getRoot();
        mControlsView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        setContentView(binding.getRoot());
        for (int i = 1; i < 67; i++) {
            findViewById(getResources().getIdentifier("key" + i, "id",
                    "org.sucareto.androidhidkeyboard")).setOnTouchListener(new KeyOnTouch());
        }
        for (int i = 1; i < 9; i++) {
            findViewById(getResources().getIdentifier("ckey" + i, "id",
                    "org.sucareto.androidhidkeyboard")).setOnTouchListener(new CtrlKeyOnTouch());
        }

    }

    String ctl_code = "\\x00";
    String key_code = "\\x00";

    class CtrlKeyOnTouch implements View.OnTouchListener {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            //TODO 按钮锁
            //TODO Shift键按下时，修改按键文字
            String cmd = "su -c echo -n '";
            int code;
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ((Vibrator) getSystemService(Service.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                    //ctlcode="\\xFF"，取FF，转16进制int，运算后转16进制String，根据大小补充\\x0或\\x
                    code = Integer.parseInt(ctl_code.substring(2, 4), 16) + Integer.parseInt(((String) view.getTag()).substring(2, 4), 16);
                    ctl_code = code < 16 ? "\\x0" + Integer.toHexString(code) : "\\x" + Integer.toHexString(code);
                    Log.d("OnClick", "按下控制键：" + ctl_code);
                    view.performClick();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    code = Integer.parseInt(ctl_code.substring(2, 4), 16) - Integer.parseInt(((String) view.getTag()).substring(2, 4), 16);
                    ctl_code = code < 16 ? "\\x0" + Integer.toHexString(code) : "\\x" + Integer.toHexString(code);
                    Log.d("OnClick", "松开控制键：" + ctl_code);
                    break;
                default:
                    return false;
            }
            cmd += (ctl_code + key_code + "\\x00\\x00\\x00\\x00\\x00\\x00").substring(0, "\\x00".length() * 8) + "' > /dev/hidg0";
            try {
                Log.d("OnClick", cmd);
                Runtime.getRuntime().exec(cmd);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    class KeyOnTouch implements View.OnTouchListener {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            String cmd = "su -c echo -n '";
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ((Vibrator) getSystemService(Service.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                    Log.d("OnClick", "按下" + view.getTag());
                    key_code += view.getTag();
                    view.performClick();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    Log.d("OnClick", "松开" + view.getTag());
                    key_code = key_code.replace((String) view.getTag(), "");
                    break;
                default:
                    return false;
            }
            cmd += (ctl_code + key_code + "\\x00\\x00\\x00\\x00\\x00\\x00").substring(0, "\\x00".length() * 8) + "' > /dev/hidg0";
            try {
                Log.d("OnClick", cmd);
                Runtime.getRuntime().exec(cmd);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }


    @Override
    protected void onStop() {
        try {
            Runtime.getRuntime().exec("su -c echo -n '\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00' > /dev/hidg0");
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onStop();
    }
}
package org.sucareto.androidhidkeyboard;

import android.app.Service;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.io.SuFile;
import com.topjohnwu.superuser.io.SuFileOutputStream;

import org.sucareto.androidhidkeyboard.databinding.ActivityKeyboardBinding;

import java.io.OutputStream;


public class Keyboard extends AppCompatActivity {
    byte[] keycode;
    OutputStream HidStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityKeyboardBinding binding = ActivityKeyboardBinding.inflate(getLayoutInflater());
        binding.getRoot().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        setContentView(binding.getRoot());
        if (!Shell.getShell().isRoot()) {
            Toast.makeText(this, R.string.msg_e_root, Toast.LENGTH_LONG).show();
            return;
        }
        if (!SuFile.open("/config/usb_gadget/keyboard/").exists() && !SuFile.open("/dev/hidg0").exists()) {
            Toast.makeText(this, R.string.msg_e_hid, Toast.LENGTH_LONG).show();
            return;
        }

        //TODO 使用其他初始化方式，解决id变动的问题
        findViewById(R.id.fn).setOnTouchListener(new FnKeyOnTouch());
        KeyOnTouch keyOnTouch = new KeyOnTouch();
        for (int i = 1; i < 72; i++) {
            try {
                findViewById(getResources().getIdentifier("Btn" + i, "id",
                        getPackageName())).setOnTouchListener(keyOnTouch);
            } catch (Exception e) {
                Log.e("setKeyOnTouch", "在" + i + "的时候：" + e);
            }

        }
        CtrlKeyOnTouch ctrlKeyOnTouch = new CtrlKeyOnTouch();
        for (int i = 1; i < 6; i++) {
            try {
                findViewById(getResources().getIdentifier("CtrlBtn" + i, "id",
                        getPackageName())).setOnTouchListener(ctrlKeyOnTouch);
            } catch (Exception e) {
                Log.e("setCtrlKeyOnTouch", "在" + i + "的时候：" + e);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            HidStream.write(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
            HidStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void onStart() {
        super.onStart();
        keycode = new byte[8];
        try {
            HidStream = SuFileOutputStream.open(SuFile.open("/dev/hidg0"));
        } catch (Exception e) {
            Toast.makeText(this, "hid设备文件打开失败", Toast.LENGTH_LONG).show();
            Log.e("onStart", String.valueOf(e));
        }
    }

    class CtrlKeyOnTouch implements View.OnTouchListener {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ((Vibrator) getSystemService(Service.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                    keycode[0] += Integer.parseInt(view.getTag().toString(), 16);
                    Log.e("CtrlKeyOnTouch", "按下控制键：" + keycode[0]);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    keycode[0] -= Integer.parseInt(view.getTag().toString(), 16);
                    view.performClick();
                    Log.e("CtrlKeyOnTouch", "松开控制键：" + keycode[0]);
                    break;
                default:
                    return false;
            }
            try {
                HidStream.write(keycode);
            } catch (Exception e) {
                Log.e("CtrlKeyOnTouch", String.valueOf(e));
            }
            return false;
        }
    }

    class KeyOnTouch implements View.OnTouchListener {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ((Vibrator) getSystemService(Service.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                    for (int i = 2; i < 8; i++) {
                        if (keycode[i] == 0) {
                            keycode[i] = (byte) Integer.parseInt(view.getTag().toString(), 16);
                            break;
                        }
                    }
                    Log.e("KeyOnTouch", "按下" + view.getTag());
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    int code = Integer.parseInt(view.getTag().toString(), 16);
                    for (int i = 2; i < 8; i++) {
                        if (keycode[i] == code) {
                            keycode[i] = 0;
                        }
                    }
                    view.performClick();
                    Log.e("KeyOnTouch", "松开" + view.getTag());
                    break;
                default:
                    return false;
            }
            try {
                HidStream.write(keycode);
            } catch (Exception e) {
                Log.e("CtrlKeyOnTouch", String.valueOf(e));
            }
            return false;
        }
    }

    class FnKeyOnTouch implements View.OnTouchListener {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            boolean FnEnable;
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ((Vibrator) getSystemService(Service.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                    FnEnable = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    FnEnable = false;
                    view.performClick();
                    break;
                default:
                    return false;
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
            return false;
        }
    }
}
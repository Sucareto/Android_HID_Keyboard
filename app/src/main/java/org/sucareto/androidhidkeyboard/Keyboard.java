package org.sucareto.androidhidkeyboard;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
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
    private byte[] keycode = null;
    private OutputStream HidStream = null;
    private boolean FnEnable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View root_view = ActivityKeyboardBinding.inflate(getLayoutInflater()).getRoot();
        root_view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        setContentView(root_view);
        if (!Shell.getShell().isRoot()) {
            Toast.makeText(this, R.string.msg_e_root, Toast.LENGTH_LONG).show();
            return;
        }
        if (!SuFile.open("/dev/hidg0").exists()) {
            Toast.makeText(this, R.string.msg_e_hid, Toast.LENGTH_LONG).show();
            return;
        }

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

    void SendKeyCode(SendKeyMode mode, byte code) {
        switch (mode) {
            case Ctrl_Add:
                keycode[0] += code;
                break;
            case Ctrl_Del:
                keycode[0] -= code;
                break;
            case Key_Add:
                for (byte i = 2; i < 8; i++) {
                    if (keycode[i] == 0) {
                        keycode[i] = code;
                        break;
                    }
                }
                break;
            case Key_Del:
                for (byte i = 2; i < 8; i++) {
                    if (keycode[i] == code) {
                        keycode[i] = 0;
                        break;
                    }
                }
                break;
            case Send_Only:
                break;
            default:
                return;
        }
        try {
            HidStream.write(keycode);
        } catch (Exception e) {
            Log.e("SendKeyCode", String.valueOf(e));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            HidStream.write(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
            HidStream.close();
        } catch (Exception e) {
            Log.e("onStop", String.valueOf(e));
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        keycode = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
        try {
            HidStream = SuFileOutputStream.open(SuFile.open("/dev/hidg0"));
            HidStream.write(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
        } catch (Exception e) {
            Toast.makeText(this, "hid设备文件打开失败", Toast.LENGTH_LONG).show();
            Log.e("onStart", String.valueOf(e));
        }
    }


    enum SendKeyMode {
        Ctrl_Add,
        Ctrl_Del,
        Key_Add,
        Key_Del,
        Send_Only,
    }

    private class SpaceOnTouch implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
                    if (FnEnable) {
//                        startActivity(new Intent(Keyboard.this, Mouse.class));
                        break;
                    }
                    SendKeyCode(SendKeyMode.Key_Add, (byte) Integer.parseInt(v.getTag().toString(), 16));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    SendKeyCode(SendKeyMode.Key_Del, (byte) Integer.parseInt(v.getTag().toString(), 16));
                    break;
            }
            return false;
        }
    }

    private class EnterOnTouch implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
                    if (FnEnable) {
                        keycode = new byte[]{0x05, 0, 0x4c, 0, 0, 0, 0, 0};
                        SendKeyCode(SendKeyMode.Send_Only, (byte) 0);
                    } else {
                        SendKeyCode(SendKeyMode.Key_Add, (byte) Integer.parseInt(v.getTag().toString(), 16));
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (FnEnable) {
                        keycode = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
                        SendKeyCode(SendKeyMode.Send_Only, (byte) 0);
                    } else {
                        SendKeyCode(SendKeyMode.Key_Del, (byte) Integer.parseInt(v.getTag().toString(), 16));
                    }
                    break;
            }
            return false;
        }
    }

    private class CtrlKeyOnTouch implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
                    SendKeyCode(SendKeyMode.Ctrl_Add, (byte) Integer.parseInt(v.getTag().toString(), 16));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    SendKeyCode(SendKeyMode.Ctrl_Del, (byte) Integer.parseInt(v.getTag().toString(), 16));
                    break;
            }
            return false;
        }
    }

    private class KeyOnTouch implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
                    SendKeyCode(SendKeyMode.Key_Add, (byte) Integer.parseInt(v.getTag().toString(), 16));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    SendKeyCode(SendKeyMode.Key_Del, (byte) Integer.parseInt(v.getTag().toString(), 16));
                    break;
            }
            return false;
        }
    }

    private class FnKeyOnTouch implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_PRESS);
                    FnEnable = true;
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    FnEnable = false;
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

            ((Button) findViewById(R.id.BtnSpace)).setText(getResources().getString(FnEnable ? R.string.FnSpace : R.string.KeyText70));//Space
            ((Button) findViewById(R.id.BtnEnter)).setText(getResources().getString(FnEnable ? R.string.FnEnter : R.string.KeyText54));//Enter
            return false;
        }
    }
}
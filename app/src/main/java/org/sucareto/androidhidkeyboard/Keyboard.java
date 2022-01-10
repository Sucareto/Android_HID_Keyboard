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

import org.sucareto.androidhidkeyboard.databinding.ActivityKeyboardBinding;


public class Keyboard extends AppCompatActivity {
    static {
        Shell.enableVerboseLogging = BuildConfig.DEBUG;//启用运行命令Debug输出
        Shell.setDefaultBuilder(Shell.Builder.create()//覆盖默认的创建函数
                .setFlags(Shell.FLAG_REDIRECT_STDERR)//错误输出重定向到标准输出
                .setTimeout(10)//设置等待超时，默认是20s
        );
    }

    String ctl_code = "\\x00";
    String key_code = "\\x00";

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
        if (!SuFile.open("/config/usb_gadget/keyboard/").exists()) {
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
        Shell.su("echo -n '\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00' > /dev/hidg0").submit();
        super.onStop();
    }

    class CtrlKeyOnTouch implements View.OnTouchListener {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            //TODO 按钮锁
            String cmd = "echo -n '";
            int code;
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ((Vibrator) getSystemService(Service.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                    //ctlcode="\\xFF"，取FF，转16进制int，运算后转16进制String，根据大小补充\\x0或\\x
                    code = Integer.parseInt(ctl_code.substring(2, 4), 16) + Integer.parseInt(((String) view.getTag()).substring(2, 4), 16);
                    ctl_code = code < 16 ? "\\x0" + Integer.toHexString(code) : "\\x" + Integer.toHexString(code);
                    Log.e("CtrlKeyOnTouch", "按下控制键：" + ctl_code);
                    view.performClick();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    code = Integer.parseInt(ctl_code.substring(2, 4), 16) - Integer.parseInt(((String) view.getTag()).substring(2, 4), 16);
                    ctl_code = code < 16 ? "\\x0" + Integer.toHexString(code) : "\\x" + Integer.toHexString(code);
                    Log.e("CtrlKeyOnTouch", "松开控制键：" + ctl_code);
                    break;
                default:
                    return false;
            }
            cmd += (ctl_code + key_code + "\\x00\\x00\\x00\\x00\\x00\\x00").substring(0, "\\x00".length() * 8) + "' > /dev/hidg0";
            Shell.su(cmd).submit();
            return false;
        }
    }

    class KeyOnTouch implements View.OnTouchListener {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            String cmd = "echo -n '";
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ((Vibrator) getSystemService(Service.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                    Log.e("KeyOnTouch", "按下" + view.getTag());
                    key_code += view.getTag();
                    view.performClick();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    Log.e("KeyOnTouch", "松开" + view.getTag());
                    key_code = key_code.replace((String) view.getTag(), "");
                    break;
                default:
                    return false;
            }
            cmd += (ctl_code + key_code + "\\x00\\x00\\x00\\x00\\x00\\x00").substring(0, "\\x00".length() * 8) + "' > /dev/hidg0";
            Shell.su(cmd).submit();
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
                    view.performClick();
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
            return false;
        }
    }
}
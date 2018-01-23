package com.hcpda.iso14443a;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.rscja.deviceapi.Module;

import utils.RFID_14443A;
import utils.SoundUtil;

/**
 * RFID14443A使用demo
 * 1、使用前请确认您的机器已安装此模块。
 * 2、要正常使用模块需要在\libs\armeabi\目录放置libDeviceAPI.so文件，同时在\libs\目录下放置DeviceAPIver20160627.jar文件。
 * 3、在操作设备前需要调用 init(2,115200) 打开设备，使用完后调用 free() 关闭设备电源
 *
 * M1-S70卡：4K字节, 共40个扇区，前32个扇区中，每个扇区4个数据块，后8个扇区中，每个扇区16个数据块，每个数据块16个字节
 * S50卡： M1卡分为16个扇区，每个扇区由4块（块0、块1、块2、块3）组成，（我们也将16个扇区的64个块按绝对地址编号为0~63）每个扇区的第三块都是密码块，密码块里分了A跟B秘钥、存取控制。
 *
 */
public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {
    Module module;
    SoundUtil soundUtil;
    RadioGroup radioGroup;
    M1Fragment m1Fragment;
    ScanFragment scanFragment;
    RadioButton rb_scan;
    RadioButton rb_m1;
    RFID_14443A rfid_14443A;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

//        try {
//            module = Module.getInstance();
//        } catch (ConfigurationException e) {
//            e.printStackTrace();
//        }
         rfid_14443A= RFID_14443A.getInstance();
        soundUtil = new SoundUtil(this);
        boolean init_result = rfid_14443A.init( );
        if (init_result) {

            Toast.makeText(getApplicationContext(), "初始化成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "初始化失败", Toast.LENGTH_SHORT).show();
        }
        initView();
        setDefaultFragment();
    }


    private void setDefaultFragment() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        scanFragment = new ScanFragment();
        transaction.replace(R.id.fragment_main, scanFragment);
        transaction.commit();
    }


    private void initView() {
        rb_scan = findViewById(R.id.rb_scan_fragment);
        rb_m1 = findViewById(R.id.rb_m1_fragment);
        radioGroup = findViewById(R.id.rg);
        radioGroup.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        rfid_14443A.power_on();
        showDialog();
    }

    public void showDialog() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setMessage("初始化..");
        dialog.show();
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);//让他显示1秒后，取消ProgressDialog
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                dialog.dismiss();
            }
        });
        t.start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rfid_14443A.free();
    }

    @Override
    protected void onPause() {
        super.onPause();
        rfid_14443A.power_off();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        switch (checkedId) {
            case R.id.rb_scan_fragment:
                if (scanFragment == null) {
                    scanFragment = new ScanFragment();
                }
                transaction.replace(R.id.fragment_main, scanFragment);
                break;
            case R.id.rb_m1_fragment:
                if (m1Fragment == null) {
                    m1Fragment = new M1Fragment();
                }
                transaction.replace(R.id.fragment_main, m1Fragment);
                break;
        }
        transaction.commit();
    }
}

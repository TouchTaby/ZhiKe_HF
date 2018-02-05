package com.hcpda.iso15693;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.hpda.deviceapi.RFIDWithISO15693;
import com.hpda.deviceapi.exception.ConfigurationException;
import com.hpda.deviceapi.exception.RFIDNotFoundException;
import com.hpda.utility.StringUtility;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    RFIDWithISO15693 mRFID;
    boolean bResult = false;

    private Spinner spBlock;
    private Button btnRead;
    private Button btnReadId;
    private Button btnWrite;
    private TextView tvResult;
    private EditText etWriteData;
    private EditText etAFI;
    private EditText etDSFID;
    private Button btnWriteAFI;
    private Button btnWriteDSFID;
    private Button btnLockAFI;
    private Button btnLockDSFID;
    private ArrayAdapter adapterBlock;
    private ArrayList<String> arrBlock = new ArrayList<String>();

    private Button btnContinuous;
    private LinearLayout llSingle;
    private ScrollView svResult;

    // 连续操作变量START
    private LinearLayout llMultiple;

    private int readSuccCount = 0;
    private int readFailCount = 0;
    private int writeSuccCount = 0;
    private int writeFailCount = 0;

    private RadioButton rRead;
    private RadioButton rWrite;
    private RadioButton rReadWrite;
    private RadioGroup rgReadWrite;

    private EditText et_between;
    private Button btnStart;
    private Button btnClear;
    private Button btnBack;

    private TextView tv_read_succ_count;
    private TextView tv_read_fail_count;
    private TextView tv_write_succ_count;
    private TextView tv_write_fail_count;
    private TextView tv_continuous_count;
    private boolean threadStop = true;
    private Thread readThread;
    private Thread writeThread;
    private boolean isContinuous = true;
    SoundUtil soundUtil;
    // 连续操作变量END

    /**
     * 读Handler
     */
    private Handler readHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg != null && !threadStop) {

                if ((readSuccCount + readFailCount) % 500 == 0) {
                    tvResult.setText("");
                }

                switch (msg.arg1) {
                    case 2: {
                        readSuccCount++;
                        String uid = (String) msg.obj;

                        String id0 = uid.substring(0, 2);
                        String id1 = uid.substring(2, 4);
                        String id2 = uid.substring(4, 6);
                        String id3 = uid.substring(6, 8);
                        String id4 = uid.substring(8, 10);
                        String id5 = uid.substring(10, 12);
                        String id6 = uid.substring(12, 14);
                        String id7 = uid.substring(14, 16);

                        uid = id7 + id6 + id5 + id4 + id3 + id2 + id1 + id0;

                        tvResult.append(getString(R.string.rfid_msg_data) + " "
                                + uid);
                        soundUtil.PlaySound(SoundUtil.SoundType.SUCCESS);
                        tvResult.append("\n==============================\n");

                    }
                    break;
                    case 1: {
                        readSuccCount++;
                        String uid = (String) msg.obj;
                        String id0 = uid.substring(0, 2);
                        String id1 = uid.substring(2, 4);
                        String id2 = uid.substring(4, 6);
                        String id3 = uid.substring(6, 8);
                        String id4 = uid.substring(8, 10);
                        String id5 = uid.substring(10, 12);
                        String id6 = uid.substring(12, 14);
                        String id7 = uid.substring(14, 16);

                        tvResult.append(getString(R.string.rfid_msg_uid) + " "
                                + uid);
                        soundUtil.PlaySound(SoundUtil.SoundType.SUCCESS);
                        tvResult.append("\n==============================\n");

                    }
                    break;
                    case 0: {
                        readFailCount++;
                        soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);
                        tvResult.append(msg.obj.toString());
                        tvResult.append("\n==============================\n");
                    }
                    break;
                }

                statContinuous();
                scrollToBottom(svResult, tvResult);
            }

        }
    };

    /**
     * 写Handler
     */
    private Handler writeHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg != null && !threadStop) {

                if ((writeSuccCount + writeFailCount) % 500 == 0) {
                    tvResult.setText("");
                }

                switch (msg.arg1) {
                    case 1: {
                        writeSuccCount++;
                        soundUtil.PlaySound(SoundUtil.SoundType.SUCCESS);
                        tvResult.append(getString(R.string.rfid_msg_write_succ));
                        tvResult.append("\n==============================\n");

                    }
                    break;
                    case 0: {
                        soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);
                        writeFailCount++;
                        tvResult.append(msg.obj.toString());
                        tvResult.append("\n==============================\n");

                    }
                    break;
                }

                statContinuous();

                scrollToBottom(svResult, tvResult);

            }

        }
    };

    /**
     * 读线程
     *
     * @author liuruifeng
     */
    class ReadRunnable implements Runnable {
        private boolean isContinuous = false;
        private long sleepTime = 1000;
        private int block;
        Message msg = null;

        public ReadRunnable(boolean isContinuous, int sleep, int block) {
            this.isContinuous = isContinuous;
            this.sleepTime = sleep;
            this.block = block;
        }

        @Override
        public void run() {

            do {

                msg = new Message();

                if (isContinuous) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                String data = null;
                try {
                    data = mRFID.read(block);
                } catch (RFIDNotFoundException e) {
                    msg.arg1 = 0;
                    msg.obj = getText(R.string.rfid_msg_read_fail);
                    soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);
                    readHandler.sendMessage(msg);
                    continue;
                }
                if (data == null) {
                    msg.arg1 = 0;
                    msg.obj = getText(R.string.rfid_mgs_error_not_found);
                    soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);
                    readHandler.sendMessage(msg);
                    continue;
                } else {
                    msg.arg1 = 2;
                    msg.obj = data;
                    soundUtil.PlaySound(SoundUtil.SoundType.SUCCESS);
                    readHandler.sendMessage(msg);
                }

            } while (isContinuous && !threadStop);
        }

    }

    /**
     * 写线程
     *
     * @author liuruifeng
     */
    class WriteRunnable implements Runnable {

        private boolean isContinuous = false;
        private long sleepTime = 1000;
        private int block;
        String strData;
        Message msg = null;

        public WriteRunnable(boolean isContinuous, int sleep, int block,
                             String data) {
            this.isContinuous = isContinuous;
            this.sleepTime = sleep;
            this.block = block;
            this.strData = data;
        }

        @Override
        public void run() {

            do {
                msg = new Message();
                if (isContinuous) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                boolean bResult = false;

                try {
                    bResult = mRFID.write(block, strData);
                } catch (RFIDNotFoundException e) {
                    msg.arg1 = 0;
                    msg.obj = getText(R.string.rfid_mgs_error_not_found);

                    writeHandler.sendMessage(msg);
                    continue;
                }

                if (bResult) {
                    // 写入成功
                    msg.arg1 = 1;
                    msg.obj = getText(R.string.rfid_msg_write_succ);
                    soundUtil.PlaySound(SoundUtil.SoundType.SUCCESS);
                    writeHandler.sendMessage(msg);
                    continue;
                } else {
                    msg.arg1 = 0;
                    msg.obj = getText(R.string.rfid_msg_write_fail);
                    soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);
                    writeHandler.sendMessage(msg);
                    continue;
                }

            } while (isContinuous && !threadStop);

        }

    }

    /**
     * 初始化连续读写控件变量
     */
    private void initContinuous() {
        rRead = (RadioButton) findViewById(R.id.rRead);
        rWrite = (RadioButton) findViewById(R.id.rWrite);
        rReadWrite = (RadioButton) findViewById(R.id.rReadWrite);
        rgReadWrite = (RadioGroup) findViewById(R.id.rgReadWrite);

        et_between = (EditText) findViewById(R.id.et_between);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnClear = (Button) findViewById(R.id.btnClear);
        btnBack = (Button) findViewById(R.id.btnBack);

        tv_read_succ_count = (TextView) findViewById(R.id.tv_read_succ_count);
        tv_read_fail_count = (TextView) findViewById(R.id.tv_read_fail_count);
        tv_write_succ_count = (TextView) findViewById(R.id.tv_write_succ_count);
        tv_write_fail_count = (TextView) findViewById(R.id.tv_write_fail_count);
        tv_continuous_count = (TextView) findViewById(R.id.tv_continuous_count);

        readSuccCount = 0;
        readFailCount = 0;
        writeSuccCount = 0;
        writeFailCount = 0;

        btnClear.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                resetContinuous();

            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                llMultiple.setVisibility(View.GONE);
                llMultiple.setAnimation(AnimationUtils.loadAnimation(
                        MainActivity.this, android.R.anim.slide_out_right));
                llSingle.setVisibility(View.VISIBLE);
                llSingle.setAnimation(AnimationUtils.loadAnimation(
                        MainActivity.this, android.R.anim.slide_in_left));

                resetContinuous();
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                btnClear.setEnabled(!threadStop);

                if (threadStop) {

                    threadStop = false;

                    btnStart.setText(R.string.title_stop);

                    int sleep = 0;// 间隔时间

                    String strBetween = et_between.getText().toString().trim();
                    if (StringUtility.isEmpty(strBetween)) {

                    } else {
                        sleep = toInt(strBetween, 0);// 毫秒
                    }

                    int block = Integer.parseInt(spBlock.getSelectedItem()
                            .toString());

                    // 判断读写类型。。
                    if (rRead.isChecked()) {
                        readThread = new Thread(new ReadRunnable(isContinuous,
                                sleep, block));

                        readThread.start();

                    } else {

                        String strData = etWriteData.getText().toString();

                        if (strData.length() == 0) {
                            // 写入内容不能为空
                            tvResult.setText(R.string.rfid_mgs_error_not_write_null);
                            return;
                        } else if (!vailHexInput(strData)) {
                            // 请输入十六进制数
                            tvResult.setText(R.string.rfid_mgs_error_nohex);
                            return;
                        }

                        if (rWrite.isChecked()) {

                            writeThread = new Thread(new WriteRunnable(
                                    isContinuous, sleep, block, strData));

                            writeThread.start();

                        } else if (rReadWrite.isChecked()) {
                            readThread = new Thread(new ReadRunnable(
                                    isContinuous, sleep, block));

                            writeThread = new Thread(new WriteRunnable(
                                    isContinuous, sleep, block, strData));

                            readThread.start();
                            writeThread.start();

                        }
                    }

                } else {
                    threadStop = true;
                    btnStart.setText(R.string.title_start);
                }

            }
        });

        // 连续操作方式改变时
        rgReadWrite.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                threadStop = true;
                btnStart.setText(R.string.title_start);
                resetContinuous();
            }
        });

    }

    /**
     * 重置连续读写内容
     */
    private void resetContinuous() {
        readSuccCount = 0;
        readFailCount = 0;
        writeSuccCount = 0;
        writeFailCount = 0;

        threadStop = true;
        tvResult.setText("");

        tv_continuous_count.setText("0");
        tv_read_fail_count.setText("0");
        tv_read_succ_count.setText("0");
        tv_write_fail_count.setText("0");
        tv_write_succ_count.setText("0");
        btnStart.setText(getString(R.string.title_start));
        btnClear.setEnabled(true);

    }

    /**
     * 统计相关数据
     */
    private void statContinuous() {
        int total = readSuccCount + readFailCount + writeSuccCount
                + writeFailCount;

        if (total % 1000 == 0) {
            tvResult.setText("");
        }

        tv_continuous_count.setText(String.valueOf(total));
        tv_read_succ_count.setText(String.valueOf(readSuccCount));
        tv_read_fail_count.setText(String.valueOf(readFailCount));
        tv_write_succ_count.setText(String.valueOf(writeSuccCount));
        tv_write_fail_count.setText(String.valueOf(writeFailCount));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        soundUtil = new SoundUtil(this);
        try {
            mRFID = RFIDWithISO15693.getInstance();
        } catch (ConfigurationException e) {

            Toast.makeText(MainActivity.this, R.string.rfid_mgs_error_config,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        init();
        initData();
        initContinuous();
    }

    private void init() {
        svResult = (ScrollView) findViewById(R.id.svResult);

        spBlock = (Spinner) findViewById(R.id.spBlock);
        btnRead = (Button) findViewById(R.id.btnRead);
        btnReadId = (Button) findViewById(R.id.btnReadId);
        btnWrite = (Button) findViewById(R.id.btnWrite);
        tvResult = (TextView) findViewById(R.id.tvResult);
        etWriteData = (EditText) findViewById(R.id.etWriteData);
        etAFI = (EditText) findViewById(R.id.etAFI);
        etDSFID = (EditText) findViewById(R.id.etDSFID);
        btnWriteAFI = (Button) findViewById(R.id.btnWriteAFI);
        btnWriteDSFID = (Button) findViewById(R.id.btnWriteDSFID);
        btnLockAFI = (Button) findViewById(R.id.btnLockAFI);
        btnLockDSFID = (Button) findViewById(R.id.btnLockDSFID);

        llSingle = (LinearLayout) findViewById(R.id.llSingle);
        llMultiple = (LinearLayout) findViewById(R.id.llMultiple);
        btnContinuous = (Button) findViewById(R.id.btnContinuous);

        btnRead.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                readTag();

            }
        });

        btnWrite.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                writeTag();

            }
        });

        btnReadId.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                scan();

            }
        });
        btnWriteAFI.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                writeAFI();

            }
        });
        btnWriteDSFID.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                writeDSFID();

            }
        });

        btnLockAFI.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                lockAFI();

            }
        });

        btnLockDSFID.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                lockDSFID();

            }
        });

        btnContinuous.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                llSingle.setVisibility(View.GONE);
                llSingle.setAnimation(AnimationUtils.loadAnimation(
                        MainActivity.this, android.R.anim.slide_out_right));
                llMultiple.setVisibility(View.VISIBLE);
                llMultiple.setAnimation(AnimationUtils.loadAnimation(
                        MainActivity.this, android.R.anim.slide_in_left));

                resetContinuous();

            }
        });

    }

    public void writeAFI() {
        String afi = etAFI.getText().toString();

        if (afi.length() != 2) {
            tvResult.setText(R.string.rfid_msg_1byte_fail);
            return;
        } else if (!vailHexInput(afi)) {
            // 请输入十六进制数
            tvResult.setText(R.string.rfid_mgs_error_nohex);
            return;
        }

        boolean bResult = false;
        try {
            bResult = mRFID.writeAFI(Integer.parseInt(afi));
        } catch (NumberFormatException e) {
            tvResult.setText(R.string.rfid_msg_1byte_fail);
            return;
        } catch (RFIDNotFoundException e) {
            tvResult.setText(R.string.rfid_mgs_error_not_found);
            return;
        }

        if (bResult) {
            // 写入成功
            tvResult.setText(R.string.rfid_msg_write_succ);

        } else {
            tvResult.setText(R.string.rfid_msg_write_fail);
        }
    }

    public void lockAFI() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.rfid_msg_confirm_title)
                .setMessage(R.string.rfid_msg_confirm_afi)
                .setPositiveButton(R.string.rfid_msg_confirm_true,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                boolean bResult = false;
                                try {
                                    bResult = mRFID.lockAFI();

                                } catch (NumberFormatException e) {
                                    tvResult.setText(R.string.rfid_msg_1byte_fail);

                                    return;
                                } catch (RFIDNotFoundException e) {
                                    tvResult.setText(R.string.rfid_mgs_error_not_found);
                                    return;
                                }

                                if (bResult) {
                                    // 锁定成功
                                    tvResult.setText(R.string.rfid_msg_lock_succ);

                                } else {
                                    tvResult.setText(R.string.rfid_msg_lock_fail);
                                }

                            }
                        })
                .setNegativeButton(R.string.rfid_msg_confirm_flase,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();

                            }
                        }).show();
    }

    public void writeDSFID() {
        String dsfid = etDSFID.getText().toString();

        if (dsfid.length() != 2) {
            tvResult.setText(R.string.rfid_msg_1byte_fail);
            return;
        } else if (!vailHexInput(dsfid)) {
            // 请输入十六进制数
            tvResult.setText(R.string.rfid_mgs_error_nohex);
            return;
        }

        boolean bResult = false;
        try {
            bResult = mRFID.writeDSFID(Integer.parseInt(dsfid));
        } catch (NumberFormatException e) {
            tvResult.setText(R.string.rfid_msg_1byte_fail);
            return;
        } catch (RFIDNotFoundException e) {
            tvResult.setText(R.string.rfid_mgs_error_not_found);
            return;
        }

        if (bResult) {
            // 写入成功
            tvResult.setText(R.string.rfid_msg_write_succ);

        } else {
            tvResult.setText(R.string.rfid_msg_write_fail);
        }
    }

    public void lockDSFID() {

        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.rfid_msg_confirm_title)
                .setMessage(R.string.rfid_msg_confirm_dsfid)
                .setPositiveButton(R.string.rfid_msg_confirm_true,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                boolean bResult = false;
                                try {
                                    bResult = mRFID.lockDSFID();

                                } catch (NumberFormatException e) {
                                    tvResult.setText(R.string.rfid_msg_1byte_fail);
                                    return;
                                } catch (RFIDNotFoundException e) {
                                    tvResult.setText(R.string.rfid_mgs_error_not_found);
                                    return;
                                }

                                if (bResult) {
                                    // 锁定成功
                                    tvResult.setText(R.string.rfid_msg_lock_succ);

                                } else {
                                    tvResult.setText(R.string.rfid_msg_lock_fail);

                                }

                            }
                        })
                .setNegativeButton(R.string.rfid_msg_confirm_flase,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();

                            }
                        }).show();

    }

    public void readTag() {

        String data = null;

        try {
            data = mRFID.read(spBlock.getSelectedItemPosition());
            Log.e("TAG", "----------------------" + data);
            if (data == null) {
                tvResult.append(getString(R.string.rfid_mgs_error_not_found)
                        + "\n");
                soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);
                return;
            } else {
                soundUtil.PlaySound(SoundUtil.SoundType.SUCCESS);
            }

        } catch (RFIDNotFoundException e) {
            tvResult.append(getString(R.string.rfid_msg_read_fail) + "\n");
            soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);
            return;
        }

//        tvResult.setText("");

        tvResult.append(getString(R.string.rfid_msg_data) + " " + data);

    }

    private void writeTag() {
        String strData = etWriteData.getText().toString();

        if (strData.length() == 0) {
            // 写入内容不能为空
            tvResult.setText(R.string.rfid_mgs_error_not_write_null);
            soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);
            return;
        } else if (!vailHexInput(strData)) {
            // 请输入十六进制数
            tvResult.setText(R.string.rfid_mgs_error_nohex);
            soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);
            return;
        }

        int block = spBlock.getSelectedItemPosition();

        boolean bResult = false;

        try {
            bResult = mRFID.write(block, strData);
        } catch (RFIDNotFoundException e) {
            tvResult.setText(R.string.rfid_mgs_error_not_found);
            return;
        }

        if (bResult) {
            // 写入成功
            tvResult.setText(R.string.rfid_msg_write_succ);
            soundUtil.PlaySound(SoundUtil.SoundType.SUCCESS);

        } else {
            tvResult.setText(R.string.rfid_msg_write_fail);
            soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);

        }

    }

    private void scan() {
        String uid = null;

        uid = mRFID.inventory();

        if (uid == null) {

            soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);
            tvResult.append(getString(R.string.rfid_mgs_error_not_found));
            tvResult.append("\n============\n");
            scrollToBottom(svResult, tvResult);

            return;
        } else {
            soundUtil.PlaySound(SoundUtil.SoundType.SUCCESS);
        }
        String id0 = uid.substring(0, 2);
        String id1 = uid.substring(2, 4);
        String id2 = uid.substring(4, 6);
        String id3 = uid.substring(6, 8);
        String id4 = uid.substring(8, 10);
        String id5 = uid.substring(10, 12);
        String id6 = uid.substring(12, 14);
        String id7 = uid.substring(14, 16);
        uid = id7 + id6 + id5 + id4 + id3 + id2 + id1 + id0;


        tvResult.append(getString(R.string.rfid_msg_uid) + " " + uid);

        tvResult.append("\n============\n");

        scrollToBottom(svResult, tvResult);
    }

    private void initData() {

        // adapterBlock
        arrBlock.clear();
        arrBlock.addAll(builNum(28));

        adapterBlock = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_spinner_item, arrBlock);
        adapterBlock
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBlock.setAdapter(adapterBlock);
    }

    public ArrayList<String> builNum(int count) {
        if (count < 1) {
            return null;

        }

        ArrayList<String> arrStr = new ArrayList<String>();

        for (int i = 0; i < count; i++) {
            arrStr.add(String.valueOf(i));

        }
        return arrStr;
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        new InitTask().execute();

    }

    /**
     * 设备上电异步类
     *
     * @author liuruifeng
     */
    public class InitTask extends AsyncTask<String, Integer, Boolean> {
        ProgressDialog mypDialog;

        @Override
        protected Boolean doInBackground(String... params) {
            // TODO Auto-generated method stub
            return mRFID.init();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);

            mypDialog.cancel();

            if (!result) {
                Toast.makeText(MainActivity.this, "init fail",
                        Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();

            mypDialog = new ProgressDialog(MainActivity.this);
            mypDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mypDialog.setMessage("init...");
            mypDialog.setCanceledOnTouchOutside(false);
            mypDialog.show();
        }

    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        mRFID.free();

        btnStart.setText(getString(R.string.title_start));
        threadStop = true;

        btnClear.setEnabled(true);

    }

    /**
     * 将ScrollView根据内部控件高度定位到底端
     *
     * @param scroll
     * @param inner
     */
    public void scrollToBottom(final View scroll, final View inner) {

        Handler mHandler = new Handler();

        mHandler.post(new Runnable() {
            public void run() {
                if (scroll == null || inner == null) {
                    return;
                }
                int offset = inner.getMeasuredHeight() - scroll.getHeight();
                if (offset < 0) {
                    offset = 0;
                }

                scroll.scrollTo(0, offset);
            }
        });
    }

    /**
     * 验证十六进制输入是否正确
     *
     * @param str
     * @return
     */
    public boolean vailHexInput(String str) {

        if (str == null || str.length() == 0) {
            return false;
        }

        // 长度必须是偶数
        if (str.length() % 2 == 0) {
            return StringUtility.isHexNumberRex(str);
        }

        return false;
    }

    /**
     * 字符串转整数
     *
     * @param str
     * @param defValue
     * @return
     */
    public int toInt(String str, int defValue) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
        }
        return defValue;
    }
}

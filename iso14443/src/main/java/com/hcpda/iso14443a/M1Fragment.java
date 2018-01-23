package com.hcpda.iso14443a;


import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.rscja.utility.StringUtility;

import java.util.ArrayList;

import utils.SoundUtil;

/**
 * M1-S70卡：4K字节, 共40个扇区，前32个扇区中，每个扇区4个数据块，后8个扇区中，每个扇区16个数据块，每个数据块16个字节
 * S50卡： M1卡分为16个扇区，每个扇区由4块（块0、块1、块2、块3）组成，（我们也将16个扇区的64个块按绝对地址编号为0~63）每个扇区的第三块都是密码块，密码块里分了A跟B秘钥、存取控制。
 */
public class M1Fragment extends Fragment implements AdapterView.OnItemSelectedListener, View.OnClickListener {


    static final String TAG = "TAG";
    MainActivity context;
    Spinner sp_label_type;
    Spinner sp_key_type;
    Spinner sp_block;
    Spinner sp_sector;
    EditText et_password;
    Button bt_read;
    Button bt_write;
    EditText et_write_data;
    ScrollView sv_m1;
    TextView tv_m1_result;
    ArrayAdapter sector_adapter;//扇区适配器d
    ArrayAdapter block_adapter;//块区适配器
    String[] label = {"S50", "S70"};
    String[] key = {"A", "B"};
    byte[] findCar_CMD = {(byte) 0x50, (byte) 0x00, (byte) 0x02, (byte) 0x22, (byte) 0x10, (byte) 0x52, (byte) 0x32};//寻卡数组
    String defaultPassword = "ffffffffffff";// 默认秘钥
    boolean is_receive = true;
    private ArrayList<String> sector_arr = new ArrayList<String>();//扇区数据源
    private ArrayList<String> block_arr = new ArrayList<String>();//块区数据源

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = (MainActivity) getActivity();
        return inflater.inflate(R.layout.fragment_m1, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
    }

    private void initView() {
        et_password = context.findViewById(R.id.et_password);
        et_write_data = context.findViewById(R.id.et_write_data_content);
        bt_read = context.findViewById(R.id.bt_read);
        bt_write = context.findViewById(R.id.bt_write_data);
        sv_m1 = context.findViewById(R.id.m1_scrollView);
        tv_m1_result = context.findViewById(R.id.tv_m1_result);
        sp_label_type = context.findViewById(R.id.sp_label_type);
        sp_key_type = context.findViewById(R.id.sp_key_type);
        sp_block = context.findViewById(R.id.sp_block);
        sp_sector = context.findViewById(R.id.sp_sectors);
//        标签类型适配器
        SpinnerAdapter label_adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, label);
//        秘钥类型适配器
        ArrayAdapter ke_adapter = new ArrayAdapter(context, android.R.layout.simple_spinner_item, key);
        ke_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        s50标签下拉扇区 适配器
        sector_arr.clear();
        sector_arr.addAll(set_sp_number(16));
        sector_adapter = new ArrayAdapter(context, android.R.layout.simple_spinner_item, sector_arr);
        sector_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        块区适配器
        block_arr.clear();
        block_arr.addAll(set_sp_number(4));
        block_adapter = new ArrayAdapter(context, android.R.layout.simple_spinner_item, block_arr);
        block_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp_block.setAdapter(block_adapter);//下拉块区适配器
        sp_sector.setAdapter(sector_adapter);//下拉扇区设置适配器
        sp_key_type.setAdapter(ke_adapter);
        sp_label_type.setAdapter(label_adapter);
        sp_label_type.setOnItemSelectedListener(this);
        sp_key_type.setOnItemSelectedListener(this);
        sp_sector.setOnItemSelectedListener(this);
        sp_block.setOnItemSelectedListener(this);
        bt_write.setOnClickListener(this);
        bt_read.setOnClickListener(this);
        sp_sector.setSelection(1);
    }

    //    界面所有下拉选择监听
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        getBlock();
        switch (parent.getId()) {
            //标签类型
            case R.id.sp_label_type:
                String select = (String) parent.getSelectedItem();
                if (select.equals("S70")) {
                    sector_adapter.clear();
                    sector_adapter.addAll(set_sp_number(40));
                    sector_adapter.notifyDataSetChanged();
                } else {
                    sector_adapter.clear();
                    sector_adapter.addAll(set_sp_number(16));
                    sector_adapter.notifyDataSetChanged();
                }
                break;
            //秘钥类型
            case R.id.sp_key_type:
                break;
            //扇区
            case R.id.sp_sectors:
                if (position > 32) {
                    block_adapter.clear();
                    block_adapter.addAll(set_sp_number(16));
                    block_adapter.notifyDataSetChanged();
                } else {
                    block_adapter.clear();
                    block_adapter.addAll(set_sp_number(4));
                    block_adapter.notifyDataSetChanged();
                }
                break;
            //块区
            case R.id.sp_block:
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    //    界面所有按钮监听
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_read:
                String success_data = read();
                context.soundUtil.PlaySound(SoundUtil.SoundType.SUCCESS);
                tv_m1_result.append("ID:" + context.rfid_14443A.read_id() + "\n" + "数据：" + success_data + "\n");
                scrollToBottom(sv_m1,tv_m1_result);
                break;
            case R.id.bt_write_data:
                String write_data = et_write_data.getText().toString().trim();
                Log.e(TAG, "onClick: " + write_data );
                if (!vailHexInput(write_data)) {
                    Toast.makeText(getActivity(), "内容必须是16进制", Toast.LENGTH_SHORT).show();
                } else if (write_data.length() == 0) {
                    Toast.makeText(getActivity(), "写入内容不能为空", Toast.LENGTH_SHORT).show();
                } else if (sp_block.getSelectedItemPosition() == 3) {
                    Toast.makeText(getActivity(), "此块区是密码控制块，不可操作", Toast.LENGTH_SHORT).show();
                }
                if (write()) {
                    tv_m1_result.append("数据写入成功" + "\n");
                    context.soundUtil.PlaySound(SoundUtil.SoundType.SUCCESS);
                }
                break;
        }
    }

    private String read() {
        return context.rfid_14443A.read(getKey_type(), "FFFFFFFFFFFF", getBlock());
    }

    //写卡
    private boolean write() {
        return context.rfid_14443A.write("FFFFFFFFFFFF", getKey_type(), getBlock(), et_write_data.getText().toString().trim());
//        String write_data = et_write_data.getText().toString().trim();
//        if (!vailHexInput(write_data)) {
//            Toast.makeText(getActivity(), "内容必须是16进制", Toast.LENGTH_SHORT).show();
//        } else if (write_data.length() == 0) {
//            Toast.makeText(getActivity(), "写入内容不能为空", Toast.LENGTH_SHORT).show();
//        } else if (sp_block.getSelectedItemPosition() == 3) {
//            Toast.makeText(getActivity(), "此块区是密码控制块，不可操作", Toast.LENGTH_SHORT).show();
//        } else {
//            int verify_LRC = 0;
//            if (context.module.send(findCar_CMD)) {
//                byte[] data = context.module.receive();
//                Log.e(TAG, "写卡返回数组长度---" + data.length);
//                String data_to = "";
//                if (data.length > 6) {
//                    Log.e(TAG, "ID长度 data[7] = " + data[7]);
//                    data_to = StringUtility.bytes2HexString(data, data.length);
//                    Log.e(TAG, "写卡返回--------" + data_to.toString());
//                } else {
//                    tv_m1_result.append("请把卡靠近设备" + "\n");
//                }
//                if (data_to.length() > 24) {
//                    //寻卡成功，继续认证秘钥 data[7]  表示ID数据长度，所以要从16开始截取，再减去2位的LRC
//                    String ID = data_to.substring(16, data_to.length() - 2);
//                    if (data[7] == 4) {
//                        verify_LRC = (byte) 0x50 ^ (byte) 0x00 ^ (byte) 0x0c ^ (byte) 0x16 ^ (byte) Integer.parseInt(getKey_type(), 16) ^ (byte) Integer.parseInt(getBlock(), 16) ^ Integer.parseInt(Integer.toHexString(data[8] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[9] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[10] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[11] & 0xFF), 16) ^ (byte) 0xff ^ (byte) 0xff ^ (byte) 0xff ^ (byte) 0xff ^ (byte) 0xff ^ (byte) 0xff;//计算 UID 8位的校验位
//                    } else if (data[7] == 7) {
//                        verify_LRC = 0x50 ^ 0x00 ^ 0x0c ^ 0x16 ^ Integer.parseInt(getKey_type(), 16) ^ Integer.parseInt(getBlock(), 16) ^ Integer.parseInt(Integer.toHexString(data[8] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[9] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[10] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[11] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[12] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[13] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[14] & 0xFF), 16) ^ 0xff ^ 0xff ^ 0xff ^ 0xff ^ 0xff ^ 0xff;//计算 UID 14位的校验位
//                    }
//                    String ren_lrc = Integer.toHexString(verify_LRC);
//                    String psw = et_password.getText().toString().trim();
//                    Log.e(TAG, "认证校验---------++++++++++-------" + ren_lrc);
//                    if (psw.length() < 1) {
//                        //如果没有输入秘钥就默认FFFFFFFFFFFF
//                        psw = defaultPassword;
//                    }
//                    //认证命令
//                    String verify = "50" + // 50 表示数据头
//                            "000c" + // 0c 表示数据长度
//                            "16" + //0x16 为验证秘钥命令
//                            getKey_type() +//秘钥类型 60为A，61为B
//                            getBlock() + //表示数据块
//                            ID + // 卡ID
//                            psw + // 秘钥
//                            ren_lrc; // 校验位
//                    Log.e(TAG, "秘钥认证命令组合----" + verify);
//                    Log.e(TAG, "秘钥认证----校验位----" + ren_lrc);
//
//                    if (context.module.send(StringUtility.hexString2Bytes(verify))) {
//                        byte[] renz = context.module.receive();//认证接收
//                        String succ_data = StringUtility.bytes2HexString(renz, renz.length);
//                        Log.e(TAG, "认证接收数据-----" + succ_data);
//                        if (succ_data.equals("5000001646")) {
//                            byte[] write = hexString2Bytes(write_data);
//                            byte[] final_data = new byte[32];
//                            for (int i = 0; i < 32; i++) {
//                                final_data[i] = 0x00;
//                                Log.e(TAG, "写卡数组 -------------final_data[" + i + "]" + " = " + final_data[i]);
//                            }
//                            for (int i = 0; i < write.length; i++) {
//                                final_data[i] = write[i];
//                                Log.e(TAG, "写卡数组 -------------data[0] = " + final_data[i]);
//                            }
//                            int write_lrc = 0x50 ^ 0x00 ^ 0x11 ^ 0x18 ^ Integer.parseInt(getBlock(), 16) ^
//                                    final_data[0] ^ final_data[1] ^ final_data[2] ^
//                                    final_data[3] ^ final_data[4] ^ final_data[5] ^ final_data[6] ^
//                                    final_data[7] ^ final_data[8] ^ final_data[9] ^ final_data[10] ^
//                                    final_data[11] ^ final_data[12] ^ final_data[13] ^ final_data[14] ^ final_data[15];
//                            //认证秘钥通过 继续写卡
//                            String write_com = "50" +
//                                    "00" +
//                                    "11" +//数据内容总长度
//                                    "18" +// CMD
//                                    getBlock() +//数据块
//                                    StringUtility.byte2HexString(final_data[0]) +
//                                    StringUtility.byte2HexString(final_data[1]) +
//                                    StringUtility.byte2HexString(final_data[2]) +
//                                    StringUtility.byte2HexString(final_data[3]) +
//                                    StringUtility.byte2HexString(final_data[4]) +
//                                    StringUtility.byte2HexString(final_data[5]) +
//                                    StringUtility.byte2HexString(final_data[6]) +
//                                    StringUtility.byte2HexString(final_data[7]) +
//                                    StringUtility.byte2HexString(final_data[8]) +
//                                    StringUtility.byte2HexString(final_data[9]) +
//                                    StringUtility.byte2HexString(final_data[10]) +
//                                    StringUtility.byte2HexString(final_data[11]) +
//                                    StringUtility.byte2HexString(final_data[12]) +
//                                    StringUtility.byte2HexString(final_data[13]) +
//                                    StringUtility.byte2HexString(final_data[14]) +
//                                    StringUtility.byte2HexString(final_data[15]) +
//                                    getWriteLrc(write_lrc);//校验位
//
//                            Log.e(TAG, "写卡秘钥认证通过 ");
//                            Log.e(TAG, "写卡校验位是" + getWriteLrc(write_lrc));
//                            Log.e(TAG, "写卡命令组合" + write_com);
//                            if (context.module.send(StringUtility.hexString2Bytes(write_com))) {
//                                try {
//                                    Thread.sleep(200);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                                byte[] re_byte = context.module.receive();
//                                String success_data = StringUtility.bytes2HexString(re_byte, re_byte.length);
//                                Log.e(TAG, "写卡成功：数据内容是--------" + success_data);
//                                if (success_data.equals("5000001848")) {
//                                    tv_m1_result.append("数据写入成功" + "\n");
//                                    context.soundUtil.PlaySound(SoundUtil.SoundType.SUCCESS);
//                                } else if (succ_data.length() == 0) {
//                                    tv_m1_result.append("写卡失败" + "\n");
//                                    context.soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);
//                                } else {
//                                    tv_m1_result.append("写卡失败" + "\n");
//                                    context.soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);
//                                }
//                            }
//                        } else {
//                            tv_m1_result.append("秘钥认证 失败" + "\n");
//                            context.soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);
//                        }
//                    }
//                } else {
//                    tv_m1_result.append("寻卡失败" + "\n");
//                    context.soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);
//                }
//                scrollToBottom(sv_m1, tv_m1_result);
//
//            } else {
//                Log.e("TAG", "read: 发送失败");
//            }
//        }
    }

    public ArrayList<String> set_sp_number(int count) {
        if (count < 1) {
            return null;
        }
        ArrayList<String> arrStr = new ArrayList<String>();
        for (int i = 0; i < count; i++) {
            arrStr.add(String.valueOf(i));
        }
        return arrStr;
    }

    public String getKey_type() {
        return sp_key_type.getSelectedItem().toString();
    }

    public String getWriteLrc(int lrc) {
        String LRC = Integer.toHexString(lrc & 0xff);
        if (LRC.length() == 1) {
            LRC = "0" + LRC;
            return LRC;
        } else {
            return LRC;
        }
    }

    public String getBlock() {
        int block;
        String radix;
        //最终块区 = 扇区 * 4 + 块区，从0 开始数
        block = sp_sector.getSelectedItemPosition() * 4 + sp_block.getSelectedItemPosition();
        radix = Integer.toHexString(block);
        if (block <= 15) {
            return "0" + radix;
        } else {
            return radix;
        }
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
     * 十六进制字符串转byte数组
     *
     * @param s 十六进制字符串
     * @return byte数组
     */
    public static byte[] hexString2Bytes(String s) {
        byte[] bytes;
        bytes = new byte[s.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

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


}

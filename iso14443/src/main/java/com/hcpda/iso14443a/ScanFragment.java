package com.hcpda.iso14443a;


import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import utils.SoundUtil;


/**
 * A simple {@link Fragment} subclass.
 */
public class ScanFragment extends Fragment implements View.OnClickListener {
    TextView tv_result;
    ScrollView scrollView;
    MainActivity context;
    Button bt_clear;
    Button bt_read;
    static final String TAG = "TAG";
    byte[] findCar_CMD = {(byte) 0x50, (byte) 0x00, (byte) 0x02, (byte) 0x22, (byte) 0x10, (byte) 0x52, (byte) 0x32};

    public ScanFragment() {
        // Required empty public constructor
    }

    int index = 2;

    private void getFocus() {
        getView().setFocusable(true);
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == 139) {
                    if (event.getRepeatCount() == 0) {
                        index++;
                        if (index % 2 == 1) {
                            Log.e("TAG", "onKey: ");
                            read_id();
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void read_id() {
            String data_to = context.rfid_14443A.read_id();
            if (!data_to.equals("寻卡失败，请把标签靠近设备")) {
                tv_result.append("ID:" + data_to + "\n");
                context.soundUtil.PlaySound(SoundUtil.SoundType.SUCCESS);
            } else {
                tv_result.append("寻卡失败，请把标签靠近设备" + "\n");
                context.soundUtil.PlaySound(SoundUtil.SoundType.FAILURE);
            }
            scrollToBottom(scrollView, tv_result);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        context = (MainActivity) getActivity();
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tv_result = context.findViewById(R.id.tv_result);
        scrollView = context.findViewById(R.id.scrollView);
        bt_clear = context.findViewById(R.id.bt_clear);
        bt_read = context.findViewById(R.id.bt_scan);
        bt_read.setOnClickListener(this);
        bt_clear.setOnClickListener(this);
        getFocus();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_clear:
                tv_result.setText("");
                break;
            case R.id.bt_scan:
                read_id();
                break;
        }
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

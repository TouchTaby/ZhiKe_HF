package utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.hcpda.iso14443a.R;

/**
 * Created by administrator on 2017-12-18.
 * 声音播放工具，声音为蜂鸣器，分为成功和失败
 */

public class SoundUtil {

    private static int successID = -1;
    private static int failureID = -1;
    private static SoundPool soundPool = null;
    private Context cc;

    public SoundUtil(Context c) {
        cc = c;
    }

    public void PlaySound(SoundType type) {
        soundPool = getSoundPool();
        if (soundPool == null)
            return;
        int id = -1;
        if (type == SoundType.SUCCESS) {
            if (successID == -1) {
                successID = soundPool.load(cc, R.raw.barcodebeep, 1);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            id = successID;
        } else if (type == SoundType.FAILURE) {
            if (failureID == -1) {
                failureID = soundPool.load(cc, R.raw.serror, 1);
            }
            id = failureID;
        }
        if (id != -1)
            soundPool.play(id, 1, 1, 0, 0, 1);

    }
    private static SoundPool getSoundPool() {
        if (soundPool == null) {
            soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);
        }
        return soundPool;
    }

    public enum SoundType {
        FAILURE, SUCCESS
    }

}


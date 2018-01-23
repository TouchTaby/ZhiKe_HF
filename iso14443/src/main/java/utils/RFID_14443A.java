package utils;

import com.rscja.deviceapi.Module;
import com.rscja.deviceapi.exception.ConfigurationException;
import com.rscja.utility.StringUtility;

/**
 * rfid 14443A 操作类
 * 调用模块需在libs下添加deviceapi20171026.jar 和添加 libDeviceAPI.so 文件
 * 获取实例后需模块上电init(); 停止使用后关闭free（）;
 * M1-S70卡：4K字节, 共40个扇区，前32个扇区中，每个扇区4个数据块，后8个扇区中，每个扇区16个数据块，每个数据块16个字节
 * S50卡： M1卡分为16个扇区，每个扇区由4块（块0、块1、块2、块3）组成，（我们也将16个扇区的64个块按绝对地址编号为0~63）每个扇区的第三块都是密码块，密码块里分了A跟B秘钥、存取控制。
 */

public class RFID_14443A {
    private static RFID_14443A single = null;
    static Module module;
    byte[] findCar_CMD = {(byte) 0x50, (byte) 0x00, (byte) 0x02, (byte) 0x22, (byte) 0x10, (byte) 0x52, (byte) 0x32};

    public synchronized static RFID_14443A getInstance() {
        if (single == null) {
            single = new RFID_14443A();
        }
        try {
            module = Module.getInstance();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        return single;
    }

    public boolean init() {
        return module.init(2, 115200);
    }

    public boolean free() {
        return module.free();
    }

    public boolean power_on() {
        return module.powerOn(2);
    }

    public boolean power_off() {
        return module.powerOff(2);
    }

    /**
     * 寻卡id 返回卡ID号，一般为4个字节，8位长度
     * @return
     */

    public String read_id() {
        String ID = "";
        if (module.send(findCar_CMD)) {
            byte[] data = module.receive();
            String data_to = StringUtility.bytes2HexString(data, data.length);
            if (data_to.length() > 24) {
                //寻卡成功，继续认证秘钥 data[7]  表示ID数据长度，所以要从16开始截取，再减去2位的LRC
                ID = data_to.substring(16, data_to.length() - 2);
            } else {
                ID = "寻卡失败，请把标签靠近设备";
            }
        }
        return ID;
    }

    /**
     * @param key_type A跟B 秘钥 String 类型
     * @param psw      秘钥区域密码，默认为FFFFFFFFFFFF 每一个扇区都有独立的密码，每一个扇区的第四块都为独立密码块
     * @param block    绝对块号，s50卡为0~63 s70卡为0~255
     * @return 返回需要读取块区的数据，每一块区的数据长度为16个字节
     */
    public String read(String key_type, String psw, String block) {
        int verify_LRC = 0;//认证校验
        byte[] data = null;
        String read_data_ = "";
        String ID = "";
        if (module.send(findCar_CMD)) {
            data = module.receive();
            String data_to = StringUtility.bytes2HexString(data, data.length);
            if (data_to.length() > 24) {
                //寻卡成功，继续认证秘钥 data[7]  表示ID数据长度，所以要从16开始截取，再减去2位的LRC
                ID = data_to.substring(16, data_to.length() - 2);
                if (data[7] == 4) {
                    verify_LRC = (byte) 0x50 ^ (byte) 0x00 ^ (byte) 0x0c ^ (byte) 0x16 ^ (byte) Integer.parseInt(getKey_type(key_type), 16) ^ (byte) Integer.parseInt(block, 16) ^ Integer.parseInt(Integer.toHexString(data[8] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[9] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[10] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[11] & 0xFF), 16) ^ (byte) 0xff ^ (byte) 0xff ^ (byte) 0xff ^ (byte) 0xff ^ (byte) 0xff ^ (byte) 0xff;//计算 UID 8位的校验位
                } else if (data[7] == 7) {
                    verify_LRC = 0x50 ^ 0x00 ^ 0x0c ^ 0x16 ^ Integer.parseInt(getKey_type(key_type), 16) ^ Integer.parseInt(block, 16) ^ Integer.parseInt(Integer.toHexString(data[8] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[9] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[10] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[11] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[12] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[13] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[14] & 0xFF), 16) ^ 0xff ^ 0xff ^ 0xff ^ 0xff ^ 0xff ^ 0xff;//计算 UID 14位的校验位
                }
                String ren_lrc = Integer.toHexString(verify_LRC);
                //认证命令
                String verify = "50" + // 50 表示数据头
                        "000c" + // 0c 表示数据长度
                        "16" + //0x16 为验证秘钥命令
                        getKey_type(key_type) +//秘钥类型 60为A，61为B
                        block + //表示数据块
                        ID + // 卡ID
                        psw + // 秘钥
                        ren_lrc; // 校验位
                if (module.send(StringUtility.hexString2Bytes(verify))) {
                    byte[] identification = module.receive();//认证接收
                    String success_data = StringUtility.bytes2HexString(identification, identification.length);
                    if (success_data.equals("5000001646")) {
                        int read_lrc = 0x50 ^ 0x00 ^ 0x01 ^ 0x17 ^ Integer.parseInt(block, 16);
                        //认证秘钥通过 继续读卡
                        String read_com = "50" +
                                "00" +
                                "01" +//数据块长度
                                "17" +// CMD
                                block +//数据块
                                Integer.toHexString(read_lrc);//校验位
                        if (module.send(StringUtility.hexString2Bytes(read_com))) {
                            byte[] read_data = module.receive();
                            String final_data = StringUtility.bytes2HexString(read_data, read_data.length);
                            if (final_data.length() != 12) {
                                read_data_ = final_data.substring(8, 40);
                            } else {
                                read_data_ = "读卡失败";
                            }
                        }
                    }
                } else {
                    read_data_ = "认证秘钥失败";
                }
            }
        } else {
            read_data_ = "寻卡失败，请把标签靠近设备";
        }
        return read_data_;
    }

    /**
     * @param psw        块区密码
     * @param key_type   A跟B秘钥类型 String 类型
     * @param block      绝对块号，s50卡为0~63 s70卡为0~255
     * @param write_data 要求写入需为16进制 每一块区数据长度为16个字节
     * @return boolean
     */
    public boolean write(String psw, String key_type, String block, String write_data) {
        int verify_LRC = 0;
        byte[] data = module.receive();
        String data_to = "";
        if (data.length > 6) {
            data_to = StringUtility.bytes2HexString(data, data.length);
        } else {
            return false;
        }
        if (data_to.length() > 24) {
            //寻卡成功，继续认证秘钥 data[7]  表示ID数据长度，所以要从16开始截取，再减去2位的LRC
            String ID = data_to.substring(16, data_to.length() - 2);
            if (data[7] == 4) {
                verify_LRC = (byte) 0x50 ^ (byte) 0x00 ^ (byte) 0x0c ^ (byte) 0x16 ^ (byte) Integer.parseInt(getKey_type(key_type), 16) ^ (byte) Integer.parseInt(block, 16) ^ Integer.parseInt(Integer.toHexString(data[8] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[9] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[10] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[11] & 0xFF), 16) ^ (byte) 0xff ^ (byte) 0xff ^ (byte) 0xff ^ (byte) 0xff ^ (byte) 0xff ^ (byte) 0xff;//计算 UID 8位的校验位
            } else if (data[7] == 7) {
                verify_LRC = 0x50 ^ 0x00 ^ 0x0c ^ 0x16 ^ Integer.parseInt(getKey_type(key_type), 16) ^ Integer.parseInt(block, 16) ^ Integer.parseInt(Integer.toHexString(data[8] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[9] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[10] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[11] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[12] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[13] & 0xFF), 16) ^ Integer.parseInt(Integer.toHexString(data[14] & 0xFF), 16) ^ 0xff ^ 0xff ^ 0xff ^ 0xff ^ 0xff ^ 0xff;//计算 UID 14位的校验位
            }
            String ren_lrc = Integer.toHexString(verify_LRC);
            //认证命令
            String verify = "50" + // 50 表示数据头
                    "000c" + // 0c 表示数据长度
                    "16" + //0x16 为验证秘钥命令
                    getKey_type(key_type) +//秘钥类型 60为A，61为B
                    block + //表示数据块
                    ID + // 卡ID
                    psw + // 秘钥
                    ren_lrc; // 校验位
            if (module.send(StringUtility.hexString2Bytes(verify))) {
                byte[] identification = module.receive();//认证接收
                String succ_data = StringUtility.bytes2HexString(identification, identification.length);
                if (succ_data.equals("5000001646")) {
                    byte[] write = hexString2Bytes(write_data);
                    byte[] final_data = new byte[32];
                    for (int i = 0; i < 32; i++) {
                        final_data[i] = 0x00;// 默认填充满整个数据都是0，为16个字节
                    }
                    for (int i = 0; i < write.length; i++) {
                        final_data[i] = write[i];
                    }
                    int write_lrc = 0x50 ^ 0x00 ^ 0x11 ^ 0x18 ^ Integer.parseInt(block, 16) ^
                            final_data[0] ^ final_data[1] ^ final_data[2] ^
                            final_data[3] ^ final_data[4] ^ final_data[5] ^ final_data[6] ^
                            final_data[7] ^ final_data[8] ^ final_data[9] ^ final_data[10] ^
                            final_data[11] ^ final_data[12] ^ final_data[13] ^ final_data[14] ^ final_data[15];
                    //认证秘钥通过 继续写卡
                    String write_com = "50" +
                            "00" +
                            "11" +//数据内容总长度
                            "18" +// CMD
                            block +//数据块
                            StringUtility.byte2HexString(final_data[0]) +
                            StringUtility.byte2HexString(final_data[1]) +
                            StringUtility.byte2HexString(final_data[2]) +
                            StringUtility.byte2HexString(final_data[3]) +
                            StringUtility.byte2HexString(final_data[4]) +
                            StringUtility.byte2HexString(final_data[5]) +
                            StringUtility.byte2HexString(final_data[6]) +
                            StringUtility.byte2HexString(final_data[7]) +
                            StringUtility.byte2HexString(final_data[8]) +
                            StringUtility.byte2HexString(final_data[9]) +
                            StringUtility.byte2HexString(final_data[10]) +
                            StringUtility.byte2HexString(final_data[11]) +
                            StringUtility.byte2HexString(final_data[12]) +
                            StringUtility.byte2HexString(final_data[13]) +
                            StringUtility.byte2HexString(final_data[14]) +
                            StringUtility.byte2HexString(final_data[15]) +
                            getWriteLrc(write_lrc);//校验位
                    if (module.send(StringUtility.hexString2Bytes(write_com))) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        byte[] re_byte = module.receive();
                        String success_data = StringUtility.bytes2HexString(re_byte, re_byte.length);
                        if (success_data.equals("5000001848")) {
                            return true;
                        } else if (succ_data.length() == 0) {
                            return false;
                        } else {
                            return false;
                        }
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
        return false;
    }

    private String getKey_type(String key_type) {
        if (key_type.equals("A")) {
            key_type = "60";
        } else if (key_type.equals("B")) {
            key_type = "61";
        }
        return key_type;
    }

    private String getWriteLrc(int lrc) {
        String LRC = Integer.toHexString(lrc & 0xff);
        if (LRC.length() == 1) {
            LRC = "0" + LRC;
            return LRC;
        } else {
            return LRC;
        }
    }

    /**
     * 十六进制字符串转byte数组
     *
     * @param s 十六进制字符串
     * @return byte数组
     */
    private static byte[] hexString2Bytes(String s) {
        byte[] bytes;
        bytes = new byte[s.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

}

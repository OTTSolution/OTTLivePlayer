package com.xugaoxiang.djstava.live_vtm.utils;

import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by zero on 2016/10/12.
 */

public class FileUtils {

    /**
     * 是否有储存卡
     * @return
     */
    public static boolean isExternalStorageAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 保存字符串到文件
     * @param filePath
     * @param str
     * @return
     */
    public static boolean saveStringToFile(String str, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                File dir = new File(file.getParent());
                dir.mkdirs();
                file.createNewFile();
            }
            FileOutputStream os = new FileOutputStream(file);
            os.write(str.getBytes());
            os.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 从文件读取内容
     * @param path
     * @return
     */
    public static String getStringFromFile(String path) {
        String str = null;
        File file = new File(path);
        if (!file.exists())
            return str;
        FileInputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1)
                stream.write(buffer, 0, length);
            str = stream.toString();
            stream.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
     * 创建指定大小和类型的文件
     * @author cxq
     * @param targetFile 文件路径以及文件名，需要加后缀
     * @param fileLength 文件大小
     * @param unit 单位，KB,MB，GB
     * @retrun boolean
     */
    public static boolean createFile(String targetFile, long fileLength, int unit) {
        //指定每次分配的块大小
        long KBSIZE = 1024;
        long MBSIZE1 = 1024 * 1024;
        long MBSIZE10 = 1024 * 1024 * 10;
        switch (unit) {
            case 0://KB
                fileLength = fileLength * 1024;
                break;
            case 1://MB
                fileLength = fileLength * 1024*1024;
                break;
            case 2://GB
                fileLength = fileLength * 1024*1024*1024;
                break;
            default:
                break;
        }
        FileOutputStream fos = null;
        File file = new File(targetFile);
        try {
            if (!file.exists()) {
                File dir = new File(file.getParent());
                dir.mkdirs();
                file.createNewFile();
            }
            long batchSize;
            batchSize = fileLength;
            if (fileLength > KBSIZE) {
                batchSize = KBSIZE;
            }
            if (fileLength > MBSIZE1) {
                batchSize = MBSIZE1;
            }
            if (fileLength > MBSIZE10) {
                batchSize = MBSIZE10;
            }
            long count = fileLength / batchSize;
            long last = fileLength % batchSize;

            fos = new FileOutputStream(file);
            FileChannel fileChannel = fos.getChannel();
            for (int i = 0; i < count; i++) {
                ByteBuffer buffer = ByteBuffer.allocate((int) batchSize);
                fileChannel.write(buffer);

            }
            if (last != 0) {
                ByteBuffer buffer = ByteBuffer.allocate((int) last);
                fileChannel.write(buffer);
            }
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}

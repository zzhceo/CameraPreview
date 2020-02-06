package com.zzh.camerapreview.utils;

import android.os.Environment;
import android.util.Log;


import com.zzh.camerapreview.constants.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class FileHelper {

    public static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String TAG = FileHelper.class.getSimpleName();
    private static final int SAMPLING_RATE = Constants.WAV_FILE_SAMPLERATE;
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FOLDER = "audioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final String AUDIO_RECORDER_TEMP_TEST_FILE = "record_temp_test.raw";
    private static final String AUDIO_RECORDER_CHANGED_SAMPLE_FILE = "record_temp_changed.raw";
    public static int AUDIO_SOURCE_SAMPLING_RATE = Constants.RECORDER_SAMPLERATE;//todo 需要设置音频数据来源的采样率

    private FileHelper() {
    }

    /**
     * 在AUDIO_RECORDER_FOLDER获取文件路径
     *
     * @param fileName 文件名
     * @return 文件路径
     */
    public static String getFilename(String fileName) {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + fileName +
                AUDIO_RECORDER_FILE_EXT_WAV);
    }

    /**
     * 原始pcm数据文件
     *
     * @return
     */
    public static String getTempFilename() {
        return getFilePath(AUDIO_RECORDER_TEMP_FILE);
    }

    /**
     * 转换成16k 采样率的文件
     *
     * @return
     */
    public static String getChangeSampleFile() {
        return getFilePath(AUDIO_RECORDER_CHANGED_SAMPLE_FILE);
    }

    /**
     * 调试保存所有录音过程的文件
     *
     * @return
     */
    public static String getTempFilenameTest() {
        return getFilePath(AUDIO_RECORDER_TEMP_TEST_FILE);
    }

    public static String getDebugVolumePath() {
        return getFilePath("debugVolume_" + System.currentTimeMillis() + ".txt");
    }

    public static String getDebugVolumeResultPath() {
        return getFilePath("debugVolumeResult_" + System.currentTimeMillis() + ".txt");
    }

    private static String getFilePath(String fileName) {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, fileName);

        if (tempFile.exists()) {
            tempFile.delete();
        }

        return (file.getAbsolutePath() + "/" + fileName);
    }

    public static void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    /**
     * 转成wav文件
     *
     * @param bufferSize
     * @param fileName
     */
    public static void copyWaveFile(int bufferSize, String fileName) {
        copyWaveFile(bufferSize, fileName, getTempFilename());
    }

    public static void copyWaveFileTest(int bufferSize, String fileName) {
        copyWaveFile(bufferSize, fileName, getTempFilenameTest());
    }

    /**
     * 转成wav文件
     */
    private static void copyWaveFile(int bufferSize, String fileName, String tempFilename) {
        int channels = 1;
        long byteRate = RECORDER_BPP * SAMPLING_RATE * channels / 8;

        byte[] data = new byte[bufferSize];

        try {
            FileInputStream in = new FileInputStream(tempFilename);
            FileOutputStream out = new FileOutputStream(getFilename(fileName));
            long totalAudioLen = in.getChannel().size();
            long totalDataLen = totalAudioLen + 36;

            writeWaveFileHeader(out, totalAudioLen, totalDataLen,
                    (long) SAMPLING_RATE, channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy wave file.");
            e.printStackTrace();
        }
    }

    private static void writeWaveFileHeader(
            FileOutputStream out,
            long totalAudioLen,
            long totalDataLen,
            long longSampleRate,
            int channels,
            long byteRate1) throws IOException {
        byte[] header = new byte[44];
        // ChunkID, RIFF, 占4bytes
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        // ChunkSize, pcmLen + 36, 占4bytes
        long chunkSize = totalAudioLen + 36;
        header[4] = (byte) (chunkSize & 0xff);
        header[5] = (byte) ((chunkSize >> 8) & 0xff);
        header[6] = (byte) ((chunkSize >> 16) & 0xff);
        header[7] = (byte) ((chunkSize >> 24) & 0xff);
        // Format, WAVE, 占4bytes
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        // Subchunk1ID, 'fmt ', 占4bytes
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        // Subchunk1Size, 16, 占4bytes
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        // AudioFormat, pcm = 1, 占2bytes
        header[20] = 1;
        header[21] = 0;
        // NumChannels, mono = 1, stereo = 2, 占2bytes
        header[22] = (byte) channels;
        header[23] = 0;
        // SampleRate, 占4bytes
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        // ByteRate = SampleRate * NumChannels * BitsPerSample / 8, 占4bytes
        long byteRate = longSampleRate * channels * RECORDER_BPP / 8;
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // BlockAlign = NumChannels * BitsPerSample / 8, 占2bytes
        header[32] = (byte) (channels * RECORDER_BPP / 8);
        header[33] = 0;
        // BitsPerSample, 占2bytes
        header[34] = (byte) RECORDER_BPP;
        header[35] = 0;
        // Subhunk2ID, data, 占4bytes
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        // Subchunk2Size, 占4bytes
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }


}

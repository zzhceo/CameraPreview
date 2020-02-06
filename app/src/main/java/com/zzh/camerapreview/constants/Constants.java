package com.zzh.camerapreview.constants;

import android.media.AudioFormat;
import android.media.MediaRecorder;

import java.util.UUID;

public class Constants {
    public static final int RECORDER_SAMPLERATE = 16000;//本地录音的采样率
    public static final int WAV_FILE_SAMPLERATE = 16000;//固定数值，语音识别库要16k
    public static final int AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT;
    public static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    public static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    /*Hint: If you are connecting to a Bluetooth serial board then try using the well-known SPP
        java.util.UUID 00001101-0000-1000-8000-00805F9B34FB.
            However if you are connecting to an Android peer then please generate your own unique UUID.*/
    public static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//    public static final UUID SPP_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
}

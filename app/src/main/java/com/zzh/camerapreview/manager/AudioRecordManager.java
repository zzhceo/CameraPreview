package com.zzh.camerapreview.manager;

import android.media.AudioRecord;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.zzh.camerapreview.utils.FileHelper;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.zzh.camerapreview.constants.Constants.AUDIO_SOURCE;
import static com.zzh.camerapreview.constants.Constants.RECORDER_AUDIO_ENCODING;
import static com.zzh.camerapreview.constants.Constants.RECORDER_CHANNELS;
import static com.zzh.camerapreview.constants.Constants.RECORDER_SAMPLERATE;

public class AudioRecordManager {
    // time pattern
    public static final String AUDIO_RECORD_ELAPSE_TIME_PATTERN = "mm:ss:SS";
    private static final String TAG = AudioRecordManager.class.getSimpleName();
    private final int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    public String recordAudioFilePath = "";
    protected long startTime;
    protected boolean emptyRecord = false;
    protected boolean recordExists = false;
    protected boolean recordError = false;
    protected AudioRecord recorder = null;
    protected Thread recordingThread = null, timerThread = null;
    AudioRecordManagerHost host;
    private FileOutputStream outputStream = null;
    // add by zzh
    private boolean needSaveRecord = false;
    private String audioFileName;
    private boolean isRecording;
    private boolean needAutoStop = false;
    // unit second
    private int autoStopTime = 10;

    public AudioRecordManager(String audioFileName) {
        this.audioFileName = audioFileName;
    }

    public void setAudioFileName(String audioFileName) {
        this.audioFileName = audioFileName;
    }

    public void setAutoStopParam(boolean needAutoStop, int autoStopTime) {
        this.needAutoStop = needAutoStop;
        if (autoStopTime > 0)
            this.autoStopTime = autoStopTime;
    }

    public void setSaveFileParam(boolean needSaveFile) {
        this.needSaveRecord = needSaveFile;
    }

    public void setHost(AudioRecordManagerHost host) {
        this.host = host;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void startRecording() {
        if (isRecording) {
            Log.w(TAG, "startRecording failed, isRecording");
        }
        Log.i(TAG, "startRecording");
        isRecording = true;

        emptyRecord = true;
        recordError = false;
        if (host != null)
            host.showOrHideStartRecordButton(false);

        //Start the record
        recorder = new AudioRecord(AUDIO_SOURCE,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize);
        recorder.startRecording();

        if (recordExists) {
//            speakerRecg.resetInput();
            recordExists = false;
        }
        // add by zzh
        startTime = System.currentTimeMillis();
        //This thread is meant to record the audio samples with android recorder.
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
//                startTime = System.currentTimeMillis();

                String filePath = FileHelper.getTempFilename();
                if (needSaveRecord) {
                    try {
                        outputStream = new FileOutputStream(filePath);
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "Failed to find audio file.", e);
                        return;
                    }
                }

                byte[] buf = new byte[bufferSize];
                while (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    int samplesRead = recorder.read(buf, 0, buf.length);
//                    Log.e(TAG, "is recording");
                    if (needSaveRecord) {
                        if (samplesRead > 0) {
                            emptyRecord = false;
                            if (outputStream != null) {
                                try {
                                    outputStream.write(buf);
                                } catch (IOException e) {
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }, "AudioRecorder Thread");

        //This thread is meant to increase the timer with the current time.
        timerThread = new Thread(new Runnable() {
            private Handler handler = new Handler();

            @Override
            public void run() {
                while ((recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            long currentTime = System.currentTimeMillis() - startTime;
                            String result = new SimpleDateFormat(AUDIO_RECORD_ELAPSE_TIME_PATTERN, Locale.SIMPLIFIED_CHINESE)
                                    .format(new Date(currentTime));
//                            timeText.setText(result);
                            if (host != null)
                                host.updateRecordTime(result);
//                            Log.i(TAG, "showOrHideStartRecordButton stop record 11, autoStopTime="
//                                    + autoStopTime + ", currentTime=" + currentTime + ", start time=" + startTime);
                            if (needAutoStop && autoStopTime > 0 && currentTime >= autoStopTime * 1000) {
//                                Log.i(TAG, "showOrHideStartRecordButton stop record");
                                stopRecording();
                            }
                        }
                    });
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "Timer Thread");

        recordingThread.start();
        timerThread.start();
    }

    /**
     * Stop the record and reset record features.
     */
    public void stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "stopRecording failed, is not Recording");
        }
        isRecording = false;
        if (host != null)
            host.showOrHideStartRecordButton(true);

        if (recorder != null) {
            recorder.stop();
            try {
                recordingThread.join();
                timerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            recorder.release();
            recorder = null;
            recordExists = !recordError;
            recordingThread = null;
//            if (host != null)
//                host.showOrHideStartRecordButton(true);

            String resultText = "recording_completed";
            if (recordError) {
                resultText = "recording_not_completed";
            }
            // After recording, copy from temp file to WAV file.
            // Close stream.
            try {
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close stream.", e);
                return;
            }
            if (!TextUtils.isEmpty(audioFileName)) {
                String fileName = audioFileName;
                FileHelper.copyWaveFile(bufferSize, fileName);
                recordAudioFilePath = FileHelper.getFilename(fileName);
                Log.i(TAG, "record finished, recordAudioFilePath=" + recordAudioFilePath);
                FileHelper.deleteTempFile();
            }
        }
    }

    public String getRecordAudioFilePath() {
        Log.i(TAG, "getRecordAudioFilePath, recordAudioFilePath=" + recordAudioFilePath);
        return recordAudioFilePath;
    }

    public interface AudioRecordManagerHost {
        void showOrHideStartRecordButton(boolean isShow);

        void updateRecordTime(String time);
    }
}

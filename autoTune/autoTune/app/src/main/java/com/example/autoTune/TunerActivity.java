package com.example.autoTune;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TunerActivity extends AppCompatActivity {

    private static final String TAG = TunerActivity.class.getCanonicalName();

    public static final String STATE_NEEDLE_POS = "needle_pos";
    public static final String STATE_PITCH_INDEX = "pitch_index";
    public static final String STATE_LAST_FREQ = "last_freq";
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 443;



    private SeekBar elevation;
    private TextView debug;
    private TextView status;
    private Bluetooth bt;

    private Tuning mTuning;
    private AudioProcessor mAudioProcessor;
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private NeedleView mNeedleView;
    private TuningView mTuningView;
    private TextView mFrequencyView;

    private boolean mProcessing = false;

    private int mPitchIndex;
    private float mLastFreq;


    @Override
    protected void onStart() {
        super.onStart();
        if(Utils.checkPermission(this, Manifest.permission.RECORD_AUDIO)) {
            startAudioProcessing();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mProcessing) {
            mAudioProcessor.stop();
            mProcessing = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void requestPermissions() {
        if (!Utils.checkPermission(this, Manifest.permission.RECORD_AUDIO)) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {

                DialogUtils.showPermissionDialog(this, getString(R.string.permission_record_audio), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(TunerActivity.this,
                                new String[]{Manifest.permission.RECORD_AUDIO},
                                PERMISSION_REQUEST_RECORD_AUDIO);
                    }
                });

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        PERMISSION_REQUEST_RECORD_AUDIO);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startAudioProcessing();
                }
                break;
            }

        }
    }

    private void startAudioProcessing() {
        if (mProcessing)
            return;


        mAudioProcessor = new AudioProcessor();
        mAudioProcessor.init();
        mAudioProcessor.setPitchDetectionListener(new AudioProcessor.PitchDetectionListener() {
            @Override
            public void onPitchDetected(final float freq, double avgIntensity) {

                final int index = mTuning.closestPitchIndex(freq);
                final Pitch pitch = mTuning.pitches[index];
                double interval = 1200 * Utils.log2(freq / pitch.frequency); // interval in cents
                final float needlePos = (float) (interval / 100);
                final boolean goodPitch = Math.abs(interval) < 5.0;
                runOnUiThread(new Runnable() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void run() {
                        mTuningView.setSelectedIndex(index, true);
                        mNeedleView.setTickLabel(0.0F, String.format("%.02fHz", pitch.frequency));
                        mNeedleView.animateTip(needlePos);
                        mFrequencyView.setText(String.format("%.02fHz", freq));

                        findViewById(R.id.gonder).setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                String p =  String.format("%.02fHz", freq);//String.valueOf(progress);
                                debug.setText(p);
                                bt.sendMessage(p);
                            }
                        });

                        final View goodPitchView = findViewById(R.id.good_pitch_view);
                        if (goodPitchView != null) {
                            if (goodPitch) {
                                if (goodPitchView.getVisibility() != View.VISIBLE) {
                                    Utils.reveal(goodPitchView);
                                }
                            } else if (goodPitchView.getVisibility() == View.VISIBLE) {
                                Utils.hide(goodPitchView);
                            }
                        }
                    }
                });

                mPitchIndex = index;
                mLastFreq = freq;

            }
        });
        mProcessing = true;
        mExecutor.execute(mAudioProcessor);
    }



    @Override
    protected void onPause() {
        super.onPause();
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setupActivityTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mTuning = Tuning.getTuning(this, Preferences.getString(this, getString(R.string.pref_tuning_key), getString(R.string.standard_tuning_val)));

        mNeedleView = (NeedleView) findViewById(R.id.pitch_needle_view);
        mNeedleView.setTickLabel(-1.0F, "-100c");
        mNeedleView.setTickLabel(0.0F, String.format("%.02fHz", mTuning.pitches[0].frequency));
        mNeedleView.setTickLabel(1.0F, "+100c");

        int primaryTextColor = Utils.getAttrColor(this, android.R.attr.textColorPrimary);

        mTuningView = (TuningView) findViewById(R.id.tuning_view);
        mTuningView.setTuning(mTuning);


        mFrequencyView = (TextView) findViewById(R.id.frequency_view);
        mFrequencyView.setText(String.format("%.02fHz", mTuning.pitches[0].frequency));

        ImageView goodPitchView = (ImageView) findViewById(R.id.good_pitch_view);
        goodPitchView.setColorFilter(primaryTextColor);
        requestPermissions();




        debug = (TextView) findViewById(R.id.textDebug);
        status = (TextView) findViewById(R.id.textStatus);

        findViewById(R.id.restart).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                connectService();
            }
        });




        elevation = (SeekBar) findViewById(R.id.seekBar);
        elevation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d("Seekbar","onStopTrackingTouch ");
                int progress = seekBar.getProgress();
                String p = String.valueOf(progress);
                debug.setText(p);
                bt.sendMessage(p);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d("Seekbar","onStartTrackingTouch ");
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Log.d("Seekbar", "onProgressChanged " + progress);
            }
        });

        bt = new Bluetooth(this, mHandler);
        connectService();

    }




    public void connectService(){
        try {
            status.setText("Connecting...");
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter.isEnabled()) {
                bt.start();
                bt.connectDevice("HC-06");
                Log.d(TAG, "Btservice started - listening");
                status.setText("Connected");
            } else {
                Log.w(TAG, "Btservice started - bluetooth is not enabled");
                status.setText("Bluetooth Not enabled");
            }
        } catch(Exception e){
            Log.e(TAG, "Unable to start bt ",e);
            status.setText("Unable to connect " +e);
        }
    }


    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Bluetooth.MESSAGE_STATE_CHANGE:
                    Log.d(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    break;
                case Bluetooth.MESSAGE_WRITE:
                    Log.d(TAG, "MESSAGE_WRITE ");
                    break;
                case Bluetooth.MESSAGE_READ:
                    Log.d(TAG, "MESSAGE_READ ");
                    break;
                case Bluetooth.MESSAGE_DEVICE_NAME:
                    Log.d(TAG, "MESSAGE_DEVICE_NAME "+msg);
                    break;
                case Bluetooth.MESSAGE_TOAST:
                    Log.d(TAG, "MESSAGE_TOAST "+msg);
                    break;
            }
        }
    };




    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putFloat(STATE_NEEDLE_POS, mNeedleView.getTipPos());
        outState.putInt(STATE_PITCH_INDEX, mPitchIndex);
        outState.putFloat(STATE_LAST_FREQ, mLastFreq);
        super.onSaveInstanceState(outState);
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mNeedleView.setTipPos(savedInstanceState.getFloat(STATE_NEEDLE_POS));
        int pitchIndex = savedInstanceState.getInt(STATE_PITCH_INDEX);
        mNeedleView.setTickLabel(0.0F, String.format("%.02fHz", mTuning.pitches[pitchIndex].frequency));
        mTuningView.setSelectedIndex(pitchIndex);
        mFrequencyView.setText(String.format("%.02fHz", savedInstanceState.getFloat(STATE_LAST_FREQ)));
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                NavUtils.showSettingsActivity(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }


}

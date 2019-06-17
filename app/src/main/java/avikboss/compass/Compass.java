package avikboss.compass;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class Compass extends AppCompatActivity implements SensorEventListener {

    ImageView compass_img;
    TextView txt_compass;
    int mAzimuth;
    private SensorManager mSensorManager;
    private Sensor mRotationV, mAccelerometer, mMagnetometer;
    private ToggleButton recButton;
    boolean haveSensor = false, haveSensor2 = false;
    float[] rMat = new float[9];
    float[] orientation = new float[3];
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private Timer timer = new Timer();
    private DataRecorder recorder = new DataRecorder("CompassData","csv");

    private EditText rate;
    private EditText cell;
    private boolean correctMode = false;
    private ArrayList<String> correctBuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        compass_img = (ImageView) findViewById(R.id.img_compass);
        txt_compass = (TextView) findViewById(R.id.txt_azimuth);
        recButton = (ToggleButton) findViewById(R.id.toggleRec);
        correctBuffer = new ArrayList<>();

        rate = findViewById(R.id.rateField);
        cell = findViewById(R.id.cellField);
        getPermission();

    }

    public void getPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        }
    }

    public void start() {
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) == null) {
            if ((mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) || (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null)) {
                noSensorsAlert();
            } else {
                mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                haveSensor = mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
                haveSensor2 = mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
            }
        } else {
            mRotationV = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            haveSensor = mSensorManager.registerListener(this, mRotationV, SensorManager.SENSOR_DELAY_UI);
        }
    }


    /**
     * Handles the updating of the heading value and compass image.
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rMat, event.values);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(rMat, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(rMat, orientation);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }

        mAzimuth = Math.round(mAzimuth);
        compass_img.setRotation(-mAzimuth);

        String where = "";

        if (mAzimuth >= 350 || mAzimuth <= 10)
            where = "N";
        if (mAzimuth < 350 && mAzimuth > 280)
            where = "NW";
        if (mAzimuth <= 280 && mAzimuth > 260)
            where = "W";
        if (mAzimuth <= 260 && mAzimuth > 190)
            where = "SW";
        if (mAzimuth <= 190 && mAzimuth > 170)
            where = "S";
        if (mAzimuth <= 170 && mAzimuth > 100)
            where = "SE";
        if (mAzimuth <= 100 && mAzimuth > 80)
            where = "E";
        if (mAzimuth <= 80 && mAzimuth > 10)
            where = "NE";

        txt_compass.setText(String.format(Locale.US,"%d° %s", mAzimuth, where));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        start();
    }

    public void noSensorsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Your device doesn't support the Compass.")
                .setCancelable(false)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                });
        alertDialog.show();
    }

    public void stop() {
        if (haveSensor && haveSensor2) {
            mSensorManager.unregisterListener(this, mAccelerometer);
            mSensorManager.unregisterListener(this, mMagnetometer);
        } else {
            if (haveSensor)
                mSensorManager.unregisterListener(this, mRotationV);
        }
    }

    /**
     * Creates a file to store recorded data and writes to that file on a Timer, whose interval is set
     * by the number text field on the UI. Defaults to 10 measurements/sec.
     */
    public void record() {
        recorder.start(getCellNum());
        if (rate.getText().toString().equals("")) {
            rate.setText(getString(R.string.default_rate));
        }

        long period = (long) (1000 / (Float.valueOf(rate.getText().toString())));


        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @SuppressLint("DefaultLocale")
            @Override
            public void run() {
               long datestr = System.currentTimeMillis();
                Log.i("HEADING", Integer.toString(mAzimuth));
                if (!correctMode) {
                    recorder.write(String.format("%d,%s,%d\n",
                            datestr, getCellNum(), mAzimuth));
                } else {
                    correctBuffer.add(String.format("%d,%s,%%d\n", datestr, getCellNum()));
                }
            }
        }, 0, period);
    }

    /**
     * Closes file and stops recording data.
     */
    public void stopRecording() {
        timer.cancel();
        recorder.stop();
    }

    /**
     * Controls correct mode. When in correct mode, the compass will not record measurements. It will
     * instead wait until correct mode is disabled, and then fill in any missed recording times with
     * whatever the current heading is at the time correct mode is disabled. This allows you to
     * adjust the compass if it gets off without creating too many strange patterns in the data.
     *
     */
    public void toggleCorrect(View view) {
        if (correctMode && recButton.isChecked()) {
            for (String line : correctBuffer) {
                recorder.write(String.format(line,mAzimuth));
            }
            correctBuffer = new ArrayList<>();
        }
        correctMode = !correctMode;
    }

    public void toggleRec(View view) {
        if (!recButton.isChecked()) {
            stopRecording();
        } else {
            record();
        }
    }

    @SuppressLint("SetTextI18n")
    public void nextCell(View view) {
        if (cell.getText().toString().equals("") || cell.getText() == null) {
            cell.setText("0");
        }

        cell.setText(Integer.toString(Integer.parseInt(cell.getText().toString())+1));
    }

    @SuppressLint("SetTextI18n")
    public void prevCell(View view) {
        if (cell.getText().toString().equals("") || cell.getText() == null) {
            cell.setText("1");
        }

        cell.setText(Integer.toString(Integer.parseInt(cell.getText().toString())-1));
    }

    public String getCellNum() {
        return this.cell.getText().toString();
    }
}



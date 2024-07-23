package link.linkcompass;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    TextView azimuthTextView;
    ImageView compassImageView;


    private SensorManager sensorManager;

    private Sensor magnetometerSensor;
    private Sensor accelerometerSensor;
    private Sensor gravitySensor;
    private Sensor rotationVectorSensor;

    int azimuth;
    int oldAzimuth = 0;


    Window window;
    WindowManager.LayoutParams layoutParams;

    protected Location mCurrentLocation;
    protected float mDeclination;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        azimuthTextView = findViewById(R.id.azimuthTextView);
        compassImageView = findViewById(R.id.compassFrontImageView);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);


        //keep screen on
        window = getWindow();
        layoutParams = window.getAttributes();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mDeclination = 0;

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (mCurrentLocation == null) {

            mCurrentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (mCurrentLocation == null)
                mCurrentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);


        }

        if (mCurrentLocation != null)
            mDeclination = getDeclination(mCurrentLocation, System.currentTimeMillis());



    }

    int operationMode;

    @Override
    protected void onResume() {
        super.onResume();

        if (rotationVectorSensor != null) {

            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);

            operationMode = 2;

        } else if (gravitySensor != null && magnetometerSensor != null) {

            sensorManager.registerListener(this, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);

            operationMode = 1;

        } else if (magnetometerSensor != null && accelerometerSensor != null) {

            sensorManager.registerListener(this, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

            operationMode = 0;

        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }


    float[] magneticField = new float[3];
    float[] acceleration = new float[3];
    float[] gravity = new float[3];

    float[] rotationMatrix = new float[9];
    float[] inclinationMatrix = new float[9];
    float[] orientation = new float[3];

    float[] rotationVector = new float[5];


    ObjectAnimator rotationAnimation;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(sensorEvent.values, 0, magneticField, 0, sensorEvent.values.length);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(sensorEvent.values, 0, acceleration, 0, sensorEvent.values.length);
                break;
            case Sensor.TYPE_GRAVITY:
                System.arraycopy(sensorEvent.values, 0, gravity, 0, sensorEvent.values.length);
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                System.arraycopy(sensorEvent.values, 0, rotationVector, 0, sensorEvent.values.length);
                break;
        }

        if (operationMode == 0 || operationMode == 1) {

            if (SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, (operationMode == 0) ? acceleration : gravity, magneticField)) {

                float azimuthRad = SensorManager.getOrientation(rotationMatrix, orientation)[0] + mDeclination; //azimuth in radians
                double azimuthDeg = Math.toDegrees(azimuthRad); // azimuth in degrees; value from -180 to 180

                azimuth = ((int) azimuthDeg + 360) % 360; //convert -180/180 to 0/360


            }

        } else {

            // calculate the rotation matrix
            SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);
            // get the azimuth value (orientation[0]) in degree
            azimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rotationMatrix, orientation)[0]) + Math.round(mDeclination) + 360) % 360;

        }

        azimuthTextView.setText(String.valueOf(azimuth) + "°");
        //compassImageView.setRotation(-azimuth); //set orientation without animation

        //set rotation with animation

        float tempAzimuth;
        float tempCurrentAzimuth;

        if (Math.abs(azimuth - oldAzimuth) > 180) {
            if (oldAzimuth < azimuth) {
                tempCurrentAzimuth = oldAzimuth + 360;
                tempAzimuth = azimuth;
            } else {
                tempCurrentAzimuth = oldAzimuth;
                tempAzimuth = azimuth + 360;
            }
            rotationAnimation = ObjectAnimator.ofFloat(compassImageView, "rotation", -tempCurrentAzimuth, -tempAzimuth);
            rotationAnimation.setDuration(250);
            rotationAnimation.start();
        } else {
            rotationAnimation = ObjectAnimator.ofFloat(compassImageView, "rotation", -oldAzimuth, -azimuth);
            rotationAnimation.setDuration(250);
            rotationAnimation.start();
        }
        oldAzimuth = azimuth;


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**************ACTION BAR MENU************************/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.help:

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("Compass");
                alertDialogBuilder.setMessage("This is Compass app, part of the ITAcademy and LinkAcademy Android Development Program.\n\nAuthor: Vladimir Dresevic, Link Group");

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public static float getDeclination(Location location, long timestamp) {
        if (location == null)
            return 0;

        GeomagneticField geoField = new GeomagneticField(
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                (float) location.getAltitude(),
                timestamp
        );
        return geoField.getDeclination();
    }


}

package org.mhacks;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import de.humatic.nmj.*;
import android.view.View;

import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Vector;

class Sink implements NetworkMidiClient {
}

public class Main extends Activity implements SensorEventListener {
    /**
     * Called when the activity is first created.
     */
    Sink target;
    NetworkMidiSystem sys;

    NetworkMidiOutput output;
    SensorManager smgr;
    long lastUpdate;
    private View view;


    final int[] effectValues = {1,7,10,11,64,71,74,91,93};
    final int[] effectBanks = {0xb0,0xb1,0xb2,0xb3,0xb4,0xb5,0xb6,0xb7,0xb8,0xb9,0xba,0xbb,0xbc,0xbd,0xbe,0xbf};

    ToggleButton reportX, reportY, reportZ, reportA, reportL;


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    TextView status, status2;

    private HashMap<Vector<Integer>, byte[]> cache;



    public int sendValue(float val, float clampLow, float clampHigh, int effectBank, int effectValue){
        byte[] vals = new byte[3];
        vals[0] = (byte) effectBanks[effectBank];
        vals[1] = (byte) effectValues[effectValue];
        if ((clampLow<clampHigh && val<clampLow) ||(clampHigh<=clampLow && val>clampLow)){
            //lower than minimum values
            vals[2] = (byte) 0;
        }else if((clampLow<clampHigh && val>clampHigh) || (clampHigh<clampLow && val<clampHigh)){
            //higher than maximum value
            vals[2] = (byte) 127;
        }else {
            vals[2] = (byte) Math.round((val - clampLow) / (clampHigh - clampLow) * 127);
        }
        try{
            Vector<Integer> key = new Vector<Integer>(effectBank, effectValue);
            if(cache.containsKey(key) && Arrays.equals(cache.get(key), vals)){
                return (int)vals[2];
            }
            output.sendMidiOnThread(vals);
            status2.append("Sent values "+vals[0]+"/"+vals[1]+"/"+vals[2]+"\n");
            cache.put(key, vals);
            status2.scrollTo(0, status2.getHeight());
            return (int)vals[2];
        }catch(Exception e){
            status2.append(e.getMessage());
            status2.scrollTo(0,status2.getHeight());
            return 0;
        }
    }

    public boolean sendNote(int note, boolean on){
        byte[] vals = new byte[3];
        if(on){
            vals[0] = (byte) 144;
        }else{
            vals[0] = (byte) 128;
        }
        vals[1] = (byte) note;
        vals[3] = (byte) 64;
        try{
            Vector<Integer> key = new Vector<Integer>(note, on ? 1 : 0);
            if(cache.containsKey(key) && Arrays.equals(cache.get(key), vals)){
                return false;
            }
            output.sendMidiOnThread(vals);
            status2.append("Sent values "+vals[0]+"/"+vals[1]+"/"+vals[2]+"\n");
            cache.put(key, vals);
            status2.scrollTo(0, status2.getHeight());
            return true;
        }catch(Exception e){
            status2.append(e.getMessage());
            status2.scrollTo(0,status2.getHeight());
            return false;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);
        view = findViewById(R.id.textView);
        view.setBackgroundColor(Color.GREEN);

        lastUpdate = System.currentTimeMillis();

        cache = new HashMap<Vector<Integer>, byte[]>();

        smgr = (SensorManager) getSystemService(SENSOR_SERVICE);

        status = (TextView) findViewById(R.id.op);
        status2 = (TextView) findViewById(R.id.stat2);
        status2.setMovementMethod(new ScrollingMovementMethod());
        reportX = (ToggleButton) findViewById(R.id.reportX);
        reportY = (ToggleButton) findViewById(R.id.reportY);
        reportZ = (ToggleButton) findViewById(R.id.reportZ);
        reportA = (ToggleButton) findViewById(R.id.reportA);
        reportL = (ToggleButton) findViewById(R.id.reportL);

        // this goes wherever you setup your button listener:
        ((Button) findViewById(R.id.btn1)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendNote(66, true);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendNote(66, false);
                }
                return true;
            }
        });

        // this goes wherever you setup your button listener:
        ((Button) findViewById(R.id.btn2)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendNote(68, true);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendNote(68, false);
                }
                return true;
            }
        });

        // this goes wherever you setup your button listener:
        ((Button) findViewById(R.id.btn3)).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    sendNote(70, true);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendNote(70, false);
                }
                return true;
            }
        });

    }

    public void onConnect(View view){
        try{
            sys = NetworkMidiSystem.get(this);
        }catch(Exception e){
            status.setText(e.getMessage());
            return;
        }

        String ip1 = ((EditText) findViewById(R.id.ipField1)).getText().toString();
        String ip2 = ((EditText) findViewById(R.id.ipField2)).getText().toString();
        String ip3 = ((EditText) findViewById(R.id.ipField3)).getText().toString();
        String ip4 = ((EditText) findViewById(R.id.ipField4)).getText().toString();
        //35.2.123.178

        //set the protocol and number of channels
        NMJConfig.setNumChannels(1);
        NMJConfig.setMode(0, NMJConfig.RTPA);
        NMJConfig.setPort(0, Integer.parseInt(((EditText) findViewById(R.id.portField)).toString()));
        NMJConfig.setIP(0,ip1+"."+ip2+"."+ip3+"."+ip4);

        //attempt to start up the connection



        status.setText(NMJConfig.getIP(0));
        try {
            output = sys.openOutput(0, new Sink());
            status.append("//"+String.valueOf(NMJConfig.getRTPState(0)));

        }catch(Exception e){
            status.setText(e.getMessage());
        }

        //for
        /*
        NetworkInterface itf;
        try {
            itf = NetworkInterface.getByName("wlan0");

        }catch(SocketException e){
            t.setText(e.toString());
            return;
        }

        t.setText("NetworkInterfaces:");
        t.append("::"+itf.getName());
        */
        //t.append("Done");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                getAccelerometer(event);
                break;
            case Sensor.TYPE_PROXIMITY:
                getProximity(event);
                break;
            case Sensor.TYPE_LIGHT:
                getLight(event);
                break;
        }
    }

    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        // Movement

        float x = values[0];
        float y = values[1];
        float z = values[2];
        if(reportX.isChecked())
            ((ProgressBar) findViewById(R.id.levelX)).setProgress(sendValue(x, -10, 10, 0, 0));
        if(reportY.isChecked())
            ((ProgressBar) findViewById(R.id.levelY)).setProgress(sendValue(y, -10, 10, 0, 1));
        if(reportZ.isChecked())
            ((ProgressBar) findViewById(R.id.levelZ)).setProgress(sendValue(z, -10, 10, 0, 2));


        TextView t = (TextView) findViewById(R.id.textView);
        t.setText("x=" + String.valueOf(x) + "; \n" + "y=" + String.valueOf(y) + "; \nz=" + String.valueOf(z) +
                "\ncolor=" + Integer.toHexString((Math.round((x + 10) / 20 * 255) * 256 * 256 +
                Math.round((y + 10) / 20 * 255) * 256 +
                Math.round((z + 10) / 20 * 255))));

        t.append(String.valueOf(reportX.isChecked())+"::"+String.valueOf(reportY.isChecked())+"::"+String.valueOf(reportZ.isChecked()));
        view.setBackgroundColor(0xFF000000 + Math.round((x + 10) / 20 * 255) * 256 * 256 +
                Math.round((y + 10) / 20 * 255) * 256 +
                Math.round((z + 10) / 20 * 255));

        status.setText("RTP State: "+String.valueOf(NMJConfig.getRTPState(0))+", Connectivity: "+String.valueOf(NMJConfig.getConnectivity(this)));
    }

    private void getProximity(SensorEvent event) {
        float[] value = event.values;
        float a = value[0];
        if (reportA.isChecked()){
            if(a<5){
                sendNote(64, true);
            }else{
                sendNote(64, false);
            }
        }

        TextView s = (TextView) findViewById(R.id.proximity);
        s.setText("proximity: " + String.valueOf(a));
    }

    private void getLight(SensorEvent event) {
        float value[] = event.values;
        float l = value[1];
        //        if (reportL.isChecked())
        TextView s = (TextView) findViewById(R.id.light);
            s.setText("light: " + String.valueOf(l));
        }

    @Override
    protected void onResume() {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        smgr.registerListener(this,
                smgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        smgr.registerListener(this,
                smgr.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                SensorManager.SENSOR_DELAY_NORMAL);
        smgr.registerListener(this,
                smgr.getDefaultSensor(Sensor.TYPE_LIGHT),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        smgr.unregisterListener(this);
    }
}




package org.mhacks;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import de.humatic.nmj.*;
import android.view.View;
import android.widget.ToggleButton;

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

public class Main extends Activity implements SensorEventListener, NetworkMidiListener {
    /**
     * Called when the activity is first created.
     */
    Sink target;
    NetworkMidiSystem sys;

    NetworkMidiOutput output;
    SensorManager smgr;
    long lastUpdate;
    private View view;
    private MidiLogger midiLogger;

    final int[] effectValues = {1,7,10,11,64,71,74,91,93};
    final int[] effectBanks = {0xb0,0xb1,0xb2,0xb3,0xb4,0xb5,0xb6,0xb7,0xb8,0xb9,0xba,0xbb,0xbc,0xbd,0xbe,0xbf};

    ToggleButton reportX, reportY, reportZ;


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    TextView status, status2;

    private HashMap<Vector<Integer>, byte[]> cache;



    public void sendValue(float val, float clampLow, float clampHigh, int effectBank, int effectValue){
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
                return;
            }
            output.sendMidiOnThread(vals);
            status2.append("Sent values "+vals[0]+"/"+vals[1]+"/"+vals[2]+"\n");
            cache.put(key, vals);
            status2.scrollTo(0, status2.getHeight());
        }catch(Exception e){
            status2.append(e.getMessage());
            status2.scrollTo(0,status2.getHeight());
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
        try{
            sys = NetworkMidiSystem.get(this);
        }catch(Exception e){
            status.setText(e.getMessage());
            return;
        }


        //set the protocol and number of channels
        NMJConfig.setNumChannels(1);
        NMJConfig.setMode(0, NMJConfig.RTPA);
        NMJConfig.setPort(0, 5004);
        NMJConfig.setIP(0,"35.2.123.178");

        //attempt to start up the connection



        status.setText(NMJConfig.getIP(0));
        try {
            output = sys.openOutput(0, new Sink());
            status.append("//"+String.valueOf(NMJConfig.getRTPState(0)));

        }catch(Exception e){
            status.setText(e.getMessage());
        }

        midiLogger = new MidiLogger();

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
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);
        }

    }

    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        // Movement

        float x = values[0];
        float y = values[1];
        float z = values[2];
        if(reportX.isChecked())
        sendValue(x, -10, 10, 0, 0);
        if(reportY.isChecked())
        sendValue(y, -10, 10, 0, 1);
        if(reportZ.isChecked())
        sendValue(z, -10, 10, 0, 2);


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
    @Override
    protected void onResume() {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        smgr.registerListener(this,
                smgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        smgr.unregisterListener(this);
    }

    @Override
    public void midiReceived(int channel, int ssrc, byte[] data, long timestamp) {
        /**
         * As MIDI does not come in on the UI thread, it needs to be off-loaded in
         * order to be displayed. Android's Handler class is one way to do this.
         * Note: If you need to work with large numbers of simultaneous MIDI events
         * or high rate streams, consider copying the incoming data into your own
         * arrays before sending them via Handlers. nmj will internally reuse memory
         * and you may risk having data overwritten before you see it appearing in
         * Handler.handleMessage() otherwise.
         */
        Message msg = Message.obtain();
        Bundle b = new Bundle();
        b.putByteArray("MIDI", data);
        b.putInt("CH", channel);
        msg.setData(b);

        midiLogger.sendMessage(msg);

    }

    private class MidiLogger extends android.os.Handler {

        private StringBuffer sb = new StringBuffer();
        private TextView tv;

        private MidiLogger(TextView tv) {
            super();
            this.tv = tv;
        }

        public void handleMessage(android.os.Message msg) {
            Bundle b = msg.getData();
            sb.delete(0, sb.length());
            byte[] data = b.getByteArray("MIDI");

            sb.append("MIDI received: ");
            for (int i = 0; i < data.length; i++) sb.append((data[i] & 0xFF)+" ");
            sb.append("\n");
            status2.append(sb.toString()+"\n");
        }
    }

}




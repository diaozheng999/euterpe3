package org.mhacks;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import de.humatic.nmj.*;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

class Sink implements NetworkMidiClient {
}

public class Main extends Activity {
    /**
     * Called when the activity is first created.
     */
    Sink target;
    NetworkMidiSystem sys;

    NetworkMidiOutput output;

    final int[] effectValues = {1,7,10,11,64,100,101,121,123};
    final int[] effectBanks = {0xb0,0xb1,0xb2,0xb3,0xb4,0xb5,0xb6,0xb7,0xb8,0xb9,0xba,0xbb,0xbc,0xbd,0xbe,0xbf};

    TextView status;


    public void sendValue(float val, float clampLow, float clampHigh, int effectBank, int effectValue){
        byte[] vals = new byte[3];
        vals[0] = (byte) effectBanks[effectBank];
        vals[1] = (byte) effectValues[effectValue];
        if ((clampLow<clampHigh && val<clampLow) || val>clampLow){
            //lower than minimum values
            vals[2] = (byte) 0;
        }else if((clampLow<clampHigh && val>clampHigh) || val<clampHigh){
            //higher than maximum value
            vals[2] = (byte) 127;
        }else {
            vals[2] = (byte) Math.round((val - clampLow) / (clampHigh - clampLow) * 127);
        }
        try{
            output.sendMidiOnThread(vals);
        }catch(Exception e){
            status.setText(e.getMessage());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        status = (TextView) findViewById(R.id.op);
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

}



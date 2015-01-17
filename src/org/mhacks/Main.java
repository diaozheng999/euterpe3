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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        TextView t = (TextView) findViewById(R.id.op);
        try{
            sys = NetworkMidiSystem.get(this);
        }catch(Exception e){
            t.setText(e.getMessage());
            return;
        }


        //set the protocol and number of channels
        NMJConfig.setNumChannels(1);
        NMJConfig.setMode(0, NMJConfig.RTPA);
        NMJConfig.setPort(0, 5004);
        NMJConfig.setIP(0,"35.2.123.178");

        //attempt to start up the connection



        t.setText(NMJConfig.getIP(0));
        try {
            output = sys.openOutput(0, new Sink());
            t.append("//"+String.valueOf(NMJConfig.getRTPState(0)));

        }catch(Exception e){
            t.setText(e.getMessage());
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

package edu.ucr.ece.btdatacomm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import edu.ucr.ece.btdatacomm.R;

public class Advertise extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advertise);

        Thread timer = new Thread(){
            public void run(){
                try {
                    //wait 2 seconds
                    sleep(2000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }finally{
                    //start MainActivity
                    Intent openLogin = new Intent("android.intent.action.LOGIN");
                    startActivity(openLogin);
                }
            }
        };

        timer.start();
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        finish();
    }
}

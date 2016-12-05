package edu.ucr.ece.btdatacomm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

public class MainPage extends Activity {
    private static final String TAG = "BT Data Comm";

    Button btnLock,btnTemperature,btnSecurity;
    TextView lockState,Temperature,WeighingScale,securityState;
    Handler h;

    final int RECEIVE_MESSAGE = 1;        // Status  for Handler
    final int RECEIVE_FAIL = 2;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();

    private ConnectedThread mConnectedThread;
    MediaPlayer mp;
    boolean CommBegin = false;

    // SPP UUID service. DO NOT CHANGE!!! This is a Standard SerialPortService ID per
    // https://developer.android.com/reference/android/bluetooth/BluetoothDevice.html#createRfcommSocketToServiceRecord%28java.util.UUID%29
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Adafruit Bluefruit EZ-Link module (you must edit this line)
    private static String First32bit = "98:76:B6:00";
    private static String Last16bit = "9C:23";
    private static String address = First32bit+":"+Last16bit;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    long pre = System.currentTimeMillis();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        initVars();
        mp = MediaPlayer.create(MainPage.this,R.raw.getup);

        h = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case RECEIVE_MESSAGE:                                            // if receive message
                        if((System.currentTimeMillis()-pre) > 2000) {
                            mConnectedThread.write("2");
                            pre = System.currentTimeMillis();
                        }
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);            // create string from bytes array
                        sb.append(strIncom);                                        // append string
                        int endOfLineIndex = sb.indexOf("#");                    // determine the end-of-line
                        if (endOfLineIndex > 0) {                                    // if end-of-line,
                            String sbprint = sb.substring(0, endOfLineIndex);        // extract string
                            System.out.println(sbprint);
                            String[] parts = sbprint.split(",");
                            if (parts.length < 4)  break;
                            sb.delete(0, sb.length());                           // and clear
                            WeighingScale.setText(parts[1]);
                            if(HomeConfig.TEMPERATURE_SCALE){
                                Temperature.setText(parts[2]);
                            }else{
                                try {
                                    Temperature.setText(parts[3]);
                                }catch(Exception e){
                                    System.out.println("Cannot receive complete data!");
                                }
                            }
                            /*txtDataArduino.setText(parts[1]); // update TextView
                            txtDataArduino2.setText(parts[0]);
                            btnOff.setEnabled(true);
                            btnOn.setEnabled(true);*/
                        }
                        //Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                        break;
                    case RECEIVE_FAIL:
                        if (CommBegin == true) {
                            HomeConfig.SECURITY_STATUE = false;
                            securityState.setText("BT LOST");
                            btnSecurity.setBackgroundResource(R.drawable.warn);
                            mp.start();
                        }
                        break;
                }
            }

        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();        // get Bluetooth adapter
        checkBTState();

        btnLock.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                btnLock.setEnabled(true);
                //Toast.makeText(getBaseContext(), "Turn LED ON: Sending Char 1 ", Toast.LENGTH_SHORT).show();

		/*门禁控制的实现*/
                if (HomeConfig.LOCK_STATUE) {
                    mConnectedThread.write("0");
                    HomeConfig.LOCK_STATUE = false;
                    lockState.setText("UNLOCKED");
                    btnLock.setBackgroundResource(R.drawable.lock_off);
                } else {
                    mConnectedThread.write("1");
                    HomeConfig.LOCK_STATUE = true;
                    lockState.setText("LOCKED");
                    btnLock.setBackgroundResource(R.drawable.lock_on);
                }
            }
        });

        btnSecurity.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                btnSecurity.setEnabled(true);
                //Toast.makeText(getBaseContext(), "Turn LED ON: Sending Char 1 ", Toast.LENGTH_SHORT).show();

		/*门禁控制的实现*/
                if (HomeConfig.SECURITY_STATUE == false) {
                    checkBTState();
                    Log.d(TAG, "onResume(): Creating bluetooth socket ...");

                    // Set up a pointer to the remote node using it's address.
                    BluetoothDevice device = btAdapter.getRemoteDevice(address);

                    // Two things are needed to make a connection:
                    //   A MAC address, which we got above.
                    //   A Service ID or UUID.  In this case we are using the
                    //     UUID for SPP.

                    try {
                        btSocket = createBluetoothSocket(device);
                        Log.d(TAG, "onResume(): Bluetooth socket created ...");
                    } catch (IOException e) {
                        errorExit("Fatal Error", "onResume(): Create bluetooth socket FAILED: " + e.getMessage() + ".");
                    }
                    Toast.makeText(MainPage.this, "Bluetooth socket created ...", Toast.LENGTH_SHORT).show();
                    // Discovery is resource intensive.  Make sure it isn't going on
                    // when you attempt to connect and pass your message.
                    btAdapter.cancelDiscovery();

                    Log.d(TAG, "Connecting to Bluetooth Device ...");
                    Toast.makeText(MainPage.this, "Connecting to Bluetooth Device ...", Toast.LENGTH_SHORT).show();
                    try {
                        btSocket.connect();
                        CommBegin = true;
                        Log.d(TAG, "Bluetooth Device Connected ...");
                        Toast.makeText(MainPage.this, "Bluetooth Device Connected ...", Toast.LENGTH_SHORT).show();
                        HomeConfig.SECURITY_STATUE = true;
                        securityState.setText("SAFE NOW");
                        btnSecurity.setBackgroundResource(R.drawable.safe);
                    } catch (IOException e1) {
                        System.out.println("cannot connect to ardunio: "+e1);
                        e1.printStackTrace();
                        try {
                            btSocket.close();
                        } catch (IOException e2) {
                            errorExit("Fatal Error", "Unable to close socket when closing connection: " + e2.getMessage() + ".");
                        }
                    }
                    // Create a data stream so we can talk to server.
                    Log.d(TAG, "onResume(): Creating data output stream ...");
                    Toast.makeText(MainPage.this, "Creating data output stream ...", Toast.LENGTH_SHORT).show();
                    mConnectedThread = new ConnectedThread(btSocket);
                    mConnectedThread.start();
                }
            }
        });

        btnTemperature.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                btnTemperature.setEnabled(true);
		/*temperature控制的实现*/
                if(HomeConfig.TEMPERATURE_SCALE){
                    HomeConfig.TEMPERATURE_SCALE = false;
                }else{
                    HomeConfig.TEMPERATURE_SCALE = true;
                }
            }
        });

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    public void onIVConfig(View v){

        /**
         * 在实际开发中LayoutInflater这个类还是非常有用的，
         * 它的作用类似于findViewById()。
         * 不同点是LayoutInflater是用来找res/layout/下的xml布局文件，并且实例化；
         * 而findViewById()是找xml布局文件下的具体widget控件(如 Button、TextView等)。
         * 具体作用：
         * 1、对于一个没有被载入或者想要动态载入的界面，都需要使用LayoutInflater.inflate()来载入；
         * 2、对于一个已经载入的界面，就可以使用Activiyt.findViewById()方法来获得其中的界面元素。
         */
        LayoutInflater factory = LayoutInflater.from(MainPage.this);
        final View v1=factory.inflate(R.layout.child_config,null);
        AlertDialog.Builder dialog=new AlertDialog.Builder(MainPage.this);

        dialog.setTitle("Bluetooth Configuration");
        dialog.setView(v1);
        final EditText editTextIp = (EditText)v1.findViewById(R.id.connectionurl);
        final EditText editTextPort = (EditText)v1.findViewById(R.id.controlurl);
        editTextIp.setText(First32bit);		//初始值
        editTextPort.setText(Last16bit);	//初始值

        dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                First32bit   = editTextIp.getText().toString();
                Last16bit = editTextPort.getText().toString();
                Toast.makeText(MainPage.this, "Configurate Successfully！", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.setNegativeButton("CANCEL",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        dialog.show();
    }

    public void onIVExit(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainPage.this);
        builder.setMessage("Do you want to exit?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                        finish();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog

                    }
                }).show();		//finish();
    }

    public void initVars(){


        lockState = (TextView)findViewById(R.id.tvLockState);
        btnLock = (Button) findViewById(R.id.btnLock);
        Temperature = (TextView)findViewById(R.id.tvTemperature);
        btnTemperature = (Button) findViewById(R.id.btnTemperature);
        WeighingScale = (TextView)findViewById(R.id.tvWeighingScale);
        securityState= (TextView)findViewById(R.id.tvSecurity);
        btnSecurity = (Button)findViewById(R.id.btnSecurity);


        if(HomeConfig.LOCK_STATUE){
            lockState.setText("LOCKED");
            btnLock.setBackgroundResource(R.drawable.lock_on);
        }else{
            lockState.setText("UNLOCKED");
            btnLock.setBackgroundResource(R.drawable.lock_off);
        }

        if(HomeConfig.TEMPERATURE_SCALE){
            Temperature.setText("C");
        }else{
            Temperature.setText("F");
        }

        if(HomeConfig.SECURITY_STATUE){
            securityState.setText("SAFE NOW");
            btnSecurity.setBackgroundResource(R.drawable.safe);
        }else{
            securityState.setText("BT LOST");
            btnSecurity.setBackgroundResource(R.drawable.warn);
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord",
                        new Class[]{UUID.class});
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e);
            }
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume(): Creating bluetooth socket ...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
            Log.d(TAG, "onResume(): Bluetooth socket created ...");
        } catch (IOException e) {
            errorExit("Fatal Error", "onResume(): Create bluetooth socket FAILED: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "Connecting to Bluetooth Device ...");
        try {
            btSocket.connect();
            CommBegin = true;
            Log.d(TAG, "Bluetooth Device Connected ...");
            HomeConfig.SECURITY_STATUE = true;
            securityState.setText("SAFE NOW");
            btnSecurity.setBackgroundResource(R.drawable.safe);
        } catch (IOException e) {
            try {
                btSocket.close();
                CommBegin = false;
            } catch (IOException e2) {
                errorExit("Fatal Error", "onResume(): Unable to close socket when closing connection: " + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "onResume(): Creating data output stream ...");

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "Inside onPause()...");

        /*try {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "onPause(): FAILED to close socket." + e2.getMessage() + ".");
        }*/
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth not supported");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "Bluetooth is ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message) {
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://edu.ucr.ece.btdatacomm/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://edu.ucr.ece.btdatacomm/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()
            final Message msg = new Message();
            //final Bundle b = new Bundle();


            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                    h.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();        // Send to message queue Handler
                } catch (IOException e) {
                    long s = System.currentTimeMillis();
                    boolean btLost = true;
                    while((System.currentTimeMillis()-s)<5000){
                        Log.d(TAG, "Connecting to Bluetooth Device ...");
                        try {
                            btSocket.connect();
                            CommBegin = true;
                            Log.d(TAG, "Bluetooth Device Connected ...");
                            HomeConfig.SECURITY_STATUE = true;
                            securityState.setText("SAFE NOW");
                            btnSecurity.setBackgroundResource(R.drawable.safe);
                        } catch (IOException e1) {
                            try {
                                btSocket.close();
                            } catch (IOException e2) {
                                errorExit("Fatal Error", "Unable to close socket when closing connection: " + e2.getMessage() + ".");
                            }
                        }
                        try {
                            // Read from the InputStream
                            bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                            h.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();        // Send to message queue Handler
                            btLost = false;
                            break;
                        } catch (IOException e1) {


                        }
                    }
                    if (btLost) {
                        Log.d(TAG,"The apparent point of crash");
                        msg.what=RECEIVE_FAIL;
                        h.sendMessage(msg);
                        Log.d(TAG,"After cash");
                        break;
                    }
                    else{
                        continue;
                    }
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "Info: ConnectedThread: data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "Error: ConnectedThread: data to send: " + e.getMessage() + "...");
            }
        }
    }
}
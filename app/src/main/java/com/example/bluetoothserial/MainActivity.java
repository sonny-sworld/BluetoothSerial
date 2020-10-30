package com.example.bluetoothserial;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity
{
    TextView myLabel;
    EditText myTextbox;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button openButton = (Button)findViewById(R.id.open);
        Button sendButton = (Button)findViewById(R.id.send);
        Button closeButton = (Button)findViewById(R.id.close);
        myLabel = (TextView)findViewById(R.id.label);
        myTextbox = (EditText)findViewById(R.id.entry);

        //Open Button
        openButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                    findBT();
                    openBT();
//                Intent intent = new Intent(MainActivity.this, MicrochipIntentService.class);
//                startService(intent);

            }
        });

        //Send Button
        sendButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    sendData();
                }
                catch (IOException ex) { }
            }
        });

        //Close button
        closeButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                try
                {
                    closeBT();
                }
                catch (IOException ex) { }
            }
        });
    }

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            myLabel.setText("No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                String name = device.getName();
                if(name.equals("BT024"))
                {
                    mmDevice = device;
                    myLabel.setText("Found device" + name);
                    return;
                }
            }
        }
        myLabel.setText("Bluetooth Device Found");
    }

    void openBT()
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);

        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        } catch (IOException e) {
            try {
                mmSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            openBT();
            return;
        }
        beginListenForData();

        myLabel.setText("Bluetooth Opened");
    }
    Handler handler = new Handler();

    ScheduledExecutorService executorService;
    void beginListenForData() {
        stopWorker = false;
        executorService = new ScheduledThreadPoolExecutor(1, new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build());
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                 SayHello();
            }
        }, 2, 3, TimeUnit.SECONDS);
    }

    private void  SayHello() {
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        String data = null;
        try {
            mmInputStream = mmSocket.getInputStream();
            int bytesAvailable = mmInputStream.available();
            if (bytesAvailable > 0) {
                byte[] packetBytes = new byte[bytesAvailable];
                mmInputStream.read(packetBytes);
                data = new String(packetBytes, StandardCharsets.UTF_8);

            }
            final String finalData = data;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (!TextUtils.isEmpty(finalData)) {
                        myLabel.setText(finalData);
                    }
                }
            });
        } catch (IOException ex) {
            stopWorker = true;
        }
    }

    void sendData() throws IOException
    {
        String msg = myTextbox.getText().toString();
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        myLabel.setText("Data Sent");
    }

    void closeBT() throws IOException {
        stopWorker = true;
        if (mmOutputStream != null) {
            mmOutputStream.close();
        }
        if (mmInputStream != null) {
            mmInputStream.close();
        }
        if (mmSocket != null) {
            mmSocket.close();
        }
        myLabel.setText("Bluetooth Closed");
        if(executorService!=null) {
            executorService.shutdown();
        }
        mmInputStream = null;
        mmSocket = null;
        mBluetoothAdapter = null;
        mmOutputStream = null;
    }
}
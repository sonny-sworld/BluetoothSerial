package com.example.bluetoothserial;

import android.app.ActivityManager;
import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static helper methods.
 */
public class MicrochipIntentService extends IntentService {
	
	private BluetoothChatService btChat;
	private static String TAG = "MicrochipIntentService";

	public MicrochipIntentService() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			final String action = intent.getAction();
			btChat = setupMicrochipScanner();
			if(btChat!=null){
				Toast.makeText(this.getApplicationContext(), "Connected", Toast.LENGTH_LONG).show();
			}
		}
	}

	private BluetoothChatService setupMicrochipScanner(){
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		    // Device does not support Bluetooth 
		} 
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		BluetoothDevice btDevice = null;
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice d : pairedDevices) {
				if(d.getName().equalsIgnoreCase("BT024")){//only connect to printers
					btDevice = d;
				}
			}
		}
		// create a BT Service object
		if(btDevice != null){
			BluetoothChatService BTService = new BluetoothChatService(this);
			// Start the BT Service
			BTService.start();
			BTService.connect(btDevice);
			
			// give 8 seconds to connect
			int nWaitTime = 8;
			while (BTService.getState() != BluetoothChatService.STATE_CONNECTED) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				nWaitTime--;
				if (nWaitTime == 0) {
					// "Scanner timed out";
				}
			}
			return BTService;
			
		}
		return null;
	}

	String data;
	public void receiveMicrochip(byte[] buffer) {
		data = new String(buffer, StandardCharsets.UTF_8);

		Log.d(TAG ,"Microchip: "+data);
		//Check if in animal ticket screen
		ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
	    List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
	    ComponentName componentInfo = taskInfo.get(0).topActivity;
	    Log.d(TAG,"componentInfo package name: "+ componentInfo.getPackageName()+" class name: "+ componentInfo.getClassName());
//	    if(componentInfo.getClassName().equals(IssueTicketActivity.class.getName())){
//		    Intent intent = new Intent();
//		    intent.setAction(Constants.ACTION_MICROCHIP);
//			intent.putExtra(Constants.MICROCHIP_TAG, new String(buffer));
//			sendBroadcast(intent);
//	    }else{
//		    Intent intent = new Intent(this,RTLActivity.class);
//			intent.putExtra(Constants.MICROCHIP_TAG, new String(buffer));
//			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			startActivity(intent);
//	    }
	}
	
	@Override
	public void onDestroy(){
		if(btChat != null){
			Log.d(TAG, this.stopService(new Intent(this, BluetoothChatService.class)) + " stop");
		}
		super.onDestroy();
	}
	
}

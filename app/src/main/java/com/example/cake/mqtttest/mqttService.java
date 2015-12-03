package com.example.cake.mqtttest;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;


import android.annotation.SuppressLint;
import android.content.IntentFilter;
import android.content.res.Configuration;


import android.os.Binder;
import android.os.Build;
import android.os.Handler;


import com.google.android.gms.maps.model.LatLng;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class mqttService extends Service {

    public String requestTopic = "cli/";
    public String removeCustomer = "removeContomer/";
    public String taxiID ="";
    public String gCustomerResponseTopic = "customerResponse/";
    public String gTaxiResponseTopic = "taxiResponse/";

    public String reuestedCustomer = "";
    public String nationality = "";
    public String customerMessage = "";

    public LatLng currentLocation;

    private CountDownTimer waitTimer;

    public Map<String, String> requestedMessage = new HashMap<String, String>();

    private static final String TAG = "MQTTService";
    private static boolean hasWifi = false;
    private static boolean hasMmobile = false;
    private Thread thread;
    private ConnectivityManager mConnMan;
    private volatile IMqttAsyncClient mqttClient;
    public String deviceId;
    private final IBinder mBinder = new mqttBinder();
    public static final String NOTIFICATION = "com.cake.android.service.receiver";
    public static final String RESULT = "result";
    public static final String TOPIC = "topic";

    private Boolean networkOK = false;

    public JSONObject taxiData;

    boolean mAllowRebind; // indicates whether onRebind should be used


    class MQTTBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            IMqttToken token;
            boolean hasConnectivity = false;
            boolean hasChanged = false;
            NetworkInfo infos[] = mConnMan.getAllNetworkInfo();

            for (int i = 0; i < infos.length; i++) {
                if (infos[i].getTypeName().equalsIgnoreCase("MOBILE")) {
                    if ((infos[i].isConnected() != hasMmobile)) {
                        hasChanged = true;
                        hasMmobile = infos[i].isConnected();
                    }
                    Log.d(TAG, infos[i].getTypeName() + " is " + infos[i].isConnected());
                } else if (infos[i].getTypeName().equalsIgnoreCase("WIFI")) {
                    if ((infos[i].isConnected() != hasWifi)) {
                        hasChanged = true;
                        hasWifi = infos[i].isConnected();
                    }
                    Log.d(TAG, infos[i].getTypeName() + " is " + infos[i].isConnected());
                }
            }

            hasConnectivity = hasMmobile || hasWifi;
            Log.v(TAG, "hasConn: " + hasConnectivity + " hasChange: " + hasChanged + " - " + (mqttClient == null || !mqttClient.isConnected()));
            if (hasConnectivity && hasChanged && (mqttClient == null || !mqttClient.isConnected())) {
                //doConnect();
                networkOK = true;
            } else if (!hasConnectivity && mqttClient != null && mqttClient.isConnected()) {
                Log.d(TAG, "doDisconnect()");
                try {
                    token = mqttClient.disconnect();
                    token.waitForCompletion(1000);
                } catch (MqttException e) {
                    e.printStackTrace();

                }
            }
        }
    }



    public class mqttBinder extends Binder {
        public mqttService getService() {
            return mqttService.this;
        }
    }

    @Override
    public void onCreate() {
        IntentFilter intentf = new IntentFilter();
        setClientID();
        intentf.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(new MQTTBroadcastReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mConnMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        waitTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {

                doConnect();

            }


        };
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged()");
        android.os.Debug.waitForDebugger();
        super.onConfigurationChanged(newConfig);

    }

    private void setClientID() {
//        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//        WifiInfo wInfo = wifiManager.getConnectionInfo();
        //deviceId = "aaaaaaa";
        if (deviceId == null) {
            deviceId = MqttAsyncClient.generateClientId();
        }
    }

    public void publish(String topic, String message) {


        MqttMessage message1 = new MqttMessage();
        message1.setPayload(message.getBytes());
        message1.setQos(1);
        if(mqttClient != null) {
            try {
                mqttClient.publish(topic, message1);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }


    }

    public void subscribe(String topic) {
        IMqttToken token;

        try {
            token = mqttClient.subscribe(topic, 1);
            token.waitForCompletion(5000);
        } catch (MqttException e) {
            e.printStackTrace();
        }


    }

    public  void unsubscribe(String topic){
        IMqttToken token;

        try {
            token = mqttClient.unsubscribe(topic);
            token.waitForCompletion(5000);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void  removeFromManager(){
        String topic = "removeTaxi/" + taxiID;
        JSONObject data = new JSONObject();
        try {
            data.put("id",taxiID);


            publish(topic, data.toString());


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void connect(){
        doConnect();

    }

    private void doConnect(){
        Log.d(TAG, "doConnect()");
        IMqttToken token;
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setKeepAliveInterval(20*60);
        String tt = "taxiDriver/will";
        options.setWill(tt, ("id"+taxiID).getBytes(),2, false);
        try {
            // mqttClient = new MqttAsyncClient("tcp://192.168.100.2:1883", deviceId, new MemoryPersistence());
            mqttClient = new MqttAsyncClient("tcp://128.199.97.22:1883", deviceId, new MemoryPersistence());
            token = mqttClient.connect();
            token.waitForCompletion(3500);
            mqttClient.setCallback(new MqttEventCallback());


            token = mqttClient.subscribe(requestTopic+taxiID, 0);
            token.waitForCompletion(5000);
            token = mqttClient.subscribe(removeCustomer+taxiID, 0);
            Log.i("MQTT","Remove custommer ID = " + removeCustomer+taxiID);
            token.waitForCompletion(5000);
            token = mqttClient.subscribe("testtopic", 0);
            token.waitForCompletion(5000);



        } catch (MqttSecurityException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            waitTimer.start();
            switch (e.getReasonCode()) {
                case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
                case MqttException.REASON_CODE_CLIENT_TIMEOUT:
                case MqttException.REASON_CODE_CONNECTION_LOST:
                case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
                    Log.v(TAG, "c" +e.getMessage());
                    e.printStackTrace();
                    break;
                case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
                    Intent i = new Intent("RAISEALLARM");
                    i.putExtra("ALLARM", e);
                    Log.e(TAG, "b"+ e.getMessage());
                    break;
                default:
                    Log.e(TAG, "a" + e.getMessage());
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand()");
        return START_STICKY;
    }

    private class MqttEventCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable arg0) {
            Log.i("MQTT_CONNECTION_LOST", "LOST MQTT");
            //connect();
            waitTimer.start();

        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {

        }

        @Override
        @SuppressLint("NewApi")
        public void messageArrived(String topic, final MqttMessage msg) throws Exception {
            Log.i(TAG, "Message arrived from topic = " + topic);

            String payload =  msg.toString();
            if(topic.equals("testtopic")){
                Log.i(TAG, "Payload = " + payload);
                JSONObject jsonObj = new JSONObject(payload);
                Log.i(TAG, "ID = " + jsonObj.get("id").toString());

                publishResults(topic,payload);
            } else if (topic.equals("testtopic2")) {
                Log.i(TAG, "Payload = " + payload);
                publishResults(topic, payload);
            } else if (topic.equals(requestTopic+taxiID)){
                JSONObject jsonObj = new JSONObject(payload);
                requestedMessage.put(jsonObj.get("id").toString(), payload);
                for (Map.Entry entry : requestedMessage.entrySet()) {
                    Log.i("MQTT","Requested message ID = " + entry.getKey().toString());
                }
                publishResults(topic, payload);
            } else if (topic.equals(removeCustomer+taxiID)){
                requestedMessage.remove(payload);
                Log.i("MQTT", "a custommer is removed");
                for (Map.Entry entry : requestedMessage.entrySet()) {
                    Log.i("MQTT","Remain custommers = " + entry.getKey().toString());
                }
                publishResults(topic, payload);
            } else if (topic.equals(gCustomerResponseTopic + reuestedCustomer)){
                //This topic will be subscribed on demand
                customerMessage = payload;
                publishResults(topic,payload);
            }

            Handler h = new Handler(getMainLooper());
            h.post(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    }


    private void publishResults(String topic, String message) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(RESULT, message);
        intent.putExtra(TOPIC, topic);
        sendBroadcast(intent);
    }

    public String getThread(){
        return Long.valueOf(thread.getId()).toString();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind called");
        //return mMessenger.getBinder();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        Log.i(TAG, "Service un-binded");
        return mAllowRebind;
    }

    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with Re-bindService(),
        // after onUnbind() has already been called
        Log.i(TAG, "Service re-binded");
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        Log.i(TAG, "Service destroyed");

    }



}




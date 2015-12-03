package com.example.cake.mqtttest;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.app.AlertDialog;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;


public class MapsActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {


    public static final String TAG = MapsActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;







    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private mqttService mService;
    private boolean mBound = false;
    GoogleMap googleMap;
    private LatLng curentLocation;


    private CountDownTimer waitTimer;

    //Set up a receiver for image donwloading
    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            final globalVar gv = ((globalVar)getApplicationContext());

            Log.i("DOWNLOAD","DONE");

            MLRoundedImageView cc = (MLRoundedImageView) findViewById(R.id.custommerImageView);
            if(gv.driverImage != null) {
                cc.setImageBitmap(gv.driverImage);
            } else {

            }


        }
    };
    //MARK:****************************  MQTT Part ******************************************
    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Log.i("MQTT Map","Messagge from service in Map");
                String resultCode = bundle.getString(mqttService.RESULT);
                if(bundle.getString(mqttService.TOPIC).equals(mService.requestTopic+mService.taxiID)){
                    try {
                        JSONObject obj = new JSONObject(resultCode);
                        if(obj.get("requestFlag").equals("1")){
                            Log.i("MQTT map", "****** Custommer requested ******");
                            mService.reuestedCustomer = obj.get("id").toString();
                            mService.nationality = obj.get("nationality").toString();
                            String currentTime = obj.get("currentTime").toString();
                            Log.i("MQTT map", currentTime);
                            Calendar c = Calendar.getInstance();
                            // year, month, day, hourOfDay, minute
                            c.set(2001, 0, 1, 12, 0);
                            long millis = System.currentTimeMillis();

                            double l = Double.valueOf(currentTime);


                            long diff = millis - (new Double(l)).longValue();
                            Log.i("SSSSS", Long.toString(diff));
                            Log.i("SSSSS", Long.toString(millis));

                            Log.i("SSSSS", Long.toString((new Double(l)).longValue()));


                            showAlertView(obj.get("fromAddress").toString(), obj.get("toAddress").toString(), 0);



                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Log.i("MQTT Map",resultCode);
                }


            }
        }
    };

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {
        // @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                // We've bound to LocalService, cast the IBinder and get LocalService instance.
                mqttService.mqttBinder binder = (mqttService.mqttBinder) service;
                mService = binder.getService();
                mBound = true;

                mService.publish("test/tt", "Hello world from map");



            } catch (ClassCastException e) {
                // Pass
            }
        }

        // @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    //************************** End MQTT part *****************************

    private void showAlertView(String fromAddress, String toAddress, long diff){

        String mm = "To";
        String ff = "From";

        final AlertDialog acc = new AlertDialog.Builder(this)
                .setTitle("REQUEST (60)")
                .setMessage(ff + "\n" + fromAddress + "\n" + mm + "\n" + toAddress)
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {


                        JSONObject data = new JSONObject();
                        try {
                            data.put("id", mService.taxiID);
                            data.put("type", "ack");
                            data.put("value", "REJECT");

                            String topic = mService.gTaxiResponseTopic + mService.taxiID;

                            mService.publish(topic, data.toString());

                            //TODO Cancel timer
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                })
                .setNegativeButton("Accept", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Do nothing
                        waitTimer.cancel();
                        JSONObject data = new JSONObject();
                        try {
                            data.put("id", mService.taxiID);
                            data.put("type", "ack");
                            data.put("value", "OK");
                            data.put("lat", String.valueOf(curentLocation.latitude));
                            data.put("lon", String.valueOf(curentLocation.longitude));

                            String topic = mService.gTaxiResponseTopic + mService.taxiID;

                            mService.publish(topic, data.toString());

                            //Change status to unavailable

                            JSONObject locDetail = new JSONObject();
                            try {
                                locDetail.put("id", mService.taxiID);
                                locDetail.put("lat", curentLocation.latitude);
                                locDetail.put("lon", curentLocation.longitude);
                                locDetail.put("aval", 0);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                            Log.i("LOCATION", locDetail.toString());

                            String topic1 = "taxiLocation/" + mService.deviceId;
                            Log.i("LOCATION", topic);
                            mService.publish(topic1, locDetail.toString());

                            mService.subscribe(mService.gCustomerResponseTopic + mService.reuestedCustomer);


                            //TODO change to chat view activity and insert timer
                            final Intent mapViewIntent = new Intent(getApplicationContext(), chatActivity.class);

                            startActivity(mapViewIntent);


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }).create();
        acc.setCanceledOnTouchOutside(false);
        acc.show();



        waitTimer = new CountDownTimer(60000-diff, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                acc.setTitle("REQUEST (" + (millisUntilFinished/1000) + ")");
                //acc.setMessage("00:"+ (millisUntilFinished/1000));
            }

            @Override
            public void onFinish() {
                //info.setVisibility(View.GONE);
                JSONObject data = new JSONObject();
                try {
                    data.put("id", mService.taxiID);
                    data.put("type", "ack");
                    data.put("value", "REJECT");

                    String topic = mService.gTaxiResponseTopic + mService.taxiID;

                    mService.publish(topic, data.toString());

                    //TODO Cancel timer
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                acc.dismiss();
            }
        }.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Intent intent1 = new Intent(this, mqttService.class);
        // startService(intent1);
        bindService(intent1, mConnection, Context.BIND_AUTO_CREATE);

        Log.i("Recreate", "Regreate");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);





        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        //mLocationRequest = LocationRequest.create()
        //        .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
        //        .setInterval(10 * 1000)        // 10 seconds, in milliseconds
        //        .setFastestInterval(10 * 1000); // 1 second, in milliseconds
        final globalVar gv = ((globalVar)getApplicationContext());

        mLocationRequest = LocationRequest.create().setSmallestDisplacement(gv.mindistance)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(10*1000)
                .setFastestInterval(1*1000);

        setUpMapIfNeeded();
    }

    @Override
    protected void onStart() {
        super.onStart();


        //chatMessages.add()

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MQTT", "Resume");

        if(mBound == false) {
           // Intent intent1 = new Intent(this, mqttService.class);
           // startService(intent1);
           // bindService(intent1, mConnection, Context.BIND_AUTO_CREATE);
        }

        if(registerReceiver(receiver, new IntentFilter(mqttService.NOTIFICATION)) == null){
            //Log.i("ERROR","ERROR2");
        }
        if(registerReceiver(downloadReceiver, new IntentFilter("Download_done")) == null){
            //Log.i("ERROR","ERROR");
        }

        Intent intent = new Intent("Download_done");
        sendBroadcast(intent);

        final globalVar gv = ((globalVar)getApplicationContext());

        MLRoundedImageView cc = (MLRoundedImageView) findViewById(R.id.custommerImageView);
        if(gv.driverImage != null) {
            cc.setImageBitmap(gv.driverImage);
        }

        mGoogleApiClient.connect();
        setUpMapIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        unregisterReceiver(downloadReceiver);
        if(mBound == true) {
           // unbindService(mConnection);
        }
        mService.removeFromManager();

        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }


    }

    @Override
    protected void onStop() {
        super.onStop();
        mService.removeFromManager();
        Intent intent = new Intent(this, mqttService.class);
        stopService(intent);

    }

    @Override
    public void onBackPressed() {
    }




    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                setUpMap();
            }
        }
    }



    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
       // mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));

        //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }


    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Location services connected.");



        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else {
            handleNewLocation(location);
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this);

    }

    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        curentLocation = latLng;
        final globalVar gv = ((globalVar)getApplicationContext());

        gv.myLocation = curentLocation;

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));

        JSONObject locDetail = new JSONObject();
        try {
            locDetail.put("id",mService.taxiID);
            locDetail.put("lat",location.getLatitude());
            locDetail.put("lon",location.getLongitude());
            locDetail.put("aval",1);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mService.currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

        Log.i("LOCATION", locDetail.toString());

        String topic = "taxiLocation/"+mService.deviceId;
        Log.i("LOCATION", topic);
        mService.publish(topic,locDetail.toString());

        for (Map.Entry<String, String> entry : mService.requestedMessage.entrySet())
        {
            //Log.i("LOCATION","Cutommer ID = "+entry.getValue());
            try {

                JSONObject obj = new JSONObject(entry.getValue());
                Log.i("LOCATION", "Cutommer ID = " + obj.get("id"));
                String topic1 = "updateTaxiLocation/"+obj.get("id");
                mService.publish(topic1,locDetail.toString());



            } catch (Throwable t) {

            }
            //JSONObject cData = JSONObject(entry.getValue());
            //Log.i("LOCATION", "Cutommer ID = " + cData.get("id"));
        }


    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect.");
    }



    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }



    public void onProfileClick(View v){
        Log.i("PROFILE CLICK","Profile");
        final Intent mapViewIntent = new Intent(getApplicationContext(), profileActivity.class);

        startActivity(mapViewIntent);

    }
}

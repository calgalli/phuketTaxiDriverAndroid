package com.example.cake.mqtttest;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class chatActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private mqttService mService;
    private boolean mBound = false;
    private LatLng curentLocation;
    private LatLng custommerLocation;

    private Boolean isCancel = true;

    int zoomLevel = 0;

    public class chatMessage {
        public String type;
        public String message;
    }


    private List<chatMessage> chatMessages = new ArrayList<chatMessage>();

    private WebView myWebView;
    private final Handler mWebViewScrollHandler = new Handler();

    private EditText chatText;
    private RelativeLayout chatInput;
    private TextView ETAview;

    public final static String chatActivityMessage = "CHAT_ACTIVITY_MESSAGE";

    //MARK:****************************  MQTT Part ******************************************

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Log.i("MQTT chat", "Messagge from service in Chat");
                String payload = bundle.getString(mqttService.RESULT);
                String topic = bundle.getString(mqttService.TOPIC);

                onCustommerResponses(topic,payload);
                //Start to watch the topic
              /*  if(bundle.getString(mqttService.TOPIC).equals(mService.requestTopic+mService.taxiID)){
                    try {
                        JSONObject obj = new JSONObject(payload);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Log.i("MQTT Map",payload);
                } else if(bundle.get(mqttService.TOPIC).equals(mService.gCustomerResponseTopic + mService.reuestedCustomer)){

                }*/


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
                //TODO add marker to the map
                Log.i("CHAT VIEW", mService.requestedMessage.get(mService.reuestedCustomer));
                JSONObject obj = new JSONObject(mService.requestedMessage.get(mService.reuestedCustomer));
                LatLng latLng = new LatLng(Double.parseDouble(obj.get("lat").toString()), Double.parseDouble(obj.get("lon").toString()));
                custommerLocation = latLng;


                final globalVar gv = ((globalVar)getApplicationContext());

                // Move the camera instantly to hamburg with a zoom of 15.
                Marker kiel = map.addMarker(new MarkerOptions()
                                .position(custommerLocation)
                                .title("Custommer")
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_custommer_icon))
                        );

                int y = new zoomLevel(custommerLocation, gv.myLocation).getZoomLevel();
                Log.i("Xoom Lvel = ", String.valueOf(y));
                if( y > zoomLevel){
                    zoomLevel = y;
                }

                map.moveCamera(CameraUpdateFactory.newLatLngZoom(custommerLocation, zoomLevel));

                // Zoom in, animating the camera.
                map.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel), 2000, null);

                // ====================== update location to a requested customer =======================

                JSONObject data = new JSONObject();
                try {
                    data.put("id",mService.taxiID);
                    data.put("lat",String.valueOf(mService.currentLocation.latitude));
                    data.put("lon",String.valueOf(mService.currentLocation.longitude));
                    data.put("type","locationUpdate");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String topic1 = mService.gTaxiResponseTopic + mService.taxiID;
                Log.i("LOCATION", topic1);
                Log.i("LOCATION", data.toString());
                mService.publish(topic1, data.toString());



            } catch (ClassCastException e) {
                // Pass
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };


    private void onCustommerResponses(String topic, String payload){

        if(topic.equals(mService.gCustomerResponseTopic + mService.reuestedCustomer)){
            try {
                JSONObject obj = new JSONObject(payload);
                if(obj.get("type").equals("cancel")){
                    showCancelAlertView();
                    Log.i("Chat","Custommer cancel");
                } else if(obj.get("type").equals("chat")){
                    chatMessage c1 = new chatMessage();
                    c1.type = "in";
                    c1.message = obj.get("message").toString();
                    chatMessages.add(c1);
                    displayChatMessages();

                }




            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    //************************** End MQTT Part **************************************

    //************************** Web view part **************************************

    private void displayChatMessages(){
        StringBuilder sb = new StringBuilder();
        sb.append("<HTML><HEAD><LINK href=\"chatBubble.css\" type=\"text/css\" rel=\"stylesheet\"/></HEAD><body>");
        //sb.append(tables.toString());


        for (chatMessage temp : chatMessages) {
            if(temp.type.equals("out")) {
                sb.append("<div class=\"bubbledLeft\">");
                sb.append(temp.message);
                sb.append("</div>");
            } else {
                sb.append("<div class=\"bubbledRight\">");
                sb.append(temp.message);
                sb.append("</div>");
            }

        }

        sb.append("</body></HTML>");

        myWebView.loadDataWithBaseURL("file:///android_asset/", sb.toString(), "text/html", "utf-8", null);
        //Scroll down to the bottom webview
        mWebViewScrollHandler.removeCallbacks(mScrollWebViewTask);
        mWebViewScrollHandler.postDelayed(mScrollWebViewTask, 100);


    }

    //Scroll down to the bottom webview
    private final Runnable mScrollWebViewTask = new Runnable() {
        public void run() {
            myWebView.pageDown(true);
        }
    };


    //Map API




    public class ApiDirectionsAsyncTask extends AsyncTask<URL, Integer, StringBuilder> {

        private static final String TAG = "MAP API";

        private static final String DIRECTIONS_API_BASE = "https://maps.googleapis.com/maps/api/directions";
        private static final String OUT_JSON = "/json";

        // API KEY of the project Google Map Api For work
        private static final String API_KEY = "AIzaSyCkkgvHEbB9Q0k4ICWzZBJNd_wV5GEYNzc";

        @Override
        protected StringBuilder doInBackground(URL... params) {
            Log.i(TAG, "doInBackground of ApiDirectionsAsyncTask");

            HttpURLConnection mUrlConnection = null;
            StringBuilder mJsonResults = new StringBuilder();
            try {
                StringBuilder sb = new StringBuilder(DIRECTIONS_API_BASE + OUT_JSON);
                sb.append("?origin=" + String.valueOf(curentLocation.latitude) + "," +  String.valueOf(curentLocation.longitude));
                sb.append("&destination=" + String.valueOf(custommerLocation.latitude) + "," +  String.valueOf(custommerLocation.longitude));
                sb.append("&key=" + API_KEY);
                sb.append("&mode=driving&sensor=false");

                URL url = new URL(sb.toString());
                mUrlConnection = (HttpURLConnection) url.openConnection();
                InputStreamReader in = new InputStreamReader(mUrlConnection.getInputStream());

                // Load the results into a StringBuilder
                int read;
                char[] buff = new char[1024];
                while ((read = in.read(buff)) != -1){
                    mJsonResults.append(buff, 0, read);
                }

                Log.i(TAG, String.valueOf(mJsonResults));


                JSONObject jsonObject = new JSONObject();
                try {

                    jsonObject = new JSONObject(String.valueOf(mJsonResults));

                    JSONArray array = jsonObject.getJSONArray("routes");

                    JSONObject routes = array.getJSONObject(0);

                    JSONArray legs = routes.getJSONArray("legs");

                    JSONObject steps = legs.getJSONObject(0);

                    JSONObject distance = steps.getJSONObject("distance");




                    JSONObject durationText = steps.getJSONObject("duration");


                    Log.i("Distance", distance.get("text").toString());
                    Log.i("Duration", durationText.get("text").toString());
                    //dist = Double.parseDouble(distance.getString("text").replaceAll("[^\\.0123456789]","") );
                    final String d = distance.get("text").toString();
                    final String e = durationText.get("text").toString();



                    chatActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ETAview = (TextView) findViewById(R.id.ETAview);
                            ETAview.setText("ระยะทาง : " + d + " เวลา : " + e);
                            //Your code to run in GUI thread here
                        }//public void run() {
                    });



                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }


            } catch (MalformedURLException e) {
                Log.e(TAG, "Error processing Distance Matrix API URL");
                return null;

            } catch (IOException e) {
                System.out.println("Error connecting to Distance Matrix");
                return null;
            } finally {
                if (mUrlConnection != null) {
                    mUrlConnection.disconnect();
                }
            }

            return mJsonResults;
        }
    }

    private void showCancelAlertView(){



        new AlertDialog.Builder(this)
                .setTitle("REQUEST")
                .setMessage("ลูกค้ายกเลิก แต่ท่านจะได้ค่าชดเชย")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //TODO Go back to MapsActivity
                        allDone();
                       // unbindService(mConnection);

                        final Intent mapViewIntent = new Intent(getApplicationContext(), MapsActivity.class);

                        startActivity(mapViewIntent);

                    }
                }).show();



    }


    //************************** Button callback ************************************
    public void onFinishClick(View v) {

        /*Log.i("MQTT Chat", "Finish clicked");
        JSONObject done = new JSONObject();
        try {
            done.put("id", mService.taxiID);
            done.put("type", "done");
            done.put("message", "done");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        String topic1 = mService.gTaxiResponseTopic + mService.taxiID;
        mService.publish(topic1, done.toString());

        allDone();*/




        if(isCancel == true) {
            JSONObject done = new JSONObject();
            try {
                done.put("id", mService.taxiID);
                done.put("type", "onthewayCancel");
                done.put("message", "yes");

            } catch (JSONException e) {
                e.printStackTrace();
            }
            String topic1 = mService.gTaxiResponseTopic + mService.taxiID;
            mService.publish(topic1, done.toString());

            final Intent transactionIntent = new Intent(this, MapsActivity.class);

            startActivity(transactionIntent);
        } else {


            final Intent transactionIntent = new Intent(this, transactionActivity.class);

            startActivity(transactionIntent);
        }


    }

    public void onPickupClick(View v) {


        JSONObject done = new JSONObject();
        try {
            done.put("id", mService.taxiID);
            done.put("type", "picup");
            done.put("message", "yes");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        String topic1 = mService.gTaxiResponseTopic + mService.taxiID;
        mService.publish(topic1, done.toString());

        isCancel = false;

        Button bb = (Button)findViewById(R.id.finishButton);
        bb.setText("Finish");

        Button bb2 = (Button)findViewById(R.id.pickUpButton);
        bb2.setVisibility(View.GONE);

    }

    public void onSendChatMessage(View v){
        if(chatText.getText().length() > 0) {
            JSONObject done = new JSONObject();
            try {
                done.put("id", mService.taxiID);
                done.put("type", "chat");
                done.put("message", chatText.getText().toString());

            } catch (JSONException e) {
                e.printStackTrace();
            }
            String topic1 = mService.gTaxiResponseTopic + mService.taxiID;
            mService.publish(topic1, done.toString());


            chatMessage c1 = new chatMessage();
            c1.type = "out";
            c1.message =chatText.getText().toString();
            chatMessages.add(c1);
            chatText.setText("", TextView.BufferType.EDITABLE);
            displayChatMessages();

        }

    }

    //*************************** End MAP *******************************************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent intent = new Intent(this, mqttService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);



        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

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

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        myWebView = (WebView) findViewById(R.id.webView);


        chatInput = (RelativeLayout) findViewById(R.id.chatInput);
        chatText = (EditText) findViewById(R.id.chatText);

        displayChatMessages();





    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MQTT Chat", "Resume");
        if(mBound == false) {
           // Intent intent = new Intent(this, mqttService.class);
          //  bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }


        registerReceiver(receiver, new IntentFilter(mqttService.NOTIFICATION));
        mGoogleApiClient.connect();
        setUpMapIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mBound == true) {
           // unbindService(mConnection);
        }


        unregisterReceiver(receiver);
        mService.removeFromManager();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }


    @Override
    public void onBackPressed() {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void allDone(){

        //Unsubscribe custommer
        mService.unsubscribe(mService.gCustomerResponseTopic + mService.reuestedCustomer);
        //Update taxi status to available
        JSONObject locDetail = new JSONObject();
        try {
            locDetail.put("id",mService.taxiID);
            locDetail.put("lat",curentLocation.latitude);
            locDetail.put("lon",curentLocation.longitude);
            locDetail.put("aval",1);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        Log.i("LOCATION", locDetail.toString());

        String topic = "taxiLocation/"+mService.deviceId;
        Log.i("LOCATION", topic);
        mService.publish(topic, locDetail.toString());


    }

    //******************************** MAP calls *************************************************


    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.


            map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .getMap();

            // Check if we were successful in obtaining the map.
            if (map != null) {
                map.setMyLocationEnabled(true);

            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
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
        Log.d("MAP", location.toString());
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);
        curentLocation = latLng;
        //Update location to manager
        JSONObject locDetail = new JSONObject();
        try {
            locDetail.put("id",mService.taxiID);
            locDetail.put("lat",location.getLatitude());
            locDetail.put("lon",location.getLongitude());
            locDetail.put("aval",0);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        Log.i("LOCATION", locDetail.toString());

        String topic = "taxiLocation/"+mService.deviceId;

        mService.publish(topic,locDetail.toString());
        //Update location to requested custommer
        // ====================== update location to a requested customer =======================

        JSONObject data = new JSONObject();
        try {
            data.put("id",mService.taxiID);
            data.put("lat",String.valueOf(location.getLatitude()));
            data.put("lon",String.valueOf(location.getLongitude()));
            data.put("type","locationUpdate");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String topic1 = mService.gTaxiResponseTopic + mService.taxiID;
        Log.i("LOCATION", topic1);
        Log.i("LOCATION",data.toString());
        mService.publish(topic1, data.toString());

        //getDistanceInfo();

        new ApiDirectionsAsyncTask().execute();



    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
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
            Log.i("MAP", "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }
}

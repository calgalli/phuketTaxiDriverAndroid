package com.example.cake.mqtttest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.location.LocationServices;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class transactionActivity extends ActionBarActivity {

    private String pIdNumber;
    private String pCustomerID;

    private mqttService mService;
    private boolean mBound = false;


    private ServiceConnection mConnection = new ServiceConnection() {
        // @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                // We've bound to LocalService, cast the IBinder and get LocalService instance.
                mqttService.mqttBinder binder = (mqttService.mqttBinder) service;
                mService = binder.getService();
                mBound = true;

                Log.i("Nationality", mService.nationality);

                if(mService.nationality.equals("Thai") || mService.nationality.equals("Thailand")){
                    Button payByCraditcard = (Button) findViewById(R.id.craditCard);
                    payByCraditcard.setVisibility(View.GONE);

                }




            } catch (ClassCastException e) {
                // Pass
            }
        }

        // @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);
        Intent intent = new Intent(this, mqttService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mBound == false) {
         }
    }

    @Override
    protected void onPause() {
        super.onPause();
       // if(mBound == true) {
  //          unbindService(mConnection);
        //}



    }



    public void onSendTranClick(View v) {


        EditText fare = (EditText) findViewById(R.id.fare);

        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String date = df.format(Calendar.getInstance().getTime());
        DateFormat df2 = new SimpleDateFormat("HH:mm:ss");
        String time = df2.format(Calendar.getInstance().getTime());

        Log.i("DATE TIME", date);
        Log.i("DATE TIME", time);

        JSONObject locDetail = new JSONObject();
        try {
            locDetail.put("id", mService.taxiID);
            locDetail.put("type", "done");
            locDetail.put("message", fare.getText());
            locDetail.put("cash", "yes");
        } catch (JSONException e) {
            e.printStackTrace();
        }




        String topic1 = mService.gTaxiResponseTopic + mService.taxiID;
        mService.publish(topic1, locDetail.toString());


        allDone();


    }


    public void onPayByCraditClick(View v) {


        EditText fare = (EditText) findViewById(R.id.fare);

        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String date = df.format(Calendar.getInstance().getTime());
        DateFormat df2 = new SimpleDateFormat("HH:mm:ss");
        String time = df2.format(Calendar.getInstance().getTime());

        Log.i("DATE TIME", date);
        Log.i("DATE TIME", time);

        JSONObject locDetail = new JSONObject();
        try {
            locDetail.put("id", mService.taxiID);
            locDetail.put("type", "done");
            locDetail.put("message", fare.getText());
            locDetail.put("cash", "no");
        } catch (JSONException e) {
            e.printStackTrace();
        }




        String topic1 = mService.gTaxiResponseTopic + mService.taxiID;
        mService.publish(topic1, locDetail.toString());


        allDone();


    }

    public void allDone(){

        mService.unsubscribe(mService.gCustomerResponseTopic + mService.reuestedCustomer);
        JSONObject locDetail = new JSONObject();
        try {
            locDetail.put("id", mService.taxiID);
            locDetail.put("lat", mService.currentLocation.latitude);
            locDetail.put("lon", mService.currentLocation.longitude);
            locDetail.put("aval", 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        Log.i("LOCATION", locDetail.toString());

        String topic1 = "taxiLocation/" + mService.deviceId;
        mService.publish(topic1, locDetail.toString());

        mService.requestedMessage.remove(mService.reuestedCustomer);
        mService.reuestedCustomer = "";

        final Intent mapIntent = new Intent(getApplicationContext(), MapsActivity.class);
        startActivity(mapIntent);


    }



}

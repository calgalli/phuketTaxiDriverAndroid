package com.example.cake.mqtttest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class profileActivity extends ActionBarActivity implements AsyncResponse {

    SharedPreferences sharedpreferences;
    public static final String MyPREFERENCES = "MyPrefs" ;

    public class transactionDetail {

        public String custommerName = "";
        public String date = "";
        public String time = "";
        public String fare = "";
        public String cutommerId = "";
        public String tip = "";
    }

    private ArrayList<transactionDetail> transactions = new ArrayList<transactionDetail>();
    private ArrayList<String> ids = new ArrayList<String>();

    public Map<String, transactionDetail> transactionsDict = new HashMap<String, transactionDetail>();
    public Map<String, String> custommerNames = new HashMap<String, String>();


    private ListView listView;

    int totallist = 0;
    int accList = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        listView = (ListView) findViewById(R.id.profile_ListView);

        setContentView(R.layout.activity_profile);
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);


        final globalVar gv = ((globalVar) getApplicationContext());

        String[] loginParameters = {gv.taxiID};

        Log.i("PROFILE", gv.taxiID);

        sendRequest loginRequest = new sendRequest();
        loginRequest.delegate = this;
        loginRequest.execute(loginParameters);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_profile, menu);
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

    public class sendRequest extends AsyncTask<String, Void, JSONArray> {
        private ClientConnectionManager clientConnectionManager;
        //private HttpContext context;
        private HttpParams params;
        private DefaultHttpClient client;
        private JSONArray jj = null;
        public AsyncResponse delegate = null;

        @Override
        protected JSONArray doInBackground(String[] params1) {
            // do above Server call here
            final globalVar gv = ((globalVar) getApplicationContext());

            client = new DefaultHttpClient(clientConnectionManager, params);
            HttpPost get = new HttpPost("https://" + gv.mainHost + ":1880/getTaxiTransactions");
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            String custommerID = "";

            if (params1.length == 1) {
                custommerID = params1[0];

            }

            Log.i("PROFILE", "https://" + gv.mainHost + ":1880/getTaxiTransactions");

            nameValuePairs.add(new BasicNameValuePair("idNumber", custommerID));
            try {
                get.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            HttpResponse response = null;
            try {
                response = client.execute(get);
                Log.i("Http", response.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            String json = null;

            try {
                json = EntityUtils.toString(response.getEntity());
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    // String retSrc = EntityUtils.toString(entity);
                    // parsing JSON
                    JSONArray result = new JSONArray(json); //Convert String to JSON Object
                    if (result.length() > 0) {
                        jj = result;
                        // Log.i("Http", response.getStatusLine().toString());
                        Log.i("Http xxxxxx ", jj.toString());
                        return jj;
                    } else {
                        Log.i("Http", "Login fail !!!!!!!");
                        Log.i("Http", response.getStatusLine().toString());
                        Log.i("Http", json.toString());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return jj;
        }

        @Override
        protected void onPostExecute(JSONArray obj) {

            delegate.processFinishArray(obj);


        }

        @Override
        protected void onPreExecute() {
            SchemeRegistry schemeRegistry = new SchemeRegistry();

            // http scheme
            schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            // https scheme
            schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 1880));

            params = new BasicHttpParams();
            params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 1);
            params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(1));
            params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, "utf8");
            clientConnectionManager = new ThreadSafeClientConnManager(params, schemeRegistry);


        }

    }


    public class sendRequestDriverName extends AsyncTask<String, Void, JSONObject> {
        private ClientConnectionManager clientConnectionManager;
        //private HttpContext context;
        private HttpParams params;
        private DefaultHttpClient client;
        private JSONObject jj = null;
        public AsyncResponse delegate = null;

        @Override
        protected JSONObject doInBackground(String[] params1) {
            // do above Server call here
            final globalVar gv = ((globalVar) getApplicationContext());

            client = new DefaultHttpClient(clientConnectionManager, params);
            HttpPost get = new HttpPost("https://" + gv.mainHost + ":1880/getCustommerData");
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            String driverID = "";


            if (params1.length == 1) {

                driverID = params1[0];

            }

            String newStr = driverID.replaceFirst("id", "");

            Log.i("ID ==== ", newStr);


            nameValuePairs.add(new BasicNameValuePair("passportID", newStr));
            try {
                get.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            HttpResponse response = null;
            try {
                response = client.execute(get);
                Log.i("Http", response.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            String json = null;

            try {
                json = EntityUtils.toString(response.getEntity());
                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    // String retSrc = EntityUtils.toString(entity);
                    // parsing JSON
                    JSONArray result = new JSONArray(json); //Convert String to JSON Object
                    if (result.length() > 0) {
                        jj = result.getJSONObject(0);
                        // Log.i("Http", response.getStatusLine().toString());
                        // Log.i("Http", jj.toString());
                    } else {
                        Log.i("Http", "Login fail !!!!!!!");
                        Log.i("Http", response.getStatusLine().toString());
                        Log.i("Http", json.toString());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return jj;
        }

        @Override
        protected void onPostExecute(JSONObject obj) {

            delegate.loginFinish(obj);


        }

        @Override
        protected void onPreExecute() {
            SchemeRegistry schemeRegistry = new SchemeRegistry();

            // http scheme
            schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            // https scheme
            schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 1880));

            params = new BasicHttpParams();
            params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 1);
            params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(1));
            params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, "utf8");
            clientConnectionManager = new ThreadSafeClientConnManager(params, schemeRegistry);


        }

    }


    @Override
    public void loginFinish(JSONObject output) {
        accList++;
        try {
            custommerNames.put(output.getString("passportID"), output.getString("name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }


        if (accList == totallist) {
            reloadTable();
            Log.i("custommerNames name = ", custommerNames.toString());
        }
    }

    @Override
    public void processFinishArray(JSONArray output) {
        if(output != null) {
            Log.i("Http xxxxxx ", output.toString());


            int i;
            JSONObject tt;
            totallist = output.length();
            for (i = 0; i < output.length(); i++) {
                transactionDetail x = new transactionDetail();

                try {
                    tt = output.getJSONObject(i);

                    x.date = tt.getString("date");
                    x.custommerName = "";
                    x.fare = tt.getString("fare");
                    x.time = tt.getString("time");
                    x.cutommerId = tt.getString("customerID");
                    x.tip = tt.getString("tip");

                    //transactionsDict.put(String.valueOf(i), x);
                    Log.i("DETAIL : ", "Date : " + x.date + " Time : " + x.time + " Fare : " + x.fare);

                    ids.add(x.cutommerId);


                } catch (JSONException e) {
                    e.printStackTrace();
                }

                transactions.add(i, x);

            }

            Log.i("GGGGGGG", transactions.toString());

            // String[] unique = new HashSet<String>(Arrays.asList(ids).toArray(new String[0]));

            Set<String> kk = new HashSet<String>(ids);

            totallist = kk.size();

            for (Object object : kk) {
                String[] loginParameters = {(String) object};
                sendRequestDriverName loginRequest = new sendRequestDriverName();
                loginRequest.delegate = this;
                loginRequest.execute(loginParameters);
            }
        }

        /*for(i = 0; i < unique.size();i++) {
            String[] loginParameters = {unique.};

            //Log.i("Driver ID = ", x.driverId);


        }*/

    }


    public void onClickBackProfile(View v) {
        Intent onthewayIntent = new Intent(getApplicationContext(), MapsActivity.class);

        startActivity(onthewayIntent);
    }

    public void onClickLogout(View v) {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("email", "");
        editor.putString("password", "");
        editor.putString("hasLogin", "no");
        editor.commit();
        Intent i_carent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(i_carent);
    }

    public void reloadTable() {
        //customeCell adapter = new customeCell(this, -1, arrayOfDrivers);

        List<Map<String, String>> data = new ArrayList<Map<String, String>>();

        for (int i = 0; i < transactions.size(); i++) {

            transactionDetail x = transactions.get(i);
            Map<String, String> datum = new HashMap<String, String>(2);

            String newStr = x.cutommerId.replaceFirst("id", "");
            datum.put("title", custommerNames.get(newStr));
            datum.put("subtitle", "Date : " + x.date + " Time : " + x.time + " Fare : " + x.fare + " tip : " + x.tip);
            data.add(datum);

           // Log.i("KKKKK", data.toString());
        }

        Log.i("DETAIL : ", data.toString());
        SimpleAdapter adapter = new SimpleAdapter(this, data,
                android.R.layout.simple_list_item_2,
                new String[] {"title", "subtitle"},
                new int[] {android.R.id.text1,
                        android.R.id.text2});

        ListView listViewx = (ListView) findViewById(R.id.profile_ListView);

        listViewx.setAdapter(adapter);

    }
}

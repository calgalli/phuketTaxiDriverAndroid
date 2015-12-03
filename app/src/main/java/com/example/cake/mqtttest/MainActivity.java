package com.example.cake.mqtttest;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;

import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;



public class MainActivity extends ActionBarActivity implements AsyncResponse {

    private mqttService mService;
    private boolean mBound = false;

    private String taxiID;
    private JSONObject taxiData;

    public Boolean isDownload = false;

    public static final String DOWNLOAD_DONE = "Download_done";

    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String DEFAULT_PREF = "NIL" ;


    SharedPreferences sharedpreferences;
    String emailP = "";
    String passwordP = "";

    private Context context;
    private ProgressDialog pd;


    //final Intent mapViewIntent = new Intent(this, MapsActivity.class);


    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Log.d("MQTT","Messagge from service");
                String resultCode = bundle.getString(mqttService.RESULT);
                Log.d("MQTT","resultCode");

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
                //Message msg = Message.obtain(null, mService.MSG_REGISTER_CLIENT);
                mService.deviceId = taxiID;
                mService.taxiID = "id"+taxiID;
                mService.taxiData = taxiData;
                Log.i("MQTT", "Taxi id = " + taxiID);
                mService.connect();
                Log.d("MQTT", "MQTT Connected");
                mService.publish("test/tt", "Hello world x");

                mService.subscribe("testtopic2");

               // changeView();






            } catch (ClassCastException e) {
                // Pass
            }
        }

        // @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };


    public void changeView(){
        new CountDownTimer(1000, 1000) {
            public void onFinish() {
                // When timer is finished
                // Execute your code here
                if(isDownload == true) {
                    final Intent mapViewIntent = new Intent(getApplicationContext(), MapsActivity.class);

                    startActivity(mapViewIntent);
                } else {
                    this.start();
                }


            }

            public void onTick(long millisUntilFinished) {
                // millisUntilFinished    The amount of time until finished.
            }
        }.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        LocationManager mlocManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);;
        boolean enabled = mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(enabled){
            Log.i("GPS","GPS is enable.");
        } else {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle("GPS is disable")
                    .setMessage("Please enable you GPS")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);


                        }
                    }).show();
        }

        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedpreferences.edit();

        String emailLogin = sharedpreferences.getString("email", DEFAULT_PREF);
        String passwordLogin = sharedpreferences.getString("password", DEFAULT_PREF);
        String hasLogin = sharedpreferences.getString("hasLogin", DEFAULT_PREF);


        if(hasLogin.equals("yes")){

            Log.i("Http", "Async task !!!!!!!!!!!!!!!!!!");
            Log.i("YYYY", emailLogin + " " + passwordLogin);

            String [] loginParameters = {emailLogin,passwordLogin};
            sendRequest loginRequest = new sendRequest();
            loginRequest.delegate = MainActivity.this;
            loginRequest.execute(loginParameters);
        }





    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MQTT", "Resume");
        registerReceiver(receiver, new IntentFilter(mqttService.NOTIFICATION));
    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }


    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService



        //HttpPost post = new HttpPost("https://128.199.97.22/driverLogin");

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    public void onLoginClick(View v){
        //getResponseFromUrl("https://128.199.97.22/driverLogin/email=fa@yahoo.com&password=123456");
        EditText text = (EditText)findViewById(R.id.email);
        String email = text.getText().toString();
        EditText passwordText = (EditText)findViewById(R.id.password);
        String password = passwordText.getText().toString();

        emailP = email;
        passwordP = password;


        Log.d("Http", "Async task !!!!!!!!!!!!!!!!!!");
        String [] loginParameters = {email,password};
        sendRequest loginRequest = new sendRequest();
        loginRequest.delegate = this;
        loginRequest.execute(loginParameters);

    }

    public void onRegisterClick(View v){
        final Intent mapViewIntent = new Intent(getApplicationContext(), registerActivity.class);

        startActivity(mapViewIntent);
    }
    //When login finish, the result will be return here
    public void loginFinish(JSONObject output){

        String hasLogin = sharedpreferences.getString("hasLogin", DEFAULT_PREF);

        if(hasLogin.equals(DEFAULT_PREF) || hasLogin.equals("no")) {
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString("email", emailP);
            editor.putString("password", passwordP);
            editor.putString("hasLogin", "yes");
            editor.commit();
        }


        //this you will received result fired from async class of onPostExecute(result) method.
        if(output == null){
            Toast.makeText(getApplicationContext(), "Login fail",
                    Toast.LENGTH_LONG).show();
        } else {

            final globalVar gv = (globalVar) getApplicationContext();
            Log.i("Http", output.toString());
            try {
                taxiID = (String) output.get("idNumber");
                gv.taxiID = (String) output.get("id");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            taxiData = output;

            Intent intent = new Intent(this, mqttService.class);
           // startService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);


            String url = "http://" + gv.mainHost + gv.pathToImage + taxiID + ".png";
            Log.i("URL", url);
            new DownloadImageTask(gv, isDownload).execute(url);


        }
    }

    @Override
    public void processFinishArray(JSONArray output) {

    }


    public class sendRequest extends AsyncTask<String, Void, JSONObject> {
        private ClientConnectionManager clientConnectionManager;
        //private HttpContext context;
        private HttpParams params;
        private DefaultHttpClient client;
        private JSONObject jj = null;
        public AsyncResponse delegate=null;
        @Override
        protected JSONObject doInBackground(String[] params1) {
            // do above Server call here
            client = new DefaultHttpClient(clientConnectionManager, params);
            HttpPost get = new HttpPost("https://128.199.97.22:1880/driverLogin");
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            String userName = "";
            String password = "";
            if(params1.length == 2) {
                userName = params1[0];
                password = params1[1];
            }
            nameValuePairs.add(new BasicNameValuePair("email", userName));
            nameValuePairs.add(new BasicNameValuePair("password", password));
            try {
                get.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            HttpResponse response = null;
            try {
                response = client.execute(get);
                Log.i("Http",response.toString());
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
                    if(result.length() == 1) {
                        jj = result.getJSONObject(0);
                        // Log.i("Http", response.getStatusLine().toString());
                        // Log.i("Http", jj.toString());
                    } else {
                        Log.i("Http", "Login fail !!!!!!!");
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
            //process message
            if(obj != null)
            {
                //do something
            }


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


            /*CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            //set the user credentials for our site "example.com"
            credentialsProvider.setCredentials(new AuthScope("example.com", AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials("UserNameHere", "UserPasswordHere"));

            context = new BasicHttpContext();
            context.setAttribute("http.auth.credentials-provider", credentialsProvider);*/
        }

    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        globalVar bmImage;
        Boolean isDownload;

        public DownloadImageTask(globalVar bmImage, Boolean isDownload) {
            this.bmImage = bmImage;
            this.isDownload = isDownload;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(context);
            pd.setTitle("Signing in");
            pd.setMessage("Please wait.");
            pd.setCancelable(false);
            pd.setIndeterminate(true);
            pd.show();
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setDriverImage(result);
            Intent intent = new Intent("Download_done");
            sendBroadcast(intent);
            Log.i("DOWNLOAD", "XXX");

            if (pd!=null) {
                pd.dismiss();

            }

            final Intent mapViewIntent = new Intent(getApplicationContext(), MapsActivity.class);

            startActivity(mapViewIntent);
            isDownload = true;
        }
    }







}







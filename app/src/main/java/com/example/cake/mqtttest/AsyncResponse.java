package com.example.cake.mqtttest;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by cake on 7/25/15 AD.
 */
public interface AsyncResponse {
    void loginFinish(JSONObject output);
    void processFinishArray(JSONArray output);
}

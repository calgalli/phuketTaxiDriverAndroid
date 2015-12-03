package com.example.cake.mqtttest;

import android.app.Application;
import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cake on 8/8/15 AD.
 */
public class globalVar extends Application {

    public Boolean allSelected = false;

    public Bitmap driverImage;
    public String taxiID;
    public String custommerID;
    public LatLng myLocation;


    public String fare;
    public  String isCash;

    public float mindistance = 100;

    String pathToImage = "/images/drivers/";
    String mainHost = "128.199.97.22";
    public String distanceKey = "AIzaSyCkkgvHEbB9Q0k4ICWzZBJNd_wV5GEYNzc";



    public  void setDriverImage(Bitmap x){
        this.driverImage = x;
    }


}

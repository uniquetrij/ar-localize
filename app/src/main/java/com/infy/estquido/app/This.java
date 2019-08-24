package com.infy.estquido.app;

import android.app.Application;
import android.content.Context;

import com.infy.estquido.MainActivity;

import java.util.concurrent.atomic.AtomicReference;


public class This {

    public static final AtomicReference<Context> CONTEXT = new AtomicReference<>();
    public static final AtomicReference<Application> APPLICATION = new AtomicReference<>();
    public static final AtomicReference<MainActivity> MAIN_ACTIVITY = new AtomicReference<>();
    public static final AtomicReference<MainActivity> LOCATION = new AtomicReference<>();


    public static class Static {
        public static final String COUCHBASE_URL = "ws://192.168.1.101:4984/estquido";
        public static final String COUCHBASE_DB = "estquido";
        public static final String COUCHBASE_USER = "estquido";
        public static final String COUCHBASE_PASS = "estquido";
    }
}

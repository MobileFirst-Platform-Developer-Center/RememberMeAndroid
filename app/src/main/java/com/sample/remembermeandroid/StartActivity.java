/**
* Copyright 2016 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.sample.remembermeandroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import com.worklight.wlclient.api.WLClient;

public class StartActivity extends AppCompatActivity {
    private StartActivity _this;
    private BroadcastReceiver loginSuccessReceiver, loginRequiredReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        _this = this;

        //Initialize the MobileFirst SDK. This needs to happen just once.
        WLClient.createInstance(this);

        //Initialize the challenge handler
        UserLoginChallengeHandler.createAndRegister();

        //Handle auto-login success
        loginSuccessReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Go to the protected area
                Intent openProtected = new Intent(_this, ProtectedActivity.class);
                _this.startActivity(openProtected);
            }
        };

        //Handle auto-login failure
        loginRequiredReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Open login screen
                Intent login = new Intent(_this, LoginActivity.class);
                _this.startActivity(login);
            }
        };

        //Try to auto-login
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_LOGIN_AUTO);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        LocalBroadcastManager.getInstance(this).registerReceiver(loginSuccessReceiver, new IntentFilter(Constants.ACTION_LOGIN_SUCCESS));
        LocalBroadcastManager.getInstance(this).registerReceiver(loginRequiredReceiver, new IntentFilter(Constants.ACTION_LOGIN_REQUIRED));
        LocalBroadcastManager.getInstance(this).registerReceiver(loginRequiredReceiver, new IntentFilter(Constants.ACTION_LOGIN_FAILURE));

    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(loginSuccessReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(loginRequiredReceiver);
        super.onStop();
    }
}

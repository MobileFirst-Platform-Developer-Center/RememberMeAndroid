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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.worklight.wlclient.api.WLAccessTokenListener;
import com.worklight.wlclient.api.WLAuthorizationManager;
import com.worklight.wlclient.api.WLClient;
import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLResourceRequest;
import com.worklight.wlclient.api.WLResponse;
import com.worklight.wlclient.api.WLResponseListener;
import com.worklight.wlclient.auth.AccessToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

public class ProtectedActivity extends AppCompatActivity {

    private ProtectedActivity _this;
    private Button getBalanceButton, logoutButton;
    private TextView resultTextView, helloLabel;
    private BroadcastReceiver logoutReceiver, loginRequiredReceiver, loginSuccessReceiver;
    private final String DEBUG_NAME = "ProtectedActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(DEBUG_NAME, "onCreate");

        _this = this;

        setContentView(R.layout.activity_protected);

        getBalanceButton = (Button)findViewById(R.id.getBalance);
        getBalanceButton.setVisibility(View.INVISIBLE);

        logoutButton = (Button)findViewById(R.id.logout);
        logoutButton.setVisibility(View.INVISIBLE);

        resultTextView = (TextView)findViewById(R.id.resultText);
        resultTextView.setVisibility(View.INVISIBLE);

        helloLabel = (TextView)findViewById(R.id.helloLabel);
        helloLabel.setVisibility(View.INVISIBLE);

        getBalanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                URI adapterPath = null;

                try {
                    adapterPath = new URI("/adapters/ResourceAdapter/balance");
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                WLResourceRequest request = new WLResourceRequest(adapterPath, WLResourceRequest.GET);
                request.send(new WLResponseListener() {
                    @Override
                    public void onSuccess(WLResponse wlResponse) {
                        updateTextView("Balance: " + wlResponse.getResponseText());
                    }

                    @Override
                    public void onFailure(WLFailResponse wlFailResponse) {
                        Log.d("Failure", wlFailResponse.getErrorMsg());
                        updateTextView("Failed to get balance.");
                    }
                });
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateTextView("");
                Intent intent = new Intent();
                intent.setAction(Constants.ACTION_LOGOUT);
                LocalBroadcastManager.getInstance(_this).sendBroadcast(intent);
            }
        });

        logoutReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(DEBUG_NAME, "logoutReceiver");
                Intent start = new Intent(_this, LoginActivity.class);
                _this.startActivity(start);
            }
        };

        loginRequiredReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(DEBUG_NAME, "loginRequiredReceiver");
                Intent login = new Intent(_this, LoginActivity.class);
                _this.startActivity(login);
            }
        };

        loginSuccessReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(DEBUG_NAME, "loginSuccessReceiver");
                updateUI();
            }
        };


        // Obtain Access Token
        WLAuthorizationManager.getInstance().obtainAccessToken("UserLogin", new WLAccessTokenListener() {
            @Override
            public void onSuccess(AccessToken accessToken) {
                Log.d("UserLogin", "auto login success");
            }

            @Override
            public void onFailure(WLFailResponse wlFailResponse) {
                Log.d("UserLogin", "auto login failure");
            }
        });
    }

    public void updateTextView(final String str){
        Runnable run = new Runnable() {
            public void run() {
                resultTextView.setText(str);
            }
        };
        this.runOnUiThread(run);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(DEBUG_NAME, "onStart");

        LocalBroadcastManager.getInstance(this).registerReceiver(loginSuccessReceiver, new IntentFilter(Constants.ACTION_LOGIN_SUCCESS));
        LocalBroadcastManager.getInstance(this).registerReceiver(logoutReceiver, new IntentFilter(Constants.ACTION_LOGOUT_SUCCESS));
        LocalBroadcastManager.getInstance(this).registerReceiver(loginRequiredReceiver, new IntentFilter(Constants.ACTION_LOGIN_REQUIRED));
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(DEBUG_NAME, "onPause");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(logoutReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(loginRequiredReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(loginSuccessReceiver);
    }


    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to logout?").setTitle("Logout");
        builder.setPositiveButton("Logout", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Constants.ACTION_LOGOUT);
                LocalBroadcastManager.getInstance(_this).sendBroadcast(intent);
            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateUI(){
        //Show the display name
        SharedPreferences preferences = _this.getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE);
        if(preferences.getString(Constants.PREFERENCES_KEY_USER,null) != null){
            try {
                JSONObject user = new JSONObject(preferences.getString(Constants.PREFERENCES_KEY_USER,null));
                helloLabel.setText("Hello " + user.getString("displayName"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            getBalanceButton.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.VISIBLE);
            resultTextView.setVisibility(View.VISIBLE);
            helloLabel.setVisibility(View.VISIBLE);
        }
    }
}

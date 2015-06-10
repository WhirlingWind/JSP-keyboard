package com.jsp.facebook;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.facebook.AccessTokenTracker;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.CallbackManager;
import com.facebook.AccessToken;
import com.jsp.util.DatabaseHandler;
import com.jsp.util.DatabaseHelper;
import com.jsp.movie.lesskey.R;


public class FBActivity extends Activity implements FacebookCallback<LoginResult>, GraphRequest.Callback, FacebookParser.OnPostListener {

    CallbackManager callbackManager;
    AccessToken accessToken = null;
    AccessTokenTracker accessTokenTracker;

    String a = "";

    private LinearLayout main_facebook;

    public DatabaseHelper dbHelper;
    public SQLiteDatabase db;
    public DatabaseHandler dbHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getApplicationContext());

        setContentView(R.layout.fb_main);

        setTitle("페이스북에서 수집");

        callbackManager=CallbackManager.Factory.create();
        com.facebook.login.widget.LoginButton fb_login_button=new com.facebook.login.widget.LoginButton(this);
        main_facebook=(LinearLayout)findViewById(R.id.start_facebook);
        main_facebook.setGravity(Gravity.CENTER_HORIZONTAL);
        fb_login_button.setReadPermissions("user_friends");
        fb_login_button.setReadPermissions("read_stream");

        fb_login_button.registerCallback(callbackManager, this);

        LinearLayout.LayoutParams param=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        main_facebook.addView(fb_login_button, param);
        accessTokenTracker=new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken newAccessToken) {
                updateWithToken(newAccessToken);
            }
        };

        progress = new ProgressDialog(this);

        dbHelper = new DatabaseHelper(this);
        try {
            dbHelper.createDataBase();
        } catch (Exception e) {

            e.printStackTrace();
        }
        db = dbHelper.openDataBase();
        dbHandler = new DatabaseHandler(db);
    }

    private void updateWithToken(AccessToken currentAccessToken){
        if(currentAccessToken!=null){
            accessToken=AccessToken.getCurrentAccessToken();

        }
    }

    @Override
    public void onResume() {
        super.onResume();

        accessToken = AccessToken.getCurrentAccessToken();

        if (accessToken != null) {

            GraphRequest request = GraphRequest.newGraphPathRequest(accessToken, "/me/home", this);

            progress.setMessage("Downloading :)");
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setIndeterminate(true);
            progress.show();

            request.executeAsync();
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        accessTokenTracker.stopTracking();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    ProgressDialog progress;

    @Override
    public void onCompleted(GraphResponse graphResponse) {

        FacebookParser parser = new FacebookParser(this, dbHandler);

        progress.setMessage("Parsing :)");

        parser.l = this;
        parser.execute(graphResponse.getJSONObject());
    }

    @Override
    public void onPostExecute() {

        progress.cancel();
    }

    @Override
    public void onSuccess(LoginResult loginResult) {

        accessToken = loginResult.getAccessToken();
        GraphRequest request = GraphRequest.newGraphPathRequest(accessToken, "/me/home", this);

        progress.setMessage("Downloading :)");
        progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progress.setIndeterminate(true);
        progress.show();

        request.executeAsync();
    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onError(FacebookException e) {

        Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
    }
}

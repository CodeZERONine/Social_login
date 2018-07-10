package org.akshanshgusain.social_login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener{
    private LoginButton mFacebook;
    private SignInButton mGoogle;
    private ImageView mImageView;
    private TextView mTextView;
    private CallbackManager mCallbackManager;
    private ProgressDialog mProgressDialog;
    private Button mGoogleOut;
    private GoogleApiClient mApiClient;
    private static final int REQ_CODE=9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        mFacebook=(LoginButton)findViewById(R.id.main_fblogin_btn);
        mGoogle=(SignInButton) findViewById(R.id.main_googlelogin_btn);
        mGoogleOut=(Button)findViewById(R.id.main_googlesignout_btn);
        mImageView=findViewById(R.id.main_dpImageView);
        mTextView=findViewById(R.id.main_metadata_textView);
        //Print keyhash of the app to add to facebook
          //printKeyHash();
        mCallbackManager = CallbackManager.Factory.create();
       mFacebook.setReadPermissions(Arrays.asList("public_profile","email","user_birthday","user_friends"));
       mFacebook.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
           @Override
           public void onSuccess(LoginResult loginResult) {
               mProgressDialog=new ProgressDialog(MainActivity.this);
               mProgressDialog.setMessage("fetching Data");
               mProgressDialog.show();

                  String accessToken = loginResult.getAccessToken().getToken();
               GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                   @Override
                   public void onCompleted(JSONObject object, GraphResponse response) {
                                   mProgressDialog.dismiss();
                       Log.d("response", response.toString());
                                   getdata(object);
                   }
               });
               //Request Graph API
               Bundle parameter =new Bundle();
               parameter.putString("fields","id,email,birthday,friends");
               request.setParameters(parameter);
               request.executeAsync();

           }

           @Override
           public void onCancel() {

           }

           @Override
           public void onError(FacebookException error) {

           }
       });

       //If already login(Facebook)
           if(AccessToken.getCurrentAccessToken() != null){
               Toast.makeText(this, AccessToken.getCurrentAccessToken().getUserId(), Toast.LENGTH_SHORT).show();
           }
           //ClickListners
        mFacebook.setOnClickListener(this);
        mGoogle.setOnClickListener(this);
        mGoogleOut.setOnClickListener(this);
        ////Google sign options
        GoogleSignInOptions signInOptions=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        mApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this,this).addApi(Auth.GOOGLE_SIGN_IN_API,signInOptions).build();
    }//End of onCreate

    private void getdata(JSONObject object) {
        try{
            URL profile_picture=    new URL("https://graph.facebook.com/"+object.getString("id")+"/picture?width=250&height=250");
            Glide.with(this).load(profile_picture.toString()).into(mImageView);
           mTextView.setText("Inside get");
            mTextView.setText(object.getString("email")+"\n"+object.getString("birthday")+"\n"+"Friends:"+object.getJSONObject("friends").getJSONObject("summary").getString("total_count"));

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    //Override onActivityResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode,resultCode,data);
        if(requestCode== REQ_CODE){
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleResult(result);
        }
    }

    private void printKeyHash() {
          try{
              PackageInfo info=getPackageManager().getPackageInfo("org.akshanshgusain.social_login", PackageManager.GET_SIGNATURES);
               for(Signature signature:info.signatures){
                   MessageDigest md=MessageDigest.getInstance("SHA");
                   md.update(signature.toByteArray());
                   Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT));
               }
          } catch (PackageManager.NameNotFoundException e) {
              e.printStackTrace();
          } catch (NoSuchAlgorithmException e) {
              e.printStackTrace();
          }
    }

    @Override
    public void onClick(View v) {
                switch(v.getId()){
                    case R.id.main_fblogin_btn: facebooklogin();
                                                                  break;
                    case R.id.main_googlelogin_btn: googlelogin();
                                                                  break;
                    case R.id.main_googlesignout_btn: googleSignOut();
                                                                   break;
                    default:
                        Toast.makeText(this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                }

    }



    private void googlelogin() {
                        Intent intent=Auth.GoogleSignInApi.getSignInIntent(mApiClient);
                        startActivityForResult(intent,REQ_CODE);
    }

    private void googleSignOut()
    {
    Auth.GoogleSignInApi.signOut(mApiClient).setResultCallback(new ResultCallback<Status>() {
        @Override
        public void onResult(@NonNull Status status) {
            updateUI(false);
        }
    });
    }

    private  void handleResult(GoogleSignInResult result){
            if(result.isSuccess())
             {
                 GoogleSignInAccount account=result.getSignInAccount();
                 String name= account.getDisplayName();
                 String email=account.getEmail();
                 String imageURL=account.getPhotoUrl().toString();
                 Glide.with(this).load(imageURL).into(mImageView);
                 mTextView.setText(name+"\n"+email+"\n");
                 updateUI(true);
             }
             else{
                updateUI(false);
            }


    }
    private void updateUI(boolean isLogin){

    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private void facebooklogin() {

    }
}

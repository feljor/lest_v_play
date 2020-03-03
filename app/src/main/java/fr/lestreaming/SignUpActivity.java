package fr.lestreaming;

import android.app.ProgressDialog;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.analytics.FirebaseAnalytics;
import fr.lestreaming.R;

import fr.lestreaming.utils.ApiResources;
import fr.lestreaming.utils.ToastMsg;
import fr.lestreaming.utils.VolleySingleton;

import org.json.JSONException;
import org.json.JSONObject;

public class SignUpActivity extends AppCompatActivity {

    private EditText etName,etEmail,etPass;
    private ImageButton btnSignup;
    private Button btnSignin;
    private ProgressDialog dialog;
    private RelativeLayout backgorundView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPreferences sharedPreferences = getSharedPreferences("push", MODE_PRIVATE);
//        boolean isDark = sharedPreferences.getBoolean("dark", false);
//
//        if (isDark) {
//            setTheme(R.style.AppThemeDark);
//        } else {
//            setTheme(R.style.AppThemeLight);
//        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        Toolbar toolbar = findViewById(R.id.toolbar);

//        if (!isDark) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
//        }

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.signup));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //---analytics-----------
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "id");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "sign_up_activity");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "activity");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.wait));
        dialog.setCancelable(false);

        etName=findViewById(R.id.name);
        etEmail=findViewById(R.id.email);
        etPass=findViewById(R.id.password);
        btnSignup=findViewById(R.id.signup);
        btnSignin=findViewById(R.id.signin);
        backgorundView=findViewById(R.id.background_view);
//        if (isDark) {
//            backgorundView.setBackgroundColor(getResources().getColor(R.color.nav_head_bg));
//            btnSignup.setBackground(getResources().getDrawable(R.drawable.btn_rounded_dark));
//        }
        btnSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                finish();
            }
        });
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isValidEmailAddress(etEmail.getText().toString())){
                    new ToastMsg(SignUpActivity.this).toastIconError(getString(R.string.invalid_email));
                }else if(etPass.getText().toString().equals("")){
                    new ToastMsg(SignUpActivity.this).toastIconError(getString(R.string.invalid_pass));
                }else if (etName.getText().toString().equals("")){
                    new ToastMsg(SignUpActivity.this).toastIconError(getString(R.string.invalid_name));
                }else {
                    String email = "&&email="+etEmail.getText().toString();
                    String pass = "&&password="+etPass.getText().toString();
                    String name = "&&name="+etName.getText().toString();
                    String url = new ApiResources().getSignup()+email+pass+name;
                    signUp(url);
                }
            }
        });
    }

    private void signUp(String url){
        dialog.show();
        url = url.replaceAll(" ", "%20");
        JsonObjectRequest jsonObjectRequest=new JsonObjectRequest(Request.Method.GET,url,null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                dialog.cancel();
                try {
                    Log.e("SIGN UP RES:::::", String.valueOf(response));
                    if (response.getString("status").equals("success")){
                        new ToastMsg(SignUpActivity.this).toastIconSuccess(getString(R.string.signup_success));

                        // save user info to sharedPref
                        saveUserInfo(response.getString("name"), etEmail.getText().toString(),
                                response.getString("user_id"));

                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                        finish();
                    }else if (response.getString("status").equals("error")){
                        new ToastMsg(SignUpActivity.this).toastIconError(response.getString("data"));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.cancel();
                new ToastMsg(SignUpActivity.this).toastIconError(getString(R.string.error_toast));
            }
        });

        new VolleySingleton(SignUpActivity.this).addToRequestQueue(jsonObjectRequest);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }

    public void saveUserInfo(String name, String email, String id) {
        SharedPreferences.Editor editor = getSharedPreferences("user", MODE_PRIVATE).edit();
        editor.putString("name", name);
        editor.putString("email", email);
        editor.putString("id", id);
        editor.putBoolean("status",true);
        editor.apply();
    }


}

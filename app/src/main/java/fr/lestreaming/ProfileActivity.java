package fr.lestreaming;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.analytics.FirebaseAnalytics;
import fr.lestreaming.R;

import fr.lestreaming.nav_fragments.FavoriteFragment;
import fr.lestreaming.network.RetrofitClient;
import fr.lestreaming.network.apis.DeactivateAccountApi;
import fr.lestreaming.network.apis.ProfileApi;
import fr.lestreaming.network.model.ResponseStatus;
import fr.lestreaming.utils.ApiResources;
import fr.lestreaming.utils.FileUtil;
import fr.lestreaming.utils.ToastMsg;
import fr.lestreaming.utils.VolleySingleton;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;

public class ProfileActivity extends AppCompatActivity {


    private TextView userName, userEmail, updateProf;
    private EditText userPass;
    private Button btnLogout;
    private CoordinatorLayout coordinatorLayout;
    private RelativeLayout relativeLayout;
    private LinearLayout myFavorite, deactivateBt;
    private ProgressDialog dialog;
    private String URL="",strGender;
    private CircleImageView userImage;
    private static final int GALLERY_REQUEST_CODE = 1;
    private Uri imageUri;
    private ProgressBar progressBar;
    private String id;
//    boolean isDark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_test);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences sharedPreferences = getSharedPreferences("push", MODE_PRIVATE);
   /*     isDark = sharedPreferences.getBoolean("dark", false);

        if (isDark) {
            setTheme(R.style.AppThemeDark);
        } else {
            setTheme(R.style.AppThemeLight);
        }*/

        //---analytics-----------
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "id");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "profile_activity");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "activity");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.fetch_data));
        dialog.setCancelable(false);

        userName=findViewById(R.id.name);
        userEmail=findViewById(R.id.email);
        userPass=findViewById(R.id.userPass);
        btnLogout=findViewById(R.id.logout_btn);
        userImage =findViewById(R.id.user_iv);
        progressBar =findViewById(R.id.progress_bar);
        deactivateBt =findViewById(R.id.deactive_bt);
        myFavorite=findViewById(R.id.favorite);
        updateProf=findViewById(R.id.edit_profile);
        relativeLayout=findViewById(R.id.relative_lyt);
        coordinatorLayout=findViewById(R.id.coordinator_lyt);


        SharedPreferences preferences=getSharedPreferences("user",MODE_PRIVATE);
        id = preferences.getString("id","0");

        updateProf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userEmail.getText().toString().equals("")){
                    Toast.makeText(ProfileActivity.this,getString(R.string.invalid_email),Toast.LENGTH_LONG).show();
                    return;
                }else if (userName.getText().toString().equals("")){
                    Toast.makeText(ProfileActivity.this,getString(R.string.invalid_name),Toast.LENGTH_LONG).show();
                    return;
                }
                final ProgressDialog progressDialog = new ProgressDialog(ProfileActivity.this, ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(false);
                progressDialog.setMessage(getString(R.string.wait));
                progressDialog.show();

                String email = userEmail.getText().toString();
                String pass = userPass.getText().toString();
                String name = userName.getText().toString();

                updateProfile(id, email, name, pass, progressDialog);

            }
        });
        String urlProfile = new ApiResources().getProfileURL()+preferences.getString("email","null");


        getProfile(urlProfile);

    }

    private void getFragment(){

        getActionBar().setTitle(getString(R.string.my_fav));
        Fragment fragment = new FavoriteFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, fragment.getClass().getSimpleName())
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();

        userImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                openGallery();

            }
        });

        deactivateBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showDeactiveDialog();

            }
        });

        myFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                coordinatorLayout.setVisibility(View.GONE);
                relativeLayout.setVisibility(View.VISIBLE);
                getFragment();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                logoutUser();
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                finish();
            }
        });

       /* updateProf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                editProfile();
            }
        });*/

    }

    private void editProfile() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_edit_profile, null);
        builder.setView(view);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        final CircleImageView profileImage = view.findViewById(R.id.profileImage);
        final AutoCompleteTextView profileName = view.findViewById(R.id.name);
        final AutoCompleteTextView profileEmail = view.findViewById(R.id.email);
        final EditText profilePass = view.findViewById(R.id.profilePassword);
        final Button okBt = view.findViewById(R.id.ok_bt);
        Button cancelBt = view.findViewById(R.id.cancel_bt);
        ImageView closeIv = view.findViewById(R.id.close_iv);
//        final ProgressBar progressBar = view.findViewById(R.id.progress_bar);
        LinearLayout topLayout = view.findViewById(R.id.top_layout);
//        if (isDark) {
//            topLayout.setBackgroundColor(getResources().getColor(R.color.overlay_dark_30));
//        }

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        okBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (profileEmail.getText().toString().equals("")){
                    Toast.makeText(ProfileActivity.this,getString(R.string.invalid_email),Toast.LENGTH_LONG).show();
                    return;
                }else if (profileName.getText().toString().equals("")){
                    Toast.makeText(ProfileActivity.this,getString(R.string.invalid_name),Toast.LENGTH_LONG).show();
                    return;
                }

                final ProgressDialog progressDialog = new ProgressDialog(ProfileActivity.this, ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(false);
                progressDialog.setMessage(getString(R.string.wait));
                progressDialog.show();
//                progressBar.setVisibility(View.VISIBLE);

                String email = profileEmail.getText().toString();
                String pass = profilePass.getText().toString();
                String name = profileName.getText().toString();

                updateProfile(id, email, name, pass, progressDialog);

            }
        });

        cancelBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        closeIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });


    }

    private void showDeactiveDialog() {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_deactivate, null);
        builder.setView(view);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        final EditText passEt = view.findViewById(R.id.pass_et);
        final EditText reasonEt = view.findViewById(R.id.reason_et);
        final Button okBt = view.findViewById(R.id.ok_bt);
        Button cancelBt = view.findViewById(R.id.cancel_bt);
        ImageView closeIv = view.findViewById(R.id.close_iv);
        final ProgressBar progressBar = view.findViewById(R.id.progress_bar);
        LinearLayout topLayout = view.findViewById(R.id.top_layout);
//        if (isDark) {
//            topLayout.setBackgroundColor(getResources().getColor(R.color.overlay_dark_30));
//        }

        okBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pass = passEt.getText().toString();
                String reason = reasonEt.getText().toString();

                if (TextUtils.isEmpty(pass)) {
                    new ToastMsg(ProfileActivity.this).toastIconError(getString(R.string.enter_password));
                    return;
                } else if(TextUtils.isEmpty(reason)) {
                    new ToastMsg(ProfileActivity.this).toastIconError(getString(R.string.reason_to_deactivate_account));
                    return;
                }
                deactivateAccount(pass, reason, alertDialog, progressBar);


            }
        });

        cancelBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        closeIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });


    }

    private void deactivateAccount(String pass, String reason, final AlertDialog alertDialog, final ProgressBar progressBar) {
        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        DeactivateAccountApi api = retrofit.create(DeactivateAccountApi.class);
        Call<ResponseStatus> call = api.deactivateAccount(id, pass, reason, Config.API_KEY);
        call.enqueue(new Callback<ResponseStatus>() {
            @Override
            public void onResponse(Call<ResponseStatus> call, retrofit2.Response<ResponseStatus> response) {

                if (response.code() == 200) {

                    ResponseStatus resStatus = response.body();

                    if (resStatus.equals("success")) {
                        logoutUser();
                        new ToastMsg(ProfileActivity.this).toastIconError(resStatus.getData());
                        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        alertDialog.dismiss();
                        finish();
                    } else {
                        new ToastMsg(ProfileActivity.this).toastIconError(resStatus.getData());
                    }



                } else {
                    new ToastMsg(ProfileActivity.this).toastIconError(getString(R.string.error_toast));
                    alertDialog.dismiss();
                }
            }

            @Override
            public void onFailure(Call<ResponseStatus> call, Throwable t) {
                t.printStackTrace();
                new ToastMsg(ProfileActivity.this).toastIconError(getString(R.string.error_toast));
                alertDialog.dismiss();
            }
        });

    }

    public void logoutUser() {
        SharedPreferences.Editor editor = getSharedPreferences("user", MODE_PRIVATE).edit();
        editor.putString("name", null);
        editor.putString("email", null);
        editor.putString("id",null);
        editor.putBoolean("status",false);
        editor.apply();
    }

    private void openGallery() {

        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto , GALLERY_REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            /*case 0:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = data.getData();
                    userIv.setImageURI(selectedImage);
                }
                break;*/
            case 1:
                if(resultCode == RESULT_OK){
                    Uri selectedImage = data.getData();
                    userImage.setImageURI(selectedImage);
                    imageUri = selectedImage;
                }
                break;
        }

    }


    private void getProfile(String url){

//        dialog.show();
        JsonObjectRequest jsonObjectRequest=new JsonObjectRequest(Request.Method.GET,url,null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
//                dialog.cancel();

                try {
                    Picasso.get().load(response.getString("image_url")).placeholder(R.drawable.ic_account_circle_black).into(userImage);
                    userName.setText(response.getString("name"));
                    userEmail.setText(response.getString("email"));
                    strGender = "&&gender="+response.getString("gender");


                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                dialog.cancel();
                Toast.makeText(ProfileActivity.this,getString(R.string.error_toast),Toast.LENGTH_LONG).show();
            }
        });

        new VolleySingleton(ProfileActivity.this).addToRequestQueue(jsonObjectRequest);


    }

    private void updateProfile(String idString, String emailString, String nameString, String passString, final ProgressDialog progressDialog){
        File file = null;
        RequestBody requestFile = null;
        MultipartBody.Part multipartBody = null;
        try {
            file = FileUtil.from(ProfileActivity.this, imageUri);
            requestFile = RequestBody.create(MediaType.parse("multipart/form-data"),
                    file);

            multipartBody = MultipartBody.Part.createFormData("photo",
                    file.getName(),requestFile);

        } catch (Exception e) {
            e.printStackTrace();
        }



        RequestBody email = RequestBody.create(MediaType.parse("text/plain"), emailString);
        RequestBody id = RequestBody.create(MediaType.parse("text/plain"), idString);
        RequestBody name = RequestBody.create(MediaType.parse("text/plain"), nameString);
        RequestBody password = RequestBody.create(MediaType.parse("text/plain"), passString);
        RequestBody key = RequestBody.create(MediaType.parse("text/plain"), Config.API_KEY);

        Retrofit retrofit = RetrofitClient.getRetrofitInstance();
        ProfileApi api = retrofit.create(ProfileApi.class);
        Call<ResponseBody> call = api.updateProfile(id, name, email, password, key, multipartBody);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.code() == 200) {
                    Toast.makeText(ProfileActivity.this, "Success.", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
//                    progressBar.setVisibility(View.GONE);
                } else {
                    Toast.makeText(ProfileActivity.this, getString(R.string.error_toast), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
//                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, getString(R.string.error_toast), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
//                progressBar.setVisibility(View.GONE);
                t.printStackTrace();
            }
        });

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
}

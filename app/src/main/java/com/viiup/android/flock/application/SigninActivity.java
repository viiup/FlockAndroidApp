package com.viiup.android.flock.application;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.joanzapata.iconify.Iconify;
import com.viiup.android.flock.helpers.CommonHelper;
import com.viiup.android.flock.services.IAsyncRequestResponse;
import com.viiup.android.flock.services.UserService;

public class SigninActivity extends AppCompatActivity {

    private TextView textViewCancel;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonSignin;
    private TextView textViewForgotPassword;
    private TextView textViewSignup;
    private ProgressDialog progressDialog;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        context = this;

        overridePendingTransition(R.anim.right_in, R.anim.right_out);

        setContentView(R.layout.signin_activity);

        textViewCancel = (TextView) findViewById(R.id.textViewCancel);
        textViewCancel.setText(Iconify.compute(this, getString(R.string.icon_fa_cancel)));
        textViewCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                finish();
                overridePendingTransition(R.anim.left_in, R.anim.left_out);
            }
        });
        editTextEmail = (EditText) findViewById(R.id.editTextEmail);
        editTextEmail.setText(R.string.fmt_email_domain);

        editTextPassword = (EditText) findViewById(R.id.editTextPassword);

        buttonSignin = (Button) findViewById(R.id.buttonSignin);
        buttonSignin.setOnClickListener(new SigninButtonClickHandler());

        textViewForgotPassword = (TextView) findViewById(R.id.textViewForgotPassword);
        textViewForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent passwordResetActivityIntent = new Intent(context, PasswordResetActivity.class);
                startActivity(passwordResetActivityIntent);
            }
        });

        textViewSignup = (TextView) findViewById(R.id.textViewSignup);
        textViewSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signupActivityIntent = new Intent(context, SignupActivity.class);
                startActivity(signupActivityIntent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.left_in, R.anim.left_out);
    }

    /*
        On click handler for the button click event for Sign In button.
     */
    private class SigninButtonClickHandler implements Button.OnClickListener, IAsyncRequestResponse {

        @Override
        public void onClick(View view) {

            InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null : getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            if (!CommonHelper.isEmailValid(context, editTextEmail.getText().toString()))
                editTextEmail.setError(getString(R.string.error_invalid_email));
            else if (editTextPassword.getText().toString().equals(""))
                editTextPassword.setError(getString(R.string.error_field_required));
            else {
                try {
                    progressDialog = ProgressDialog.show(context, getString(R.string.title_signin), getString(R.string.msg_processing_request));
                    UserService userService = new UserService();
                    userService.signin(editTextEmail.getText().toString(), editTextPassword.getText().toString(), this);
                }catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        @Override
        public void responseHandler(String authenticatedUserJson) {
            if (progressDialog != null) progressDialog.dismiss();

            if (authenticatedUserJson != null && authenticatedUserJson.length() > 0) {
                SharedPreferences mPref = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
                SharedPreferences.Editor mPrefsEditor = mPref.edit();

                mPrefsEditor.putString("authenticatedUserJson", authenticatedUserJson);
                mPrefsEditor.apply();

                Intent homeActivityIntent = new Intent(context, HomeActivity.class);
                homeActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(homeActivityIntent);
            } else {
                Toast.makeText(context, R.string.error_login_failed, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void backGroundErrorHandler(Exception ex) {

            if (progressDialog != null) progressDialog.dismiss();

            // Print stack trace...may be add logging in future releases
            ex.printStackTrace();

            // display error message
            Toast.makeText(context, R.string.error_login_failed, Toast.LENGTH_SHORT).show();
        }
    }
}

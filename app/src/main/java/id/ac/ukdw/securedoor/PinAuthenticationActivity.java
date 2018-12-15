package id.ac.ukdw.securedoor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.beautycoder.pflockscreen.PFFLockScreenConfiguration;
import com.beautycoder.pflockscreen.fragments.PFLockScreenFragment;
import com.beautycoder.pflockscreen.security.PFFingerprintPinCodeHelper;
import com.beautycoder.pflockscreen.security.PFSecurityException;

public class PinAuthenticationActivity extends AppCompatActivity {
    public static final int REQUEST_CODE = 100;
    public static final int AUTHENTICATED = 200;
    public static final int BACK_PRESSED = 500;
    public static final String AUTH_PIN = "auth_pin";

    private Context mContext;
    private Toolbar toolbarAuth;

    private SharedPreferences sp;
    private SharedPreferences.Editor spEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_authentication);
        setContext();

        sp = mContext.getSharedPreferences("secure-door", MODE_PRIVATE);
        spEdit = sp.edit();

        toolbarAuth = findViewById(R.id.toolbarAuth);
        setSupportActionBar(toolbarAuth);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        showPinAuthenticationFragment();
    }

    private void setContext() {
        mContext = this;
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent result = new Intent();
        setResult(PinAuthenticationActivity.BACK_PRESSED, result);

        finish();
        return super.onSupportNavigateUp();
    }

    /**
     * Event listener
     * --------------
     *
     * Create authentication PIN listener
     *
     */
    private PFLockScreenFragment.OnPFLockScreenCodeCreateListener mCodeCreateListener =
            new PFLockScreenFragment.OnPFLockScreenCodeCreateListener() {
                @Override
                public void onCodeCreated(String encodedCode) {
                    Toast.makeText(mContext, "PIN Created", Toast.LENGTH_LONG).show();

                    spEdit.putString(PinAuthenticationActivity.AUTH_PIN, encodedCode);
                    spEdit.commit();

                    showPinAuthenticationFragment();
                }
            };


    /**
     * Event listener
     * --------------
     *
     * PIN authentication login attempt listener
     *
     */
    private PFLockScreenFragment.OnPFLockScreenLoginListener mLoginListener =
            new PFLockScreenFragment.OnPFLockScreenLoginListener() {
                @Override
                public void onCodeInputSuccessful() {
                    Intent result = new Intent();

                    setResult(PinAuthenticationActivity.AUTHENTICATED, result);
                    //Back to main activity
                    finish();
                }

                @Override
                public void onFingerprintSuccessful() {
                    Intent result = new Intent();

                    setResult(PinAuthenticationActivity.AUTHENTICATED, result);
                    //Back to main activity
                    finish();
                }

                @Override
                public void onPinLoginFailed() {
                    popMessage(mContext, "Wrong PIN");
                }

                @Override
                public void onFingerprintLoginFailed() {
                    popMessage(mContext, "Fingerprint Authentication Failed");
                }
            };

    /**
     * Show the PIN Authentication view fragment
     *
     */
    private void showPinAuthenticationFragment() {
        try{
            final boolean isPinExist = PFFingerprintPinCodeHelper.getInstance().isPinCodeEncryptionKeyExist();

            //Set the lock screen configuration (view)
            final PFFLockScreenConfiguration.Builder builder = new PFFLockScreenConfiguration.Builder(mContext)
                    .setTitle(isPinExist ? "Insert Your Pin" : "Register PIN")
                    .setCodeLength(6)
                    .setClearCodeOnError(true)
                    .setLeftButton("Forgot Pin?", new View.OnClickListener() {
                        //If the user's forget their PIN
                        @Override
                        public void onClick(View v) {
                            //TODO: Move to forgot PIN activity
                            try {
                                PFFingerprintPinCodeHelper.getInstance().delete();

                                showPinAuthenticationFragment();
                            } catch (PFSecurityException e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .setUseFingerprint(true)
                    .setAutoShowFingerprint(true);

            //Define the PIN authentication fragment
            PFLockScreenFragment fragment = new PFLockScreenFragment();

            //Set the pin input mode
            builder.setMode(
                    isPinExist ?
                            PFFLockScreenConfiguration.MODE_AUTH
                            :
                            PFFLockScreenConfiguration.MODE_CREATE);

            if (isPinExist) { //Set the encoded PIN if the pin is exist
                fragment.setEncodedPinCode(sp.getString(PinAuthenticationActivity.AUTH_PIN, null));
                fragment.setLoginListener(mLoginListener);
            }

            //Put the pin authentication configuration (view) into the pin authentication fragment
            fragment.setConfiguration(builder.build());

            //Set the code created listener if there's no authentication PIN exist
            fragment.setCodeCreateListener(mCodeCreateListener);

            //Put the pin auth fragment to the activity
            getSupportFragmentManager().beginTransaction().replace(R.id.framePinAuthentication, fragment).commit();

        } catch (PFSecurityException e) {
            e.printStackTrace();
            popMessage(mContext, "Can't get PIN code info");
        }
    }

    /**
     * Define back button event handler
     *
     */
    @Override
    public void onBackPressed() {
        Intent result = new Intent();
        setResult(PinAuthenticationActivity.BACK_PRESSED, result);

        super.onBackPressed();
    }

    private void popMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}

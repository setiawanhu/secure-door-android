package id.ac.ukdw.securedoor;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

import id.ac.ukdw.securedoor.adapters.NotificationHelper;
import id.ac.ukdw.securedoor.model.ShiftrIO;
import id.ac.ukdw.securedoor.utils.StringUtil;

public class MainActivity extends AppCompatActivity {
    private static final String CLIENT_ID = "Android Client";

    private Context mContext;

    private MqttAndroidClient client;
    private MqttConnectOptions options;

    private TextView txtStatus;
    private LinearLayout layoutPin;
    private TextView txtPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setContext();

        setElements();
        hidePin();

        client = new MqttAndroidClient(this, ShiftrIO.MQTT_HOST, CLIENT_ID);
        setCredential();
        connectClient();

        client.setCallback(mqttCallback);
    }

    /**
     * Define all elements on view
     *
     */
    private void setElements() {
        txtStatus = findViewById(R.id.txtStatus);
        layoutPin = findViewById(R.id.layoutPin);
        txtPin = findViewById(R.id.txtPin);
    }

    /**
     * Set the activity mContext
     *
     */
    private void setContext() {
        mContext = this;
    }

    /**
     * Hide the generated PIN view
     *
     */
    private void hidePin() {
        layoutPin.setVisibility(View.GONE);
    }

    /**
     * Show the generated PIN view
     *
     */
    private void showPin() {
        layoutPin.setVisibility(View.VISIBLE);
    }

    /**
     * Set the shiftr.io client username and password
     *
     */
    private void setCredential() {
        options = new MqttConnectOptions();
        options.setUserName(ShiftrIO.USERNAME);
        options.setPassword(ShiftrIO.PASSWORD.toCharArray());
    }

    /**
     * Generate a connection to the shiftr.io broker
     *
     */
    private void connectClient() {
        try {
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Toast.makeText(mContext, "Connected to shiftr.io!", Toast.LENGTH_LONG).show();

                    subscribe(ShiftrIO.DOOR_TOPIC);
                    subscribe(ShiftrIO.PIN_TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Toast.makeText(mContext, "Oops, something's wrong", Toast.LENGTH_LONG).show();
                    exception.printStackTrace();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Subscribe to a specific topic
     *
     * @param topic
     */
    private void subscribe(String topic) {
        int qos = 1;

        try {
            IMqttToken subToken = client.subscribe(topic, qos);

            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //The topic is subscribed
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send a authenticate request to the door topic
     *
     */
    public void authenticateDoor(){
        String payload = "unlock";
        byte[] encodedPayload = new byte[0];

        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(ShiftrIO.LOCK_TOPIC, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Publish a message to /light topic
     *
     * @param v View
     */
    public void publishPin(View v) {
        String payload = StringUtil.getRandomNumberString();
        byte[] encodedPayload = new byte[0];

        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(ShiftrIO.PIN_TOPIC, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     * Share a generated PIN to another application
     *
     * @param v View
     */
    public void share(View v) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");

        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Security PIN");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, "Silahkan gunakan pin berikut untuk membuka pintu: " + txtPin.getText().toString());

        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }

    /**
     * PIN Authentication for opening the door
     *
     * @param v View
     */
    public void authenticate(View v) {
        Intent intent = new Intent(mContext, PinAuthenticationActivity.class);
        startActivityForResult(intent, PinAuthenticationActivity.REQUEST_CODE);
    }

    /**
     * Define the MQTT callback functions
     *
     */
    private MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            //connection lost callback
            Toast.makeText(mContext, "Connection lost.", Toast.LENGTH_LONG).show();
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            //message retrieved callback
            switch (topic) {
                case ShiftrIO.PIN_TOPIC:
                    txtPin.setText(new String(message.getPayload()));
                    showPin();

                    break;
                case ShiftrIO.DOOR_TOPIC:
                    String payload = new String(message.getPayload());

                    if (payload.equals("opened")) {
                        txtStatus.setTextColor(mContext.getResources().getColor(R.color.colorPrimary));
                    } else if(payload.equals("closed")) {
                        txtStatus.setTextColor(mContext.getResources().getColor(R.color.colorAccent));
                    } else if (payload.equals("emergency")) {
                        NotificationHelper notification = new NotificationHelper(mContext, 500);

                        notification.buildNotification("Emergency", "Someone breach your home");
                        notification.send();
                    }

                    txtStatus.setText(payload.toUpperCase());
                    break;
            }

            //or set notification here
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            //Publish sent callback
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PinAuthenticationActivity.REQUEST_CODE:
                if (resultCode == PinAuthenticationActivity.AUTHENTICATED) {
                    authenticateDoor();
                } else if (resultCode == PinAuthenticationActivity.BACK_PRESSED) {
                    Toast.makeText(mContext, "Not authenticated", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}

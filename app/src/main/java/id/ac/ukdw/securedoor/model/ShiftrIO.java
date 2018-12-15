package id.ac.ukdw.securedoor.model;

public class ShiftrIO {
    //reference for the MQTT_HOST: https://docs.shiftr.io/interfaces/mqtt/
    //no need to define the project name it'll defined in the setCredential() function
    public static final String MQTT_HOST = "tcp://broker.shiftr.io:1883";

    //the broker credentials
    public static final String USERNAME = "securedoor";
    public static final String PASSWORD = "thisisthesecret";

    //the available topics
    public static final String DOOR_TOPIC = "/door";
    public static final String LOCK_TOPIC = "/lock";
    public static final String PIN_TOPIC = "/pin";
}

package edu.info502.mosquitsub;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class Subscriber implements MqttCallback {

    public static final String HOST = "tcp://127.0.0.1:1883";
    public static final String TOPIC = "INFO0502";
    public static final String CLIENT_ID = "subscriber"; 

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(HOST, CLIENT_ID);
            

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            

            client.setCallback(new Subscriber());
            
            client.connect(options);
            
            client.subscribe(TOPIC);
            System.out.println("Subscribed au topic: " + TOPIC);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        System.out.println("Message recu du topic: " + topic);
        System.out.println("contenu: " + new String(message.getPayload()));
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Connexion perdu! Cause: " + cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}

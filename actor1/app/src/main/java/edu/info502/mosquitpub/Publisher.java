package edu.info502.mosquitpub;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class Publisher {

    public static final String HOST = "tcp://10.11.18.72:1883";
    public static final String TOPIC = "INFO0502";
    public static final String CLIENT_ID = "publisher";

    public static void main(String[] args) {
        try {
            MqttClient client = new MqttClient(HOST, CLIENT_ID);
            client.connect();
            String messageText = "Bonjour, MQTT!";
            MqttMessage message = new MqttMessage(messageText.getBytes());
            client.publish(TOPIC, message);
            client.disconnect();
            client.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}

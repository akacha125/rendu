package edu.info0502.pocker;

import java.util.Scanner;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;

public class Subscriber {

    private static final String MQTT_BROKER = "tcp://localhost:1883";
    private static final String GAME_TOPIC = "poker/game";
    private static final String PLAYER_TOPIC_PREFIX = "poker/player/";

    private final MqttClient mqttClient;
    private final Gson gson = new Gson();
    private String playerName;

    public Subscriber() throws MqttException {
        mqttClient = new MqttClient(MQTT_BROKER, MqttClient.generateClientId(), null);
        mqttClient.connect();
        mqttClient.setCallback(new MqttCallbackAdapter());
    }

    public void start() throws MqttException {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Entrez votre nom de joueur:");
            playerName = scanner.nextLine();

            registerPlayer();

            mqttClient.subscribe(PLAYER_TOPIC_PREFIX.concat(playerName));

            System.out.println("Vous êtes maintenant connecté. Utilisez les commandes : START pour démarrer le jeu, ou QUIT pour quitter.");

            while (true) {
                String command = scanner.nextLine();
                if (command.equalsIgnoreCase("QUIT")) {
                    disconnect();
                    break;
                }

                if (command.equalsIgnoreCase("START")) {
                    sendStartGameCommand();
                } else {
                    System.out.println("Commande non reconnue.");
                }
            }
        }
    }

    private void registerPlayer() {
        MessagePayload payload = new MessagePayload("REGISTER");
        payload.getData().put("player", playerName);

        sendMessage(GAME_TOPIC, payload);
    }

    private void sendStartGameCommand() {
        MessagePayload payload = new MessagePayload("START_GAME");
        payload.getData().clear(); 
        sendMessage(GAME_TOPIC, payload);
    }

    private void sendMessage(String topic, MessagePayload payload) {
        try {
            String jsonMessage = gson.toJson(payload);
            System.out.println("Message JSON à envoyer: " + jsonMessage);
            if (!jsonMessage.contains("\"data\":{")) {
                System.err.println("ATTENTION: Le message n'a pas le bon format JSON!");
                return;
            }
            mqttClient.publish(topic, new MqttMessage(jsonMessage.getBytes()));
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void disconnect() {
        try {
            mqttClient.disconnect();
            System.out.println("Déconnecté du serveur.");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private class MqttCallbackAdapter implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
            System.out.println("Connexion au serveur perdue.");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            System.out.println("Message reçu: " + new String(message.getPayload()));
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }

    private static class MessagePayload {

        private final String type;
        private final java.util.Map<String, String> data;

        public MessagePayload(String type) {
            this.type = type;
            this.data = new java.util.HashMap<>(); 
        }

        public String getType() {
            return type;
        }

        public java.util.Map<String, String> getData() {
            return data;
        }
    }

    public static void main(String[] args) {
        try {
            Subscriber client = new Subscriber();
            client.start();
        } catch (MqttException e) {
            System.err.println("Erreur de connexion au serveur MQTT.");
            e.printStackTrace();
        }
    }
}

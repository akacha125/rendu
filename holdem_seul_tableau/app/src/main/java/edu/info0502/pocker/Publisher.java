package edu.info0502.pocker;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Publisher {

    private static final String MQTT_BROKER = "tcp://localhost:1883";
    private static final String GAME_TOPIC = "poker/game";
    private static final String PLAYER_TOPIC_PREFIX = "poker/player/";

    private final Map<String, String> playerTopics = new ConcurrentHashMap<>();
    private final MqttClient mqttClient;
    private final Gson gson = new Gson();

    private PokerHoldem currentGame;
    private boolean gameInProgress = false;

    public Publisher() throws MqttException {
        mqttClient = new MqttClient(MQTT_BROKER, MqttClient.generateClientId(), null); // Add null persistence
        MqttConnectOptions options = new MqttConnectOptions();
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);
        options.setMaxInflight(100); // Increase max in-flight messages
        mqttClient.connect(options);
        mqttClient.setCallback(new MqttCallbackAdapter());
        mqttClient.subscribe(GAME_TOPIC);
        System.out.println("Serveur MQTT prêt !");
    }

    public void start() {
        System.out.println("Serveur de poker démarré !");
    }

    private void handleIncomingMessage(String topic, String message) {
        try {
            if (!message.trim().startsWith("{")) {
                System.out.println("Message:  " + message);
                return;
            }
            MessagePayload payload = gson.fromJson(message, MessagePayload.class);
            System.out.println("Message reçu: " + message);

            switch (payload.getType()) {
                case "REGISTER":
                    handleRegister(payload);
                    break;
                case "START_GAME":
                    handleStartGame(payload);
                    break;
                default:
                    System.out.println("Commande non reconnue: " + payload.getType());
            }
        } catch (JsonSyntaxException e) {
            System.err.println("Erreur de syntaxe JSON: " + e.getMessage());
            System.err.println("Message reçu: " + message); 
            e.printStackTrace();
        }
    }

    private void handleRegister(MessagePayload payload) {
        String playerName = payload.getData().get("player");
        String playerTopic = PLAYER_TOPIC_PREFIX + playerName;

        if (playerTopics.containsKey(playerName)) {
            sendMessage(GAME_TOPIC, "Le joueur " + playerName + " est déjà enregistré.");
            return;
        }

        playerTopics.put(playerName, playerTopic);
        sendMessage(playerTopic, "Bienvenue " + playerName);

        System.out.println("Joueur enregistré: " + playerName);
    }

    private void handleStartGame(MessagePayload payload) {
        if (gameInProgress) {
            sendMessage(GAME_TOPIC, "Une partie est déjà en cours.");
            return;
        }

        if (playerTopics.size() < 2) {
            sendMessage(GAME_TOPIC, "Il faut au moins 2 joueurs pour commencer.");
            return;
        }

        try {
            gameInProgress = true;
            currentGame = new PokerHoldem(new ArrayList<>(playerTopics.keySet()));

            broadcastMessage("La partie commence !");
            currentGame.demarrerPartie();

            playerTopics.forEach((player, topic) -> {
                Joueur joueur = currentGame.getJoueurParNom(player);
                sendMessage(topic, "Vos cartes: " + joueur.getCartesPrivees());
            });

            distributeFlop();
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage de la partie : " + e.getMessage());
            e.printStackTrace();
            gameInProgress = false; // réinitialisation en cas d'échec
        }
    }

    private void distributeFlop() {
        currentGame.distribuerFlop();
        broadcastMessage("Flop: " + currentGame.getCartesCommunes());
        distributeTurn();
    }

    private void distributeTurn() {
        currentGame.distribuerTurn();
        broadcastMessage("Turn: " + currentGame.getCartesCommunes());
        distributeRiver();
    }

    private void distributeRiver() {
        currentGame.distribuerRiver();
        broadcastMessage("River: " + currentGame.getCartesCommunes());
        showResults();
    }

    private void showResults() {
        Map<String, String> results = currentGame.calculerResultats();
        results.forEach((player, result) -> sendMessage(playerTopics.get(player), "Résultat: " + result));

        String winner = currentGame.determinerGagnant();
        broadcastMessage("Le gagnant est: " + winner);
        results.forEach((player, result) -> sendMessage(playerTopics.get(player), "Le gagnant est:  " + winner));

        endGame();
    }

    private void endGame() {
        gameInProgress = false;
        currentGame = null;
        broadcastMessage("La partie est terminée.");
    }

    private void sendMessage(String topic, String message) {
        try {
            if (!mqttClient.isConnected()) {
                System.out.println("Client MQTT déconnecté, tentative de reconnexion...");
                mqttClient.reconnect();
            }
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1); // Add QoS level
            mqttClient.publish(topic, mqttMessage);
        } catch (MqttException e) {
            System.err.println("Erreur lors de l'envoi du message au topic " + topic + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void broadcastMessage(String message) {
        sendMessage(GAME_TOPIC, message);
    }

    public static void main(String[] args) {
        try {
            Publisher server = new Publisher();
            server.start();
        } catch (MqttException e) {
            System.err.println("Erreur lors du démarrage du serveur : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private class MqttCallbackAdapter implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
            System.out.println("Connexion MQTT perdue : " + cause.getMessage());
            cause.printStackTrace();

            // Tentative de reconnexion
            while (!mqttClient.isConnected()) {
                try {
                    System.out.println("Tentative de reconnexion au broker MQTT...");
                    mqttClient.reconnect();
                    System.out.println("Reconnecté au broker MQTT !");
                    mqttClient.subscribe(GAME_TOPIC); 
                } catch (MqttException e) {
                    System.err.println("Échec de reconnexion : " + e.getMessage());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            handleIncomingMessage(topic, new String(message.getPayload()));
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }

    private static class MessagePayload {

        private String type;
        private Map<String, String> data;

        public String getType() {
            return type;
        }

        public Map<String, String> getData() {
            return data;
        }
    }
}

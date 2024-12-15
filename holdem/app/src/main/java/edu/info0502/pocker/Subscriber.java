package edu.info0502.pocker;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;

public class Subscriber {
    private static final String MQTT_BROKER = "tcp://10.11.18.72:1883";
    private static final String GAME_TOPIC = "poker/game/#";
    private static final String PLAYER_TOPIC_PREFIX = "poker/player/";
    
    private final MqttClient mqttClient;
    private final String playerName;
    private final Gson gson = new Gson();
    private String currentTableId = null;
    private final Scanner scanner = new Scanner(System.in);

    public Subscriber(String playerName) throws MqttException {
        this.playerName = playerName;
        this.mqttClient = new MqttClient(MQTT_BROKER, 
            "client-" + playerName + "-" + MqttClient.generateClientId(), null);
        
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        
        mqttClient.connect(options);
        mqttClient.setCallback(new MqttCallbackAdapter());
        
        // subscribe a une topic spécifique
        mqttClient.subscribe(GAME_TOPIC);
        mqttClient.subscribe(PLAYER_TOPIC_PREFIX + playerName);
        
        System.out.println("Client connecté pour le joueur: " + playerName);
    }

    public void start() {
        boolean running = true;
        while (running) {
            displayMenu();
            String choice = scanner.nextLine().trim();
            
            try {
                switch (choice) {
                    case "1":
                        createTable();
                        break;
                    case "2":
                        listTables();
                        break;
                    case "3":
                        joinTable();
                        break;
                    case "4":
                        startGame();
                        break;
                    case "5":
                        closeTable();
                        break;
                    case "6":
                        running = false;
                        break;
                    default:
                        System.out.println("Option invalide");
                }
            } catch (Exception e) {
                System.err.println("Erreur: " + e.getMessage());
            }
        }
        
        disconnect();
    }

    private void displayMenu() {
        System.out.println("\n=== Menu Poker ===");
        System.out.println("1. Créer une table");
        System.out.println("2. Lister les tables");
        System.out.println("3. Rejoindre une table");
        System.out.println("4. Démarrer une partie");
        System.out.println("5. Fermer la table");
        System.out.println("6. Quitter");
        System.out.print("Votre choix: ");
    }

    private void createTable() throws MqttException {
        Map<String, String> data = new HashMap<>();
        data.put("player", playerName);
        
        MessagePayload payload = new MessagePayload("CREATE_TABLE", data);
        publishMessage("poker/game", payload);
    }

    private void listTables() throws MqttException {
        Map<String, String> data = new HashMap<>();
        data.put("player", playerName);
        
        MessagePayload payload = new MessagePayload("LIST_TABLES", data);
        publishMessage("poker/game", payload);
    }

    private void joinTable() throws MqttException {
        System.out.print("Entrez l'ID de la table à rejoindre: ");
        String tableId = scanner.nextLine().trim();
        
        Map<String, String> data = new HashMap<>();
        data.put("player", playerName);
        data.put("tableId", tableId);
        
        MessagePayload payload = new MessagePayload("JOIN_TABLE", data);
        publishMessage("poker/game", payload);
        
        currentTableId = tableId;
    }

    private void startGame() throws MqttException {
        if (currentTableId == null) {
            System.out.println("Vous n'êtes à aucune table");
            return;
        }
        
        Map<String, String> data = new HashMap<>();
        data.put("player", playerName);
        
        MessagePayload payload = new MessagePayload("START_GAME", data);
        publishMessage("poker/game", payload);
    }

    private void closeTable() throws MqttException {
        if (currentTableId == null) {
            System.out.println("Vous n'êtes à aucune table");
            return;
        }
        
        Map<String, String> data = new HashMap<>();
        data.put("player", playerName);
        data.put("tableId", currentTableId);
        
        MessagePayload payload = new MessagePayload("CLOSE_TABLE", data);
        publishMessage("poker/game", payload);
        
        currentTableId = null;
    }

    private void publishMessage(String topic, MessagePayload payload) throws MqttException {
        String json = gson.toJson(payload);
        MqttMessage message = new MqttMessage(json.getBytes());
        message.setQos(1);
        mqttClient.publish(topic, message);
    }

    private void disconnect() {
        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
            }
            scanner.close();
        } catch (MqttException e) {
            System.err.println("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }

    private class MqttCallbackAdapter implements MqttCallback {
        @Override
        public void connectionLost(Throwable cause) {
            System.out.println("Connexion perdue. Tentative de reconnexion...");
            while (!mqttClient.isConnected()) {
                try {
                    mqttClient.reconnect();
                    mqttClient.subscribe(GAME_TOPIC);
                    mqttClient.subscribe(PLAYER_TOPIC_PREFIX + playerName);
                    System.out.println("Reconnecté !");
                    break;
                } catch (MqttException e) {
                    System.err.println("Échec de reconnexion: " + e.getMessage());
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
            String content = new String(message.getPayload());
        
            if (topic.startsWith("poker/game")) {
                System.out.println("🎮 " + content);
            } 
            else if (topic.equals(PLAYER_TOPIC_PREFIX + playerName)) {
                System.out.println("📨 Message personnel: " + content);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {}
    }

    private static class MessagePayload {
        private final String type;
        private final Map<String, String> data;

        public MessagePayload(String type, Map<String, String> data) {
            this.type = type;
            this.data = data;
        }
    }

    public static void main(String[] args) {
        try {
            System.out.print("Entrez votre nom: ");
            Scanner scanner = new Scanner(System.in);
            String playerName = scanner.nextLine().trim();
            
            Subscriber client = new Subscriber(playerName);
            client.start();
        } catch (MqttException e) {
            System.err.println("Erreur lors du démarrage du client: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
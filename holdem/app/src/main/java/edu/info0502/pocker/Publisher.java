package edu.info0502.pocker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
    private static final String GAME_TOPIC = "poker/game/#";  // pour tous le games topics
    private static final String TABLE_TOPIC_PREFIX = "poker/game/table/";
    private static final String PLAYER_TOPIC_PREFIX = "poker/player/";
    private static final int MAX_PLAYERS_PER_TABLE = 6;

    private final Map<String, PokerTable> tables = new ConcurrentHashMap<>();
    private final Map<String, String> playerTableMapping = new ConcurrentHashMap<>();
    private final MqttClient mqttClient;
    private final Gson gson = new Gson();

    public Publisher() throws MqttException {
        mqttClient = new MqttClient(MQTT_BROKER, MqttClient.generateClientId(), null);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setKeepAliveInterval(60);
        options.setAutomaticReconnect(true);
        mqttClient.connect(options);
        mqttClient.setCallback(new MqttCallbackAdapter());
        mqttClient.subscribe(GAME_TOPIC);
        System.out.println("Serveur MQTT multi-tables prêt !");
    }

    private class PokerTable {
        private final String tableId;
        private final String adminPlayer;
        private final Set<String> players;
        private PokerHoldem currentGame;
        private boolean gameInProgress;

        public PokerTable(String tableId, String adminPlayer) {
            this.tableId = tableId;
            this.adminPlayer = adminPlayer;
            this.players = new HashSet<>();
            this.players.add(adminPlayer);
            this.gameInProgress = false;
        }
    }

    private void handleIncomingMessage(String topic, String message) {
        try {
            if (!message.trim().startsWith("{")) {
                System.out.println("Message: " + message);
                return;
            }
            MessagePayload payload = gson.fromJson(message, MessagePayload.class);
            System.out.println("Message reçu: " + message);

            switch (payload.getType()) {
                case "CREATE_TABLE":
                    handleCreateTable(payload);
                    break;
                case "JOIN_TABLE":
                    handleJoinTable(payload);
                    break;
                case "START_GAME":
                    handleStartGame(payload);
                    break;
                case "CLOSE_TABLE":
                    handleCloseTable(payload);
                    break;
                case "LIST_TABLES":
                    handleListTables(payload);
                    break;
                default:
                    System.out.println("Commande non reconnue: " + payload.getType());
            }
        } catch (JsonSyntaxException e) {
            System.err.println("Erreur de syntaxe JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleCreateTable(MessagePayload payload) {
        String playerName = payload.getData().get("player");
        String tableId = UUID.randomUUID().toString().substring(0, 8);
        
        PokerTable table = new PokerTable(tableId, playerName);
        tables.put(tableId, table);
        playerTableMapping.put(playerName, tableId);

        
        try {
            mqttClient.subscribe(TABLE_TOPIC_PREFIX + tableId);
        } catch (MqttException e) {
            System.err.println("Erreur lors de la souscription à la table: " + e.getMessage());
        }

        sendMessage(PLAYER_TOPIC_PREFIX + playerName, 
                   "Table créée avec succès. Vous êtes l'administrateur de la table " + tableId);
        broadcastTablesList();
    }

    private void handleJoinTable(MessagePayload payload) {
        String playerName = payload.getData().get("player");
        String tableId = payload.getData().get("tableId");
        
        PokerTable table = tables.get(tableId);
        if (table == null) {
            sendMessage(PLAYER_TOPIC_PREFIX + playerName, "Table introuvable");
            return;
        }

        if (table.gameInProgress) {
            sendMessage(PLAYER_TOPIC_PREFIX + playerName, "Une partie est en cours sur cette table");
            return;
        }

        if (table.players.size() >= MAX_PLAYERS_PER_TABLE) {
            sendMessage(PLAYER_TOPIC_PREFIX + playerName, "Table complète");
            return;
        }

        table.players.add(playerName);
        playerTableMapping.put(playerName, tableId);
        sendMessage(PLAYER_TOPIC_PREFIX + playerName, "Vous avez rejoint la table " + tableId);
        broadcastToTable(tableId, playerName + " a rejoint la table");
    }

    private void handleStartGame(MessagePayload payload) {
        String playerName = payload.getData().get("player");
        String tableId = playerTableMapping.get(playerName);
        
        if (tableId == null) {
            sendMessage(PLAYER_TOPIC_PREFIX + playerName, "Vous n'êtes à aucune table");
            return;
        }

        PokerTable table = tables.get(tableId);
        if (!table.adminPlayer.equals(playerName)) {
            sendMessage(PLAYER_TOPIC_PREFIX + playerName, "Seul l'administrateur peut démarrer la partie");
            return;
        }

        if (table.players.size() < 2) {
            sendMessage(PLAYER_TOPIC_PREFIX + playerName, "Il faut au moins 2 joueurs pour commencer");
            return;
        }

        startGameOnTable(table);
    }

    private void startGameOnTable(PokerTable table) {
        try {
            table.gameInProgress = true;
            table.currentGame = new PokerHoldem(new ArrayList<>(table.players));

            broadcastToTable(table.tableId, "La partie commence !");
            table.currentGame.demarrerPartie();

        
            table.players.forEach(player -> {
                Joueur joueur = table.currentGame.getJoueurParNom(player);
                sendMessage(PLAYER_TOPIC_PREFIX + player, 
                          "Vos cartes: " + joueur.getCartesPrivees());
            });

            distributeFlop(table);
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage de la partie : " + e.getMessage());
            table.gameInProgress = false;
        }
    }

    private void distributeFlop(PokerTable table) {
        table.currentGame.distribuerFlop();
        broadcastToTable(table.tableId, "Flop: " + table.currentGame.getCartesCommunes());
        distributeTurn(table);
    }

    private void distributeTurn(PokerTable table) {
        table.currentGame.distribuerTurn();
        broadcastToTable(table.tableId, "Turn: " + table.currentGame.getCartesCommunes());
        distributeRiver(table);
    }

    private void distributeRiver(PokerTable table) {
        table.currentGame.distribuerRiver();
        broadcastToTable(table.tableId, "River: " + table.currentGame.getCartesCommunes());
        showResults(table);
    }

    private void showResults(PokerTable table) {
        Map<String, String> results = table.currentGame.calculerResultats();
        results.forEach((player, result) -> 
            sendMessage(PLAYER_TOPIC_PREFIX + player, "Résultat: " + result));

        String winner = table.currentGame.determinerGagnant();
        broadcastToTable(table.tableId, "Le gagnant est: " + winner);
        
        endGame(table);
    }

    private void endGame(PokerTable table) {
        table.gameInProgress = false;
        table.currentGame = null;
        broadcastToTable(table.tableId, "La partie est terminée");
    }

    private void handleCloseTable(MessagePayload payload) {
        String playerName = payload.getData().get("player");
        String tableId = payload.getData().get("tableId");
        
        PokerTable table = tables.get(tableId);
        if (table == null) {
            sendMessage(PLAYER_TOPIC_PREFIX + playerName, "Table introuvable");
            return;
        }

        if (!table.adminPlayer.equals(playerName)) {
            sendMessage(PLAYER_TOPIC_PREFIX + playerName, "Seul l'administrateur peut fermer la table");
            return;
        }

        // notifier tous les joueurs
        table.players.forEach(player -> {
            sendMessage(PLAYER_TOPIC_PREFIX + player, "La table " + tableId + " a été fermée");
            playerTableMapping.remove(player);
        });

        // supprimer une table
        tables.remove(tableId);
        try {
            mqttClient.unsubscribe(TABLE_TOPIC_PREFIX + tableId);
        } catch (MqttException e) {
            System.err.println("Erreur lors de la désinscription de la table: " + e.getMessage());
        }

        broadcastTablesList();
    }

    private void handleListTables(MessagePayload payload) {
        String playerName = payload.getData().get("player");
        StringBuilder tableList = new StringBuilder("Tables disponibles:\n");
        
        tables.forEach((tableId, table) -> {
            tableList.append("Table ").append(tableId)
                    .append(" (").append(table.players.size()).append("/").append(MAX_PLAYERS_PER_TABLE)
                    .append(" joueurs) - Admin: ").append(table.adminPlayer)
                    .append(" - Status: ").append(table.gameInProgress ? "En cours" : "En attente")
                    .append("\n");
        });

        sendMessage(PLAYER_TOPIC_PREFIX + playerName, tableList.toString());
    }

    private void broadcastToTable(String tableId, String message) {
        sendMessage(TABLE_TOPIC_PREFIX + tableId, message);
    }

    private void broadcastTablesList() {
        tables.forEach((tableId, table) -> {
            String status = String.format("Table %s: %d/%d joueurs", 
                tableId, table.players.size(), MAX_PLAYERS_PER_TABLE);
            sendMessage(GAME_TOPIC, status);
        });
    }

    private void sendMessage(String topic, String message) {
        try {
            if (!mqttClient.isConnected()) {
                System.out.println("Client MQTT déconnecté, tentative de reconnexion...");
                mqttClient.reconnect();
            }
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(1);
            mqttClient.publish(topic, mqttMessage);
            Thread.sleep(200);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi du message au topic " + topic + ": " + e.getMessage());
        }
    }

    private class MqttCallbackAdapter implements MqttCallback {
        @Override
        public void connectionLost(Throwable cause) {
            System.out.println("Connexion MQTT perdue : " + cause.getMessage());
            while (!mqttClient.isConnected()) {
                try {
                    System.out.println("Tentative de reconnexion...");
                    mqttClient.reconnect();
                    mqttClient.subscribe(GAME_TOPIC);
                    System.out.println("Reconnecté !");
                    break;
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
        public void deliveryComplete(IMqttDeliveryToken token) {}
    }

    private static class MessagePayload {
        private String type;
        private Map<String, String> data;

        public String getType() { return type; }
        public Map<String, String> getData() { return data; }
    }

    public static void main(String[] args) {
        try {
            Publisher server = new Publisher();
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (server.mqttClient.isConnected()) {
                        server.mqttClient.disconnect();
                        server.mqttClient.close();
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }));
        } catch (MqttException e) {
            System.err.println("Erreur lors du démarrage du serveur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
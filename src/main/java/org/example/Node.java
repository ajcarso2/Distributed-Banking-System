package org.example;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Node {
    private int id;
    private int money;
    private Map<Integer, Integer> clientCredits;
    private final int port;
    private final String host;
    private final Protocol protocol = new Protocol();

    public Node(int id, int initialMoney, String host, int port) {
        this.id = id;
        this.money = initialMoney;
        this.clientCredits = new HashMap<>();
        this.host = host;
        this.port = port;
    }

    public void start() {
        System.out.println("Node " + id + " started with " + money + " money");
        try (Socket socket = new Socket(host, port);
             PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to the leader.");

            // Add any additional communication logic with the Leader server here
            // For example, you could send a "NODE_JOIN" message to the leader
            JSONObject joinMessage = new JSONObject(protocol.nodeCreationResponse(id, port));
            output.println(joinMessage.toString());
            String response = input.readLine();

            ConnectionHandler connectionHandler = new ConnectionHandler();

            System.out.println("Connected to the leader.");

            // Continuously listen for messages from the server
            while (true) {
                String message = input.readLine();

                System.out.println("Received message: " + message);

                if (message != null) {
                    // Process the received message
                    connectionHandler.run(output, input, message);
                } else {
                    // If the server closes the connection, the message will be null
                    System.out.println("Connection closed by the server.");
                    break;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ConnectionHandler {
        public void run(PrintWriter output, BufferedReader input, String message) throws IOException {

            JSONObject json = new JSONObject(message);
            String messageType = json.getString("type");

            System.out.println("Received message: " + message);

            switch (messageType) {
                case "CREDIT_REQUEST":
                    handleCreditRequest(json, output);
                    break;
                case "REGISTER_CREDIT":
                    handleRegisterCredit(json, output);
                    break;
                case "PAYBACK_REQUEST":
                    handlePaybackRequest(json, output);
                    break;
                case "REGISTER_PAYBACK":
                    handleRegisterPayback(json, output);
                    break;
                default:
                    System.out.println("Unknown message type: " + messageType);
            }
        }

        private void handlePaybackRequest(JSONObject json, PrintWriter output) {
            int clientId = json.getInt("clientId");
            int requestedAmount = json.getInt("amount");
            System.out.println("Received payback request from client " + clientId + " for " + requestedAmount + " money");
            JSONObject response = requestPayback(clientId, requestedAmount);
            output.println(response.toString());
        }

        private JSONObject requestPayback(int clientId, int requestedAmount) {
            JSONObject response = new JSONObject();
            response.put("type", "PAYBACK_RESPONSE");
            response.put("clientId", clientId);
            response.put("amount", requestedAmount);

            if (clientCredits.containsKey(clientId)) {
                int clientCredit = clientCredits.get(clientId);
                if (clientCredit >= requestedAmount) {
                    response.put("status", "OK");
                } else {
                    response.put("status", "NOT_ENOUGH_CREDIT");
                }
            } else {
                response.put("status", "NO_CREDIT");
            }

            return response;
        }

        private void handleCreditRequest(JSONObject json, PrintWriter output) {
            int clientId = json.getInt("clientId");
            int requestedAmount = json.getInt("amount");
            System.out.println("Received credit request from client " + clientId + " for " + requestedAmount + " money");
            JSONObject response = requestCredit(clientId, requestedAmount);
            output.println(response.toString());
        }

        private void handleRegisterCredit(JSONObject json, PrintWriter output) {
            int clientId = json.getInt("clientId");
            int creditAmount = json.getInt("amount");

            output.println(registerCredit(clientId, creditAmount).toString());
        }

        private void handleRegisterPayback(JSONObject json, PrintWriter output) {
            int clientId = json.getInt("clientId");
            int paybackAmount = json.getInt("amount");

            output.println(registerPayback(clientId, paybackAmount).toString());
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int amount) {
        this.money = amount;
    }

    public JSONObject requestCredit(int clientId, int requestedAmount) {
        int clientCredit = clientCredits.getOrDefault(clientId, 0);

        // Check if client has enough credit in the node
        if (money >= (requestedAmount) * 1.5) {
            // Check if the requested amount is less than or equal to half of the total credits held by the node

            System.out.println("Requested amount: " + requestedAmount);
            System.out.println("Money: " + money);

            JSONObject json = new JSONObject();
            json.put("type", "CREDIT_APPROVAL");
            json.put("nodeId", id);
            json.put("clientId", clientId);
            json.put("amount", requestedAmount);
            money -= requestedAmount;
            return json;
        }

        JSONObject json = new JSONObject();
        json.put("type", "CREDIT_DENIAL");
        json.put("nodeId", id);
        json.put("clientId", clientId);
        json.put("amount", requestedAmount);

        return json;
    }

    public JSONObject registerCredit(int clientId, int creditAmount) {
        int currentCredit = clientCredits.getOrDefault(clientId, 0);
        clientCredits.put(clientId, currentCredit + creditAmount);
        JSONObject json = new JSONObject();
        json.put("type", "CREDIT_REGISTERED");
        json.put("nodeId", id);
        json.put("clientId", clientId);
        json.put("amount", clientCredits.getOrDefault(clientId, 0));
        return json;
    }

    public JSONObject registerPayback(int clientId, int paybackAmount) {
        int currentCredit = clientCredits.getOrDefault(clientId, 0);
        int newCredit = Math.max(currentCredit - paybackAmount, 0);
        clientCredits.put(clientId, newCredit);
        JSONObject json = new JSONObject();
        json.put("type", "PAYBACK_REGISTERED");
        json.put("nodeId", id);
        json.put("clientId", clientId);
        json.put("amount", clientCredits.getOrDefault(clientId, 0));
        return json;
    }

    public int getOwedAmount(int clientId) {
        return clientCredits.getOrDefault(clientId, 0);
    }
}

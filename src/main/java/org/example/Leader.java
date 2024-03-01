package org.example;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class Leader {
    private static Map<Integer, Integer> clientCredits;
    private final int port;
    private final ExecutorService clientThreadPool;
    private static final Object nodeLock = new Object();
    private final Thread nodeThread;
    private static ReentrantLock clientCreditsLock;
    private static final Map<Integer, PrintWriter> nodesOutput = new ConcurrentHashMap<>();
    private static final Map<Integer, BufferedReader> nodesInput = new ConcurrentHashMap<>();
    private static final Protocol protocol = new Protocol();
    private static final Map<Integer, BlockingQueue<String>> nodesMessageQueue = new ConcurrentHashMap<>();

    public Leader(int port) {
        this.clientCredits = new HashMap<>();
        this.port = port;
        this.clientThreadPool = Executors.newCachedThreadPool();
        this.nodeThread = new Thread(new NodeHandler());
        this.clientCreditsLock = new ReentrantLock();
    }

    public void start() {
        nodeThread.start();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Leader server started, listening on port " + port);
            while (true) {
                System.out.println("Waiting for connection...");
                Socket socket = serverSocket.accept();
                clientThreadPool.execute(new ConnectionHandler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static int getClientCredit(int clientId) {
        return clientCredits.getOrDefault(clientId, 0);
    }

    public static void setClientCredit(int clientId, int credit) {
        clientCredits.put(clientId, credit);
    }

    public void requestCredit(int nodeId, int amount) {


    }



    private static JSONObject handleCreditRequest(JSONObject json, PrintWriter output) throws IOException, InterruptedException {
        int clientId = json.getInt("clientId");
        int requestedAmount = json.getInt("amount");
        int positiveResponses = 0;

        System.out.println("Received credit request from client " + clientId + " for " + requestedAmount);
        //checks if the majority of nodes have approved the credit
        List<BufferedReader> nodeInputs = new ArrayList<>();
        synchronized (nodeLock) {
            for (Integer nodeHash : nodesOutput.keySet()) {
                System.out.println("Sending credit request to node " + nodeHash);
                nodesOutput.get(nodeHash).println(protocol.creditRequest(clientId, requestedAmount));
            }

            for (Integer nodeHash : nodesOutput.keySet()) {
                String jsonString = nodesMessageQueue.get(nodeHash).take();
                JSONObject nodesI = new JSONObject(jsonString);
                if (nodesI.get("type").equals("CREDIT_APPROVAL")) {
                    positiveResponses++;
                }
            }
        }

        // One two, buckle my shoe, three four, buckle some more, five six, nike kicks

        if ((positiveResponses > nodesOutput.size() / 2) || (nodesOutput.size() == 1 && positiveResponses == 1)) {
            int share = requestedAmount / nodesOutput.size();
            int totalGranted = 0;
            System.out.println("Credit request approved, granting " + share + " to each node");
            synchronized (nodeLock) {
                // Grant the credit
                for (Integer nodeHash : nodesOutput.keySet()) {
                    System.out.println("Sending credit grant to node " + nodeHash);
                    nodesOutput.get(nodeHash).println(protocol.applyCredit(clientId, share));
                    // Fix the problem here, use nodesMessageQueue instead of nodesInput
                    String responseString = nodesMessageQueue.get(nodeHash).take();
                    JSONObject response = new JSONObject(responseString);
                    if (response.get("type").equals("CREDIT_REGISTERED")) {
                        totalGranted += response.getInt("amount");
                        System.out.println("Node " + nodeHash + " granted " + response.getInt("amount"));
                    }
                }
                System.out.println("Successful Total granted: " + totalGranted);
            }
            // Return the total amount granted
            System.out.println("Returning credit granted to client " + clientId);
            return protocol.creditGranted(totalGranted);
        } else {
            System.out.println("Credit request denied");
            return protocol.creditDenied();
        }
    }

    private static JSONObject handlePaybackRequest(JSONObject json) throws IOException, InterruptedException {
        int clientId = json.getInt("clientId");
        int paymentAmount = json.getInt("amount");
        int positiveResponses = 0;

        // checks if the client has enough money to pay back
        synchronized (nodeLock) {
            //for loop that goes through all the nodes and sends a payback request
            for (Integer nodeHash : nodesOutput.keySet()) {
                System.out.println("Sending payback request to node " + nodeHash);
                nodesOutput.get(nodeHash).println(protocol.requestPayback(clientId, paymentAmount / nodesOutput.size()));
            }

            //if the node confirms the client has enough money to pay back, it will send a payback approval
            for (Integer nodeHash : nodesOutput.keySet()) {
                String jsonString = nodesMessageQueue.get(nodeHash).take();
                JSONObject nodesI = new JSONObject(jsonString);
                if (nodesI.get("status").equals("OK")) {
                    positiveResponses++;
                }
            }

            //if a majority of nodes approve the payback, then the leader will send a payback request to all the nodes
            if ((positiveResponses > nodesOutput.size() / 2) || (nodesOutput.size() == 1 && positiveResponses == 1)) {
                int share = paymentAmount / nodesOutput.size();
                int totalPaidBack = 0;
                System.out.println("Payback request approved, accepting " + share + " from client " + clientId);
                //the amount of money the client needs to pay back will be divided by the number of nodes
                for (Integer nodeHash : nodesOutput.keySet()) {
                    System.out.println("Receiving payback from client " + clientId + " for node " + nodeHash);
                    nodesOutput.get(nodeHash).println(protocol.applyPayback(clientId, share));
                    String responseString = nodesMessageQueue.get(nodeHash).take();
                    JSONObject response = new JSONObject(responseString);
                    if (response.get("type").equals("PAYBACK_REGISTERED")) {
                        totalPaidBack += response.getInt("amount");
                        System.out.println("Client " + clientId + " paid back " + response.getInt("amount") + " to node " + nodeHash);
                    }
                }
                System.out.println("Successful Total paid back: " + totalPaidBack);

                //once the transaction is complete, the leader will send a payback success message to the client
                System.out.println("Returning payback success to client " + clientId);
                return protocol.paybackResponse(clientId, totalPaidBack, true);
            } else {
                System.out.println("Payback request denied");
                return protocol.paybackResponse(clientId, 0, false);
            }
        }
    }


    private static class NodeHandler implements Runnable {
        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(9090)) {
                System.out.println("Node server started, listening on port 9090");
                while (true) {
                    System.out.println("Waiting for node connection...");
                    Socket socket = serverSocket.accept();
                    handleNodeConnection(socket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleNodeConnection(Socket socket) {
            try (
                    PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                int nodeId = 0;
                while (true) {
                    String jsonString = input.readLine();
                    System.out.println("Received JSON from node: " + jsonString);
                    JSONObject json = new JSONObject(jsonString);
                    String messageType = json.getString("type");

                    if (messageType.equals("NODE")) {
                        String action = json.getString("action");
                        nodeId = json.getInt("nodeId");

                        synchronized (nodeLock) {
                            switch (action) {
                                case "CREATED":
                                    System.out.println("Node created");
                                    nodesOutput.put(nodeId, output);
                                    nodesInput.put(nodeId, input);
                                    nodesMessageQueue.put(nodeId, new LinkedBlockingQueue<>());
                                    output.println("success");
                                    break;
                                case "DELETED":
                                    System.out.println("Node deleted");
                                    nodesOutput.remove(nodeId);
                                    nodesInput.remove(nodeId);
                                    nodesMessageQueue.remove(nodeId);
                                    break;
                                default:
                                    System.out.println("Unknown action: " + action);
                                    break;
                            }
                        }
                    } else {
                        nodesMessageQueue.get(nodeId).add(jsonString);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private static class ConnectionHandler implements Runnable {
        private final Socket socket;

        public ConnectionHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                while (true) {
                    String jsonString = input.readLine();
                    System.out.println("Received JSON: " + jsonString);
                    JSONObject json = new JSONObject(jsonString);
                    String messageType = json.getString("type");

                    if (messageType.equals("CLIENT")) {
                        synchronized (nodeLock) {
                            handleClient(json, output);
                        }
                    } else {
                        System.out.println("Unknown message type: " + messageType);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }



        private void handleClient(JSONObject json, PrintWriter output) throws IOException, InterruptedException {
            String action = json.getString("action");
            int clientId = json.getInt("clientId");

            switch (action) {
                case "SHOW_BALANCE":
                    int balance = ClientManager.getClientCredit(clientId);
                    output.println(protocol.createBalanceResponse(clientId, balance));
                    break;
                case "TRANSFER_MONEY":
                    System.out.println("Transfer money");
                    int destinationId = json.getInt("destinationId");
                    int amount = json.getInt("amount");
                    boolean success = ClientManager.transferMoney(clientId, destinationId, amount);
                    output.println(protocol.createTransferResponse(clientId, destinationId, amount, success));
                    break;
                case "SET_BALANCE":
                    int newBalance = json.getInt("newBalance");
                    ClientManager.setClientCredit(clientId, newBalance);
                    output.println(protocol.createSetBalanceResponse(clientId, newBalance));
                    break;
                case "CREDIT":
                    JSONObject response = handleCreditRequest(json, output);
                    output.println(response);
                    break;
                case "PAY":
                    int paymentAmount = json.getInt("amount");
                    JSONObject paymentResponse = handlePaybackRequest(json);
                    output.println(paymentResponse);
                    break;

                default:
                    System.out.println("Unknown action: " + action);
                    break;
            }
        }
    }

    // Add new methods in the Protocol class
    public JSONObject applyPayback(int clientId, int amount) {
        JSONObject request = new JSONObject();
        request.put("type", "PAYBACK");
        request.put("clientId", clientId);
        request.put("amount", amount);
        return request;
    }

    public JSONObject paybackResponse(int clientId, int amount, boolean success) {
        JSONObject response = new JSONObject();
        response.put("type", "PAYBACK_RESPONSE");
        response.put("clientId", clientId);
        response.put("amount", amount);
        response.put("success", success);
        return response;
    }


    private boolean transferMoney(int clientId, int destinationId, int amount) {
        clientCreditsLock.lock();
        try {
            int clientBalance = getClientCredit(clientId);
            if (clientBalance >= amount) {
                setClientCredit(clientId, clientBalance - amount);
                int destinationBalance = getClientCredit(destinationId);
                setClientCredit(destinationId, destinationBalance + amount);
                return true;
            } else {
                return false;
            }
        } finally {
            clientCreditsLock.unlock();
        }
    }

    public static void main(String[] args) {
        int port = 8080;
        Leader leader = new Leader(port);
        leader.start();
    }
}



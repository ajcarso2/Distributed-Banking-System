package org.example;

import org.json.JSONObject;

public class Protocol {
    public String createBalanceRequest(int clientId) {
        JSONObject json = new JSONObject();
        json.put("type", "CLIENT");
        json.put("action", "SHOW_BALANCE");
        json.put("clientId", clientId);
        return json.toString();
    }

    public String createTransferRequest(int clientId, int destinationId, int amount) {
        JSONObject json = new JSONObject();
        json.put("type", "CLIENT");
        json.put("action", "TRANSFER_MONEY");
        json.put("clientId", clientId);
        json.put("destinationId", destinationId);
        json.put("amount", amount);
        return json.toString();
    }

    public int parseBalanceResponse(String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        return json.getInt("balance");
    }

    public String createBalanceResponse(int clientId, int balance) {
        JSONObject json = new JSONObject();
        json.put("type", "RESPONSE");
        json.put("clientId", clientId);
        json.put("balance", balance);
        return json.toString();
    }

    public boolean isSuccessfulTransfer(String jsonString) {
        JSONObject json = new JSONObject(jsonString);
        return json.getBoolean("success");
    }

    public String createTransferResponse(int clientId, int destinationId, int amount, boolean success) {
        JSONObject json = new JSONObject();
        json.put("type", "RESPONSE");
        json.put("clientId", clientId);
        json.put("destinationId", destinationId);
        json.put("amount", amount);
        json.put("success", success);
        return json.toString();
    }

    public String createSetBalanceRequest(int clientId, int newBalance) {
        JSONObject json = new JSONObject();
        json.put("type", "CLIENT");
        json.put("action", "SET_BALANCE");
        json.put("clientId", clientId);
        json.put("newBalance", newBalance);
        return json.toString();
    }

    public String createSetBalanceResponse(int clientId, int newBalance) {
        JSONObject json = new JSONObject();
        json.put("type", "RESPONSE");
        json.put("clientId", clientId);
        json.put("newBalance", newBalance);
        return json.toString();
    }

    public String createCreditRequest(int clientId, int amount) {
        JSONObject json = new JSONObject();
        json.put("type", "CLIENT");
        json.put("action", "CREDIT");
        json.put("clientId", clientId);
        json.put("amount", amount);
        return json.toString();
    }

    public boolean isSuccessfulCredit(String response) {
        JSONObject json = new JSONObject(response);
        return json.getBoolean("success");
    }

    public String createPayRequest(int clientId, int amount) {
        JSONObject json = new JSONObject();
        json.put("type", "CLIENT");
        json.put("action", "PAY");
        json.put("clientId", clientId);
        json.put("amount", amount);
        return json.toString();
    }

    public boolean isSuccessfulPay(String response) {
        JSONObject json = new JSONObject(response);
        return json.getBoolean("success");
    }

    public String nodeCreationResponse(int nodeId, int port) {
        JSONObject json = new JSONObject();
        json.put("type", "NODE");
        json.put("action", "CREATED");
        json.put("nodeId", nodeId);
        json.put("port", port);
        return json.toString();
    }

    public String creditRequest(int clientId, int requestedAmount) {
        JSONObject json = new JSONObject();
        json.put("type", "CREDIT_REQUEST");
        json.put("clientId", clientId);
        json.put("amount", requestedAmount);
        return json.toString();
    }

    public String applyCredit(int clientId, int amount) {
        JSONObject json = new JSONObject();
        json.put("type", "REGISTER_CREDIT");
        json.put("clientId", clientId);
        json.put("amount", amount);
        return json.toString();
    }

    public JSONObject creditGranted(int totalGranted) {
        System.out.println("Credit granted");
        JSONObject json = new JSONObject();
        json.put("type", "CREDIT_GRANTED");
        json.put("totalGranted", totalGranted);
        json.put("success", true);
        return json;
    }

    public JSONObject creditDenied() {
        System.out.println("Credit denied");
        JSONObject json = new JSONObject();
        json.put("type", "CREDIT_DENIED");
        json.put("success", false);
        return json;
    }

    public JSONObject applyPayback(int clientId, int amount) {
        JSONObject request = new JSONObject();
        request.put("type", "REGISTER_PAYBACK");
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

    public String requestPayback(int clientId, int i) {
        JSONObject request = new JSONObject();
        request.put("type", "PAYBACK_REQUEST");
        request.put("clientId", clientId);
        request.put("amount", i);
        return request.toString();
    }
}
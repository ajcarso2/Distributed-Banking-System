package org.example;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Message {
    private String messageType;
    private Map<String, Object> data;

    public Message(String messageType, Map<String, Object> data) {
        this.messageType = messageType;
        this.data = data != null ? data : new HashMap<>();
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }


    public static Message fromString(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.getString("messageType");
            Map<String, Object> data = new HashMap<>();

            if (json.has("clientId")) {
                data.put("clientId", json.getInt("clientId"));
            }

            if (json.has("amount")) {
                data.put("amount", json.getInt("amount"));
            }

            return new Message(type, data);
        } catch (JSONException e) {
            System.err.println("Error parsing message string: " + e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        json.put("messageType", messageType);

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            json.put(entry.getKey(), entry.getValue());
        }

        return json.toString();
    }

    public String getType() {
        return messageType;
    }

    public int getClientId() {
        return (int) data.get("clientId");
    }

    public int getAmount() {
        return (int) data.get("amount");
    }
}

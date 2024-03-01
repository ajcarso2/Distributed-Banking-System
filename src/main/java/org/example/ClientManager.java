package org.example;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class ClientManager {
    private static Map<Integer, Integer> clientCredits = new HashMap<>();
    private static ReentrantLock clientCreditsLock = new ReentrantLock();

    public static int getClientCredit(int clientId) {
        clientCreditsLock.lock();
        try {
            return clientCredits.getOrDefault(clientId, 0);
        } finally {
            clientCreditsLock.unlock();
        }
    }

    public static void setClientCredit(int clientId, int credit) {
        clientCreditsLock.lock();
        try {
            clientCredits.put(clientId, credit);
        } finally {
            clientCreditsLock.unlock();
        }
    }

    public static boolean transferMoney(int clientId, int destinationId, int amount) {
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
}

package org.example;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private int id;
    private int port;
    private String host;
    private Protocol protocol;

    public Client(int id, int port, String host) {
        this.id = id;
        this.protocol = new Protocol();
        this.port = port;
        this.host = host;
    }

    public void start() {
        try (Socket socket = new Socket(host, port);
             PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Connected to the leader."); // Add this line for debugging

            System.out.println("Enter your clientID:");
            int clientId = Integer.parseInt(scanner.nextLine());

            output.println(protocol.createSetBalanceRequest(clientId, 2000));
            input.readLine();

            boolean running = true;
            while (running) {
                System.out.println("Choose an action:");
                System.out.println("1. Show balance");
                System.out.println("2. Transfer money");
                System.out.println("3. Request Credit");
                System.out.println("4. Pay Credit");
                System.out.println("5. Quit");

                int choice = Integer.parseInt(scanner.nextLine());
                switch (choice) {
                    case 1:
                        showBalance(clientId, output, input);
                        break;
                    case 2:
                        transferMoney(clientId, output, input);
                        break;
                    case 3:
                        requestCredit(clientId, output, input);
                        break;
                    case 4:
                        payCredit(clientId, output, input);
                        break;
                    case 5:
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            }
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void payCredit(int clientId, PrintWriter output, BufferedReader input) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the amount you want to pay:");
        int amount = Integer.parseInt(scanner.nextLine());

        output.println(protocol.createPayRequest(clientId, amount));
        JSONObject response = null;
        try {
            response = new JSONObject(input.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert response != null;
        if (response.getBoolean("success")) {
            System.out.println("Pay successful.");
        } else {
            System.out.println("Pay failed.");
        }
    }

    private void requestCredit(int clientId, PrintWriter output, BufferedReader input) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the amount you want to request:");
        int amount = Integer.parseInt(scanner.nextLine());

        output.println(protocol.createCreditRequest(clientId, amount));
        String response = input.readLine();
        JSONObject jsonObject = new JSONObject(response);
        if (protocol.isSuccessfulCredit(response)) {
            System.out.println("Credit request successful.");
            System.out.println("Your credit is: " + jsonObject.getInt("totalGranted"));
        } else {
            System.out.println("Credit request failed.");
        }
    }

    private void showBalance(int clientId, PrintWriter output, BufferedReader input) throws IOException {
        output.println(protocol.createBalanceRequest(clientId));
        String response = input.readLine();
        int balance = protocol.parseBalanceResponse(response);
        System.out.println("Current balance: " + balance);
    }

    private void transferMoney(int clientId, PrintWriter output, BufferedReader input) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the destination clientID:");
        int destinationId = Integer.parseInt(scanner.nextLine());
        System.out.println("Enter the amount to transfer:");
        int amount = Integer.parseInt(scanner.nextLine());

        output.println(protocol.createTransferRequest(clientId, destinationId, amount));
        String response = input.readLine();
        if (protocol.isSuccessfulTransfer(response)) {
            System.out.println("Transfer successful.");
        } else {
            System.out.println("Transfer failed.");
        }
    }

    public static void main(String[] args) {
        int port = 8080;
        String host = "localhost";
        Client client = new Client(0, port, host);
        client.start();
    }
}

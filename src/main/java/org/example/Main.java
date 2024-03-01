package org.example;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: gradle <leader|node|client>");
            return;
        }

        String role = args[0];
        switch (role) {
            case "leader":
                Leader leader = new Leader(Integer.parseInt(args[1]));
                leader.start();
                break;
            case "node":
                int nodeId = Integer.parseInt(args[1]);
                int initialMoney = Integer.parseInt(args[2]);
                int port = Integer.parseInt(args[3]);
                String host = (args[4]);
                Node node = new Node(nodeId, initialMoney, host,port);
                node.start();
                break;
            case "client":
                Client client = new Client(Integer.parseInt(args[1]), Integer.parseInt(args[3]), args[2]);
                client.start();
                break;
            default:
                System.err.println("Invalid role: " + role);
                break;
        }
    }
}
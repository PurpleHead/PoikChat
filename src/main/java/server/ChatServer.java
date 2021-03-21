package server;

import serializers.KeySerializer;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer extends Thread {

    private ServerSocket serverSocket;
    private boolean running;
    private ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<>();

    public ChatServer (int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.running = true;
    }

    @Override
    public void run() {
        while(running) {
            try {
                Socket socket = serverSocket.accept();
                String socketID;
                PublicKey publicKey;
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());

                System.out.println("> Socket on address " + socket.getLocalAddress() + " is attempting to connect!");
                socketID = inputStream.readUTF();
                publicKey = KeySerializer.fromString(inputStream.readUTF());

                Client c = new Client(this, socket, socketID, publicKey);
                if(!clients.containsKey(socketID)) {
                    clients.put(socketID, c);
                    c.start();
                    c.sendMessage(ServerResponse.CONNECTION_SUCCESS);
                    System.out.println("> User \'" + socketID + "\' successfully connected!");

                    // Broadcast client key to other clients
                    broadcast(c, ServerResponse.STORE_KEY, false);
                    broadcast(c, c.getUsername(), false);
                    broadcast(c, KeySerializer.convertToString(c.getPublicKey()), false);

                    // Sending existing Keys
                    clients.forEach((k, v) -> {
                        try {
                            if(!v.equals(c)) {
                                c.sendMessage(ServerResponse.STORE_KEY);
                                c.sendMessage(k);
                                c.sendMessage(KeySerializer.convertToString(v.getPublicKey()));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

                    serverBroadcast("User \'" + socketID + "\' joined the chat.", true);
                    c.sendMessage("Please don't forget: only direct messages are encrypted and cannot be read by others!");
                } else {
                    c.sendMessage("There is already a user with this username connected!");
                    System.out.println("> User \'" + socketID + "\' is already connected!");
                }

            } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
            }
        }

        clients.forEach((k, v) -> v.setRunning(false));
    }

    public void serverBroadcast (String message, boolean showUser) {
        broadcast(null, message, showUser);
    }

    public void broadcast (Client sender, String message) {
        broadcast(sender, message, true);
    }

    public void broadcast (Client sender, String message, boolean showUser) {
        System.out.println("> Broadcasting message \'" + message + "\'");
        String user = "";
        if(showUser) {
            user = (sender == null ? "Server: " : sender.getUsername() + ": ");
        }
        final String finalUser = user;
        this.clients.forEach((k, v) -> {
            if (!v.equals(sender)) {
                try {
                    v.sendMessage(finalUser + message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void disconnect (String username) {
        Client client = clients.get(username);
        System.out.println("> User \'" + client.getUsername() + "\' disconnected!");
        clients.remove(username);

        // Tell clients to remove public key
        serverBroadcast(ServerResponse.DISCARD_KEY, false);
        serverBroadcast(username, false);

        serverBroadcast("User \'" + username + "\' left the chat.", true);
    }

    public void shutdown () {
        this.running = false;
    }

}

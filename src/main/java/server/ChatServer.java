package server;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
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
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(inputStream.readUTF())));
                System.out.println(publicKey);

                Client c = new Client(this, socket, socketID, publicKey);
                if(!clients.containsKey(socketID)) {
                    clients.put(socketID, c);
                    c.start();
                    c.sendMessage("success");
                    System.out.println("> User \'" + socketID + "\' successfully connected!");
                    serverBroadcast("User \'" + socketID + "\' joined the chat.");
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

    public void serverBroadcast (String message) {
        broadcast(null, message);
    }

    public void broadcast (Client sender, String message) {
        System.out.println("> Broadcasting message \'" + message + "\'");
        this.clients.forEach((k, v) -> {
            if (!v.equals(sender)) {
                try {
                    v.sendMessage((sender == null ? "Server: " : sender.getUsername() + ": ") + message);
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
        serverBroadcast("User \'" + username + "\' left the chat.");
    }

    public void shutdown () {
        this.running = false;
    }

}

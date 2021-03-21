package client;

import serializers.KeySerializer;
import server.ServerResponse;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ConcurrentHashMap;

public class ChatClient extends Thread {

    private ConcurrentHashMap<String, PublicKey> publicKeys = new ConcurrentHashMap<>();

    private Socket socket;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private boolean running;
    private KeyPair keyPair;

    public ChatClient(int port, String username) throws IOException, NoSuchAlgorithmException {
        this.socket = new Socket("localhost", port);
        this.keyPair = ChatClient.generateKeyPair();
        this.inputStream = new DataInputStream(socket.getInputStream());
        this.outputStream = new DataOutputStream(socket.getOutputStream());
        this.outputStream.writeUTF(username);
        this.outputStream.writeUTF(KeySerializer.convertToString(keyPair.getPublic()));

        try {
            String response = inputStream.readUTF();
            if (response.equals(ServerResponse.CONNECTION_SUCCESS)) {
                this.running = true;
            } else {
                this.running = false;
                System.out.println(response);
            }
        } catch (SocketException e) {
            System.out.println("Connection refused.");
        }
    }

    @Override
    public void run() {
        while (running) {
            String message;
            try {
                message = inputStream.readUTF();
                if(message.equals(ServerResponse.STORE_KEY)) {
                    String username = inputStream.readUTF();
                    PublicKey publicKey = KeySerializer.fromString(inputStream.readUTF());
                    publicKeys.put(username, publicKey);
                    System.out.println("Stored key from user " + username);
                } else if (message.equals(ServerResponse.DISCARD_KEY)) {
                    String username = inputStream.readUTF();
                    publicKeys.remove(username);
                    System.out.println("Removed stored key from: " + username);
                } else {
                    System.out.println(message);
                }
            } catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
                System.out.println("Connection to server lost.");
                this.running = false;
                System.exit(0);
            }
        }
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    public void sendMessage(String message) throws IOException {
        this.outputStream.writeUTF(message);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}

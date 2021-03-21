package client;

import constants.CommandConstants;
import encryption.MessageEncryptor;
import serializers.KeySerializer;
import server.ServerResponse;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.*;
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
        this.keyPair = MessageEncryptor.generateKeyPair();
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
                    publicKeys.put(username.toLowerCase(), publicKey);
                } else if (message.equals(ServerResponse.DISCARD_KEY)) {
                    String username = inputStream.readUTF();
                    publicKeys.remove(username.toLowerCase());
                } else if(message.equals(ServerResponse.ENCRYPTED_MESSAGE)) {
                    System.out.println(inputStream.readUTF());
                    String encryptedMessage = inputStream.readUTF();
                    System.out.println(MessageEncryptor.decryptMessage(encryptedMessage, keyPair.getPrivate()));
                } else {
                    System.out.println(message);
                }
            } catch (Exception e) {
                System.out.println("Connection to server lost.");
                this.running = false;
                System.exit(0);
            }
        }
    }

    public void sendMessage(String message) throws IOException {
        if(message.startsWith(CommandConstants.WHISPER)) {
            String[] split = message.split(" ");
            try {
                String username = split[1].toLowerCase();
                String messageBody = "";

                for (int i = 2; i < split.length; i++) {
                    messageBody += split[i] + " ";
                }

                String encryptedMessage = MessageEncryptor.encryptMessage(messageBody, publicKeys.get(username));
                message = split[0] + " " + username + " " + encryptedMessage;
            } catch (Exception e) {
                System.out.println(CommandConstants.WHISPER_TEMPLATE);
            }
        }
        this.outputStream.writeUTF(message);

    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}

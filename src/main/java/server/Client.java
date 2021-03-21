package server;

import constants.CommandConstants;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Objects;

public class Client extends Thread {

    private ChatServer server;
    private Socket socket;
    private boolean running;
    private String username;
    private DataOutputStream outputStream;
    private PublicKey publicKey;

    public Client (ChatServer server, Socket socket, String username, PublicKey publicKey) throws IOException {
        this.server = server;
        this.socket = socket;
        this.running = true;
        this.username = username;
        this.publicKey = publicKey;
        this.outputStream = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        while (running) {
            try {
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                String message = inputStream.readUTF();

                if(message.startsWith(CommandConstants.WHISPER)) {
                    try {
                        String[] split = message.split(" ");
                        String messageBody = "";
                        for(int i = 2; i < split.length; i++) {
                            messageBody += split[i]  + " ";
                        }
                        server.sendDirectMessage(this, split[1], messageBody);
                    } catch (IndexOutOfBoundsException e) {
                        this.sendMessage(CommandConstants.WHISPER_TEMPLATE);
                    }
                } else {
                    server.broadcast(this, message);
                }


            } catch (IOException e) {
                server.disconnect(this.username);
                this.setRunning(false);
            }
        }
    }

    public void sendMessage (String message) throws IOException {
        this.outputStream.writeUTF(message);
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public String getUsername() {
        return username;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return username.equals(client.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}

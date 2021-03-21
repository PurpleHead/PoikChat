package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

public class Client extends Thread {

    private ChatServer server;
    private Socket socket;
    private boolean running;
    private String username;
    private DataOutputStream outputStream;

    public Client (ChatServer server, Socket socket, String username) throws IOException {
        this.server = server;
        this.socket = socket;
        this.running = true;
        this.username = username;
        this.outputStream = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        while (running) {
            try {
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                String message = inputStream.readUTF();

                server.broadcast(this, message);

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

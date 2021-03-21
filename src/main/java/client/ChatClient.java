package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class ChatClient extends Thread {

    private Socket socket;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private boolean running;

    public ChatClient (int port, String username) throws IOException {
        socket = new Socket("localhost", port);
        this.inputStream = new DataInputStream(socket.getInputStream());
        this.outputStream = new DataOutputStream(socket.getOutputStream());
        this.outputStream.writeUTF(username);

        try {
            String response = inputStream.readUTF();
            if(response.equals("success")) {
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
                System.out.println(message);
            } catch (IOException e) {
                System.out.println("Connection to server lost.");
                this.running = false;
                System.exit(0);
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
}

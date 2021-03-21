package server;

import java.io.IOException;

public class ServerMain {

    public static void main(String[] args) throws IOException, InterruptedException {
        ChatServer server = new ChatServer(25565);
        server.start();
        Thread.sleep(60000);
        server.shutdown();
    }
}

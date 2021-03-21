package client;

import java.io.IOException;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) throws IOException {
        System.out.println("Please enter your username!");
        Scanner reader = new Scanner(System.in);
        String username = reader.nextLine();

        ChatClient chatClient = new ChatClient(25565, username);
        chatClient.start();

        while (chatClient.isRunning()) {
            String message = reader.nextLine();
            chatClient.sendMessage(message);
        }
    }

}

package src;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static char currentPlayer = 'X';  // Player X starts first
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    public Client(String serverAddress, int port) {
        setupConnection(serverAddress, port);
        handleServerResponse();
    }

    private static void setupConnection(String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port);
            System.out.println("Connected to server at " + serverAddress + ":" + port);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void sendMove(int move) {
        out.println(move);  // Send the player's move to the server
    }

    private static void handleServerResponse() {
        try {
            String response;
            Scanner scanner = new Scanner(System.in);
    
            while ((response = in.readLine()) != null) {
                if (response.contains("Your turn")) {
                    System.out.println(response);  // Tell the player it's their turn
                    System.out.print("Enter your move (1-9): ");
                    int move = scanner.nextInt();
                    sendMove(move);
                } else if (response.contains("WINNER")) {
                    System.out.println("Game Over: " + response);  // Show winner message
                    break;
                } else if (response.contains("You Lose!")) {
                    System.out.println("Game Over: " + response);  // Show loser message
                    break;
                } else if (response.contains("DRAW")) {
                    System.out.println("Game Over: " + response);  // Show draw message
                    break;
                } else {
                    System.out.println(response);
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling server response: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Client("127.0.0.1", 5165);  // Connect to the server
    }
}

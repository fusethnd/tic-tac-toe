package src;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static final int PORT = 5165;
    private static char[] board = new char[9];  // Tic-Tac-Toe board (9 cells)
    private static int currentPlayer = 1;  // Player 1 starts (X)
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server is listening on port " + PORT + "...");

            // Player handler references
            ClientHandler player1 = null;
            ClientHandler player2 = null;

            // Accept player 1
            Socket clientSocket1 = serverSocket.accept();
            player1 = new ClientHandler(clientSocket1, 'X');
            player1.setOtherPlayer(player2); // Set the other player reference for player 1
            player1.start();
            System.out.println("Player 1 connected");

            // Accept player 2
            Socket clientSocket2 = serverSocket.accept();
            player2 = new ClientHandler(clientSocket2, 'O');
            player2.setOtherPlayer(player1); // Set the other player reference for player 2
            player2.start();
            System.out.println("Player 2 connected");

            // Wait for both players to finish
            player1.join();
            player2.join();

        } catch (IOException | InterruptedException e) {
            System.out.println("Error in server setup: " + e.getMessage());
        }
    }

    public static class ClientHandler extends Thread {
        private Socket clientSocket;
        private char playerSymbol;
        private BufferedReader in;
        private PrintWriter out;
        private ClientHandler otherPlayer; // Reference to the other player

        public ClientHandler(Socket socket, char symbol) {
            this.clientSocket = socket;
            this.playerSymbol = symbol;
        }

        public void setOtherPlayer(ClientHandler otherPlayer) {
            this.otherPlayer = otherPlayer;  // Set the reference to the other player
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                out.println("Welcome Player " + playerSymbol + ". You're playing as " + playerSymbol);

                while (true) {
                    synchronized (Server.class) {
                        while (currentPlayer != (playerSymbol == 'X' ? 1 : 2)) {
                            Server.class.wait();  // Wait for the correct player's turn
                        }

                        out.println("Your turn. Current board:");
                        printBoard();  // Show the current board to the player
                        out.println("Please enter a number (1-9): ");
                        int move = Integer.parseInt(in.readLine()) - 1;

                        if (board[move] == '\0') {
                            board[move] = playerSymbol;
                            if (checkWin(playerSymbol)) {
                                out.println("WINNER_" + playerSymbol);
                                // Notify the other player that they lost
                                otherPlayer.out.println("You Lose! " + playerSymbol + " wins");
                                break;
                            }
                            if (checkDraw()) {
                                out.println("DRAW");
                                // Notify the other player about the draw
                                otherPlayer.out.println("Game Over: It's a draw!");
                                break;
                            }
                            currentPlayer = (currentPlayer == 1) ? 2 : 1;  // Switch turn
                            Server.class.notifyAll();  // Notify the other player
                        } else {
                            out.println("Invalid move. Try again.");
                        }
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }

        private void printBoard() {
            System.out.println("Current Board:");
            for (int i = 0; i < 9; i++) {
                System.out.print((board[i] == '\0' ? i + 1 : board[i]) + " ");
                if ((i + 1) % 3 == 0) System.out.println();
            }
        }

        // Check for winner
        private boolean checkWin(char playerSymbol) {
            // Check winning conditions (same as before)
            int[][] winConditions = {
                {0, 1, 2}, {3, 4, 5}, {6, 7, 8},  // Rows
                {0, 3, 6}, {1, 4, 7}, {2, 5, 8},  // Columns
                {0, 4, 8}, {2, 4, 6}               // Diagonals
            };
            for (int[] condition : winConditions) {
                if (board[condition[0]] == playerSymbol && board[condition[1]] == playerSymbol && board[condition[2]] == playerSymbol) {
                    return true;
                }
            }
            return false;
        }

        // Check for draw
        private boolean checkDraw() {
            for (char cell : board) {
                if (cell == '\0') return false;  // If there's an empty cell, it's not a draw
            }
            return true;
        }
    }
}
package src;

import java.io.*;
import java.net.*;

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
            player1.setOtherPlayer(player2); // Will be set again later
            player1.start();
            System.out.println("Player 1 connected");

            // Accept player 2
            Socket clientSocket2 = serverSocket.accept();
            player2 = new ClientHandler(clientSocket2, 'O');
            player2.setOtherPlayer(player1);
            player1.setOtherPlayer(player2); // Now that player2 is known
            player2.start();
            System.out.println("Player 2 connected");

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
        private ClientHandler otherPlayer;

        public ClientHandler(Socket socket, char symbol) {
            this.clientSocket = socket;
            this.playerSymbol = symbol;
        }

        public void setOtherPlayer(ClientHandler otherPlayer) {
            this.otherPlayer = otherPlayer;
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
                            Server.class.wait();
                        }

                        out.println("Your turn. Current board:");
                        sendBoardToPlayer(out);  // send to self
                        out.println("Please enter a number (1-9): ");

                        int move = Integer.parseInt(in.readLine()) - 1;

                        if (move < 0 || move >= 9 || board[move] != '\0') {
                            out.println("Invalid move. Try again.");
                            continue;
                        }

                        board[move] = playerSymbol;

                        if (checkWin(playerSymbol)) {
                            out.println("WINNER_" + playerSymbol);
                            otherPlayer.out.println("You Lose! " + playerSymbol + " wins");
                            sendBoardToPlayer(out);
                            sendBoardToPlayer(otherPlayer.out);
                            break;
                        }

                        if (checkDraw()) {
                            out.println("DRAW");
                            otherPlayer.out.println("Game Over: It's a draw!");
                            sendBoardToPlayer(out);
                            sendBoardToPlayer(otherPlayer.out);
                            break;
                        }

                        // Notify opponent
                        otherPlayer.out.println("Opponent moved. Here's the board:");
                        sendBoardToPlayer(otherPlayer.out);

                        currentPlayer = (currentPlayer == 1) ? 2 : 1;
                        Server.class.notifyAll();
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

        private void sendBoardToPlayer(PrintWriter out) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 9; i++) {
                sb.append(board[i] == '\0' ? (i + 1) : board[i]);
                sb.append(" ");
            }
            out.println(sb.toString().trim());
        }

        private boolean checkWin(char playerSymbol) {
            int[][] winConditions = {
                {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
                {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
                {0, 4, 8}, {2, 4, 6}
            };
            for (int[] cond : winConditions) {
                if (board[cond[0]] == playerSymbol &&
                    board[cond[1]] == playerSymbol &&
                    board[cond[2]] == playerSymbol) {
                    return true;
                }
            }
            return false;
        }

        private boolean checkDraw() {
            for (char cell : board) {
                if (cell == '\0') return false;
            }
            return true;
        }
    }
}

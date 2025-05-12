package src;

import java.io.*; // สำหรับจัดการ input/output stream
import java.net.*; // สำหรับใช้งาน socket และ server socket

public class Server {
    private static final int PORT = 5165; // พอร์ตที่ server จะเปิดรอการเชื่อมต่อ
    private static char[] board = new char[9];  // กระดาน XO มี 9 ช่อง
    private static int currentPlayer = 1;  // เริ่มต้นที่ผู้เล่น 1 (X)
    private static ServerSocket serverSocket; // ตัวรับการเชื่อมต่อจาก client

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT); // สร้าง server socket ที่พอร์ต 5165
            System.out.println("Server is listening on port " + PORT + "...");

            ClientHandler player1 = null;
            ClientHandler player2 = null;

            // รอการเชื่อมต่อจากผู้เล่น 1
            Socket clientSocket1 = serverSocket.accept();
            player1 = new ClientHandler(clientSocket1, 'X'); // กำหนดให้เป็นผู้เล่น X
            player1.setOtherPlayer(player2); // ตั้งค่าอีกฝั่งเป็น null ชั่วคราว
            player1.start();
            System.out.println("Player 1 connected");

            // รอการเชื่อมต่อจากผู้เล่น 2
            Socket clientSocket2 = serverSocket.accept();
            player2 = new ClientHandler(clientSocket2, 'O'); // ผู้เล่น O
            player2.setOtherPlayer(player1); // ผู้เล่น O รู้จักผู้เล่น X
            player1.setOtherPlayer(player2); // ผู้เล่น X รู้จักผู้เล่น O
            player2.start();
            System.out.println("Player 2 connected");

            player1.join(); // รอให้ thread ของ player1 จบก่อน
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
        private ClientHandler otherPlayer; // ผู้เล่นฝั่งตรงข้าม

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
                    synchronized (Server.class) { // ป้องกันไม่ให้ 2 thread เล่นพร้อมกัน
                        while (currentPlayer != (playerSymbol == 'X' ? 1 : 2)) {
                            Server.class.wait(); // รอให้ถึงตาตัวเอง
                        }

                        out.println("Your turn. Current board:");
                        sendBoardToPlayer(out); // ส่งกระดานให้ผู้เล่นนี้ดู
                        out.println("Please enter a number (1-9): ");

                        int move = Integer.parseInt(in.readLine()) - 1; // รับช่องที่ผู้เล่นต้องการเล่น

                        if (move < 0 || move >= 9 || board[move] != '\0') {
                            out.println("Invalid move. Try again."); // ช่องไม่ว่าง
                            continue;
                        }

                        board[move] = playerSymbol; // ลงหมากในกระดาน

                        if (checkWin(playerSymbol)) {
                            out.println("WINNER_" + playerSymbol); // บอกผู้เล่นว่าเขาชนะ
                            otherPlayer.out.println("You Lose! " + playerSymbol + " wins"); // บอกอีกฝั่งว่าแพ้
                            sendBoardToPlayer(out); // ส่งกระดานล่าสุด
                            sendBoardToPlayer(otherPlayer.out);
                            break;
                        }

                        if (checkDraw()) {
                            out.println("DRAW"); // บอกทั้งคู่ว่าเสมอ
                            otherPlayer.out.println("DRAW");
                            sendBoardToPlayer(out);
                            sendBoardToPlayer(otherPlayer.out);
                            break;
                        }

                        otherPlayer.out.println("Opponent moved. Here's the board:");
                        sendBoardToPlayer(otherPlayer.out); // อัปเดตกระดานให้ผู้เล่นอีกฝั่ง

                        currentPlayer = (currentPlayer == 1) ? 2 : 1; // สลับผู้เล่น
                        Server.class.notifyAll(); // แจ้งอีก thread ว่าเล่นต่อได้
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close(); // ปิดการเชื่อมต่อเมื่อจบเกม
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }

        private void sendBoardToPlayer(PrintWriter out) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 9; i++) {
                sb.append(board[i] == '\0' ? (i + 1) : board[i]); // แสดงตัวเลขถ้ายังว่าง ไม่งั้นแสดง X/O
                sb.append(" ");
            }
            out.println(sb.toString().trim()); // ส่งข้อความที่เป็นสถานะกระดาน
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
                    return true; // ชนะตามเงื่อนไข
                }
            }
            return false;
        }

        private boolean checkDraw() {
            for (char cell : board) {
                if (cell == '\0') return false; // ถ้ายังมีช่องว่างอยู่ แสดงว่ายังไม่เสมอ
            }
            return true;
        }
    }
}

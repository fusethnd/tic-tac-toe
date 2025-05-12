package src;

import java.io.*; // สำหรับการสื่อสาร input/output กับ server
import java.net.*; // สำหรับสร้าง socket connection
import java.util.Scanner; // สำหรับรับ input จากผู้ใช้

public class Client {
    private static char currentPlayer = 'X';  // ผู้เล่นเริ่มต้นเป็น X (แต่ไม่ได้ใช้จริงในฝั่ง client นี้)
    private static Socket socket; // ตัวแปรเก็บการเชื่อมต่อกับ server
    private static PrintWriter out; // ส่งข้อความไปยัง server
    private static BufferedReader in; // รับข้อความจาก server

    public Client(String serverAddress, int port) {
        setupConnection(serverAddress, port); // เชื่อมต่อกับ server
        handleServerResponse(); // จัดการการรับข้อมูลจาก server
    }

    private static void setupConnection(String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port); // สร้างการเชื่อมต่อ socket ไปยัง server
            System.out.println("Connected to server at " + serverAddress + ":" + port);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // อ่านข้อความจาก server
            out = new PrintWriter(socket.getOutputStream(), true); // ส่งข้อความแบบ auto-flush
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage()); // แจ้งเมื่อเชื่อมต่อไม่ได้
            System.exit(1); // ปิดโปรแกรม
        }
    }

    private static void sendMove(int move) {
        out.println(move);  // ส่งหมายเลขช่อง (1-9) ที่ผู้เล่นเลือก ไปให้ server
        
    }

    private static void handleServerResponse() {
        try {
            String response;
            Scanner scanner = new Scanner(System.in); // รับค่าจาก keyboard

            while ((response = in.readLine()) != null) { // อ่านข้อความจาก server ทีละบรรทัด
                if (response.contains("Your turn")) {
                    System.out.println(response);  // แจ้งว่าถึงตาเรา
                    System.out.print("Enter your move (1-9): ");
                    int move = scanner.nextInt(); // รับค่าช่องที่ต้องการจะลงจากผู้ใช้
                    sendMove(move); // ส่งไปยัง server
                } else if (response.contains("You Win!")) {
                    System.out.println("Game Over: " + response);  // แสดงข้อความชนะ
                    break;
                } else if (response.contains("You Lose!")) {
                    System.out.println("Game Over: " + response);  // แสดงข้อความแพ้
                    break;
                } else if (response.contains("DRAW")) {
                    System.out.println("Game Over: " + response);  // แสดงข้อความเสมอ
                    break;
                } else {
                    System.out.println(response); // แสดงข้อความอื่น ๆ เช่น กระดานเกม
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling server response: " + e.getMessage()); // แจ้งเมื่อเกิดปัญหาการสื่อสาร
        }
    }

    public static void main(String[] args) {
        new Client("127.0.0.1", 5165);  // เริ่มโปรแกรมและเชื่อมต่อไปยัง server localhost ที่พอร์ต 5165
    }
}

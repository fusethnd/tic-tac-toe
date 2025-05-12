package src;

import javax.swing.*; // ใช้งาน GUI component เช่น JFrame, JButton
import java.awt.*; // ใช้งาน layout และการจัดวาง UI
import java.awt.event.ActionListener; // (ยังไม่ได้ใช้โดยตรงในโค้ดนี้)
import java.io.*; // ใช้งานสำหรับอ่านและเขียนข้อมูลผ่าน socket
import java.net.*; // ใช้งานสำหรับเชื่อมต่อ network ด้วย Socket

public class GUI {
    private JFrame frame; // หน้าต่างหลักของเกม
    private JButton[] buttons = new JButton[9]; // ปุ่มทั้งหมด 9 ช่องในตาราง XO
    private Socket socket; // socket สำหรับเชื่อมต่อกับ server
    private BufferedReader in; // รับข้อมูลจาก server
    private PrintWriter out; // ส่งข้อมูลไปยัง server
    private char playerSymbol; // ตัวอักษรที่แทนผู้เล่น (X หรือ O)
    private JLabel statusLabel; // แสดงข้อความว่าเป็นตาใคร

    public GUI(String serverAddress, int port) {
        setupGUI(); // สร้างหน้าตา GUI
        setupConnection(serverAddress, port); // เชื่อมต่อกับ server
        handleServerResponse(); // รอฟังคำตอบจาก server
    }

    private void setupConnection(String serverAddress, int port) {
        try {
            socket = new Socket(serverAddress, port); // สร้าง socket เชื่อมไปยัง server
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // สร้างตัวอ่านข้อความจาก server
            out = new PrintWriter(socket.getOutputStream(), true); // สร้างตัวส่งข้อความไป server
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Connection failed: " + e.getMessage()); // แจ้งเตือนถ้าเชื่อมต่อไม่สำเร็จ
            System.exit(1);
        }
    }

    private void setupGUI() {
        frame = new JFrame("Tic Tac Toe");
        frame.setSize(400, 400);
        frame.setLayout(new BorderLayout()); // ใช้ layout แบบแบ่งเป็นทิศ (NORTH, CENTER, SOUTH, ...)

        JPanel boardPanel = new JPanel(new GridLayout(3, 3)); // ตาราง 3x3 สำหรับวางปุ่ม
        Font font = new Font("Arial", Font.BOLD, 40); // ฟอนต์ใหญ่สำหรับปุ่ม

        for (int i = 0; i < 9; i++) {
            final int idx = i; // ต้องใช้ final เพื่อให้ใช้ใน lambda ได้
            buttons[i] = new JButton(""); // สร้างปุ่มเปล่าแต่ละช่อง
            buttons[i].setFont(font); // ตั้งฟอนต์
            buttons[i].setFocusPainted(false); // ไม่แสดงขอบเมื่อถูกเลือก
            buttons[i].addActionListener(e -> sendMove(idx + 1)); // เมื่อกดปุ่ม จะส่งตำแหน่งไปให้ server
            boardPanel.add(buttons[i]); // ใส่ปุ่มลงใน panel
        }

        statusLabel = new JLabel("Connecting...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        frame.add(statusLabel, BorderLayout.NORTH); // วางไว้ด้านบน
        frame.add(boardPanel, BorderLayout.CENTER); // วาง panel ไว้ตรงกลางของ frame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // ปิดโปรแกรมเมื่อปิดหน้าต่าง
        frame.setVisible(true); // แสดง frame ขึ้นมาบนหน้าจอ
    }

    private void sendMove(int move) {
        out.println(move);  // ส่งค่าหมายเลขช่อง (1-9) ที่ผู้เล่นเลือก ไปให้ server
        statusLabel.setText("Waiting for " + (playerSymbol == 'X' ? 'O' : 'X') + "..."); // แก้สถานะทันที
    }

    private void updateBoard(char[] board) {
        for (int i = 0; i < 9; i++) {
            buttons[i].setText(board[i] == '\0' ? "" : String.valueOf(board[i])); // แสดง X หรือ O บนปุ่ม หรือปล่อยว่าง
            buttons[i].setEnabled(board[i] == '\0'); // ปิดปุ่มถ้ามีคนเลือกแล้ว
        }
    }

    private void handleServerResponse() {
        new Thread(() -> { // ใช้ thread แยกเพื่อไม่ให้ UI ค้าง
            try {
                String response;
                while ((response = in.readLine()) != null) { // อ่านข้อความจาก server ทีละบรรทัด
                    if (response.contains("Welcome Player")) {
                        playerSymbol = response.charAt(response.length() - 1); // บันทึกว่าเราเป็น X หรือ O
                        frame.setTitle("Tic Tac Toe - You are " + playerSymbol); // แสดง title ของหน้าต่าง
                    } else if (response.contains("Your turn")) {
                        frame.setTitle("Tic Tac Toe - Your Turn (" + playerSymbol + ")");
                        statusLabel.setText("Your Turn (" + playerSymbol + ")");
                    } else if (response.startsWith("Opponent moved")) {
                        statusLabel.setText("Waiting for " + (playerSymbol == 'X' ? 'O' : 'X') + "...");
                    } else if (response.startsWith("WINNER") ||
                               response.contains("You Lose!") ||
                               response.contains("DRAW")) {
                        statusLabel.setText("Game Over");
                        JOptionPane.showMessageDialog(frame, response, "Game Over", JOptionPane.INFORMATION_MESSAGE); // แจ้งผลลัพธ์เกม
                        for (JButton button : buttons) button.setEnabled(false); // ปิดทุกปุ่ม
                    } else {
                        // ถ้าข้อมูลที่ได้รับคือสถานะกระดาน เช่น "X O 3 4 5 6 7 8 9"
                        String[] parts = response.trim().split(" ");
                        if (parts.length == 9) {
                            char[] board = new char[9];
                            for (int i = 0; i < 9; i++) {
                                char c = parts[i].charAt(0);
                                board[i] = (c == 'X' || c == 'O') ? c : '\0'; // ถ้ายังไม่ถูกเลือกให้เก็บเป็น null
                            }
                            updateBoard(board); // อัปเดต UI ตามกระดานล่าสุด
                        }
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Connection lost: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); // แจ้ง error หากหลุดจาก server
                System.exit(1);
            }
        }).start(); // เริ่ม thread รับข้อความ
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GUI("127.0.0.1", 5165)); // สร้างหน้าต่าง GUI และเชื่อมไปที่ localhost port 5165
    }
}
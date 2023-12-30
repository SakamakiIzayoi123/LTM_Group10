import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class client {
    public static final int MAX_DATA = 5;
    public static Socket socket;
    public static PrintWriter out;
    public static Scanner scanner;
    public static ArrayList<Integer> data = new ArrayList<Integer>();
    final int portNumber = 12345;`
    final String serverAddress = "localhost";
    public static int i_min = 0;
    public static int i_max = 100;
    public static int min = -10;
    public static int max = 10;

    // Hàm sinh số ngẫu nhiên trong khoảng [min, max]
    private static int random() {
        return (int) ((Math.random() * (max - min)) + min);
    }

    // Hám sinh dữ liệu ngẫu nhiên dựa trên dữ liệu cũ của data
    private static int randomData() {
        if (data.size() == 0) {
            return random();
        }
        int lastData = data.get(data.size() - 1);
        int newData = lastData + random();

        if (newData < i_min) {
            newData = i_min;
        }
        if (newData > i_max) {
            newData = i_max;
        }

        return newData;
    }

    // Hàm khởi tạo
    public void init() {
        try {
            // Kết nối đến server
            socket = new Socket(serverAddress, portNumber);
            System.out.println("Connected to server: " + socket.getInetAddress());

            // Tạo đối tượng PrintWriter để gửi dữ liệu đến server
            out = new PrintWriter(socket.getOutputStream(), true);

            // Tạo đối tượng Scanner để nhận dữ liệu từ server
            scanner = new Scanner(socket.getInputStream());

            // Gửi và nhận dữ liệu
            System.out.println("Server says: " + scanner.nextLine());
            out.println("Hello, server! This is the client.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm hủy
    public void destroy() {
        try {
            // Đóng kết nối
            out.close();
            scanner.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Kiểm tra xem socket của client có kết nối đến server hay không
    public static boolean isServerConnected() {
        return socket.isConnected() && !socket.isClosed();
    }

    // Gửi dữ liệu tới server (mọi kiểu dữ liệu)
    public static void send(Object data) {
        // Kiểm tra server có còn hoạt động không
        if (!isServerConnected()) {
            System.out.println("Server is not connected.");
            System.exit(0);
        }
        out.println(data);
        System.out.println("Client: " + data);
    }

    public static void main(String[] args) {
        client client = new client();
        client.init();

        // Gửi dữ liệu tới server sau mỗi 0.3 giây
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    // Gửi dữ liệu tới server
                    int newData = randomData();
                    data.add(newData);
                    send(newData);
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();

        // Nhận dữ liệu từ server
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        // Nhận dữ liệu từ server
                        String receivedData = scanner.nextLine();
                        System.out.println("Server: " + receivedData);
                        if (receivedData.equals("Not to water")) {
                            min = -3;
                            max = -1;
                        } else if (receivedData.equals("Water")) {
                            min = 1;
                            max = 3;
                        }
                        if (receivedData.equals("Exit")) {
                            System.exit(0);
                        }
                    } catch (Exception e) {
                        System.out.println("Server is not connected.");
                        System.exit(0);
                    }
                }
            }
        });
        thread2.start();

        // Đợi thread kia chạy xong
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        client.destroy();
    }
}

package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

public class server {
    public static final int Comparing_Data_Min = 30; // độ ẩm cho phép
    public static final int Comparing_Data_Max = 70; // độ ẩm cho phép
    public static final int MAX_DATA = 50;
    private static int clientCount = 0;
    private ServerSocket serverSocket;
    public static JFrame frame;
    public static JTabbedPane tabbedPane = new JTabbedPane();
    public static ArrayList<Integer> data = new ArrayList<Integer>();
    private static int count = 50;
    JFreeChart chart;
    public static CategoryDataset dataset;

    private static void addDataset(String rowKey, String columnKey, double value) {
        System.out.println(dataset.getColumnCount());
        // Nếu số dòng dữ liệu vượt quá MAX_DATA thì xóa dòng đầu tiên
        if (dataset.getColumnCount() >= MAX_DATA) {
            ((DefaultCategoryDataset) dataset).removeColumn(0);
        }
        ((DefaultCategoryDataset) dataset).addValue(value, rowKey, columnKey);
    }

    public void init(int portNumber) {
        try {
            // Tạo server socket
            serverSocket = new ServerSocket(portNumber);
            System.out.println("Server is listening on port " + portNumber);

            frame = new JFrame("Swing App");
            frame.setSize(640, 480);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            dataset = new DefaultCategoryDataset();

            chart = ChartFactory.createLineChart(
                    "Humidity Line Chart", // Tiêu đề biểu đồ
                    "Category", // Nhãn trục x
                    "Humidity(%)", // Nhãn trục y
                    dataset, // Dữ liệu
                    PlotOrientation.VERTICAL, // Hướng biểu đồ
                    true, // Hiển thị legend
                    true, // Hiển thị tooltips
                    true // Hiển thị URLs
            );

            // trục y có giá trị từ 0 đến 100
            chart.getCategoryPlot().getRangeAxis().setRange(0, 100);

            // ẩn trục x
            chart.getCategoryPlot().getDomainAxis().setVisible(false);

            // ẩn grid line
            chart.getCategoryPlot().setDomainGridlinesVisible(false);

            // ẩn legend
            chart.getLegend().setVisible(false);

            // Thêm biểu đồ vào JFrame
            ChartPanel chartPanel = new ChartPanel(chart);
            tabbedPane.addTab("Trung bình", chartPanel);
            frame.add(tabbedPane);
            frame.setVisible(true);

            // Thêm dữ liệu mẫu
            for (int i = 0; i < MAX_DATA; i++) {
                addDataset("humidity", String.valueOf(i), 0);
            }

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        // Lấy trung bình cộng của dữ liệu
                        double avg = 0;
                        for (int i = 0; i < data.size(); i++) {
                            avg += data.get(i);
                        }
                        avg /= data.size();
                        data.clear();
                        // thêm dữ liệu vào biểu đồ
                        addDataset("humidity", String.valueOf(count), avg);
                        count++;
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            thread.start();

            while (true) {
                // Chấp nhận kết nối từ client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Tạo một luồng mới để xử lý kết nối với client này
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();

                // Tăng số lượng client
                clientCount++;
                System.out.println("Number of clients: " + clientCount);

                CategoryDataset node_dataset = new DefaultCategoryDataset();

                JFreeChart node_Chart = ChartFactory.createLineChart(
                        "Humidity Line Chart", // Tiêu đề biểu đồ
                        "Category", // Nhãn trục x
                        "Humidity(%)", // Nhãn trục y
                        node_dataset, // Dữ liệu
                        PlotOrientation.VERTICAL, // Hướng biểu đồ
                        true, // Hiển thị legend
                        true, // Hiển thị tooltips
                        true // Hiển thị URLs
                );

                node_Chart.getCategoryPlot().getRangeAxis().setRange(0, 100);
                node_Chart.getCategoryPlot().getDomainAxis().setVisible(false);
                node_Chart.getCategoryPlot().setDomainGridlinesVisible(false);
                node_Chart.getLegend().setVisible(false);

                ChartPanel node_chartPanel = new ChartPanel(node_Chart);
                tabbedPane.addTab("Node" + String.valueOf(clientCount), node_chartPanel);
                clientHandler.setDataset(node_dataset);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        try {
            // Đóng server socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server socket closed.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final PrintWriter out;
        private CategoryDataset node_dataset;

        public ClientHandler(Socket clientSocket) throws IOException {
            this.clientSocket = clientSocket;
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        }

        public void setDataset(CategoryDataset node_dataset) {
            this.node_dataset = node_dataset;
        }

        @Override
        public void run() {
            try {
                // Tạo đối tượng Scanner để nhận dữ liệu từ client
                Scanner scanner = new Scanner(clientSocket.getInputStream());

                // Gửi chào mừng đến client
                out.println("Hello, client! Welcome to the server.");
                scanner.nextLine();

                // Nhận và in ra dữ liệu từ client
                while (scanner.hasNextLine()) {
                    String clientMessage = scanner.nextLine();
                    int humidity = Integer.parseInt(clientMessage);
                    if (humidity > Comparing_Data_Max) {
                        out.println("Not to water");
                    } else if (humidity < Comparing_Data_Min) {
                        out.println("Water");
                    } else {
                        out.println("dcm");
                    }

                    data.add(humidity);
                    // thêm dữ liệu vào biểu đồ
                    ((DefaultCategoryDataset) node_dataset).addValue(humidity, "humidity", clientMessage);
                }

                // Giảm số lượng client khi kết nối đóng
                clientCount--;
                System.out.println("Number of clients: " + clientCount);

                // Đóng kết nối
                out.close();
                scanner.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        server server = new server();
        server.init(12345);

        // Server will keep running until manually stopped

        // To stop the server, you can add a shutdown hook or handle it in a different
        // way
    }
}

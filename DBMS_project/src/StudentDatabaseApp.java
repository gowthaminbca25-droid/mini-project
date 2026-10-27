package src;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDatabaseApp {
    private static final String URL = "jdbc:mysql://localhost:3306/student_db";
    private static final String USER = "root";
    private static final String PASSWORD = "root"; // 
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver Missing!");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // --- CRUD OPERATIONS ---
    
    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        String query = "SELECT * FROM students";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                students.add(new Student(rs.getInt("student_id"), rs.getString("name"), rs.getString("email"), rs.getString("course"), rs.getInt("semester")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return students;
    }

    public boolean addStudent(String name, String email, String course, int semester) {
        String query = "INSERT INTO students (name, email, course, semester) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, course);
            pstmt.setInt(4, semester);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean updateStudent(int id, String name, String email, String course, int semester) {
        String query = "UPDATE students SET name = ?, email = ?, course = ?, semester = ? WHERE student_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, course);
            pstmt.setInt(4, semester);
            pstmt.setInt(5, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean deleteStudent(int id) {
        String query = "DELETE FROM students WHERE student_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    // --- MAIN METHOD: LIGHTWEIGHT HTTP SERVER ENGINE ---
    
    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        StudentDatabaseApp app = new StudentDatabaseApp();

        System.out.println("🚀 Starting Server on port " + port + "...");

        // Main API endpoint: /api/students
        server.createContext("/api/students", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                // Handle Cross-Origin Resource Sharing (CORS) so your HTML file can talk to Java
                exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

                if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();
                String response = "";
                int statusCode = 200;

                try {
                    if (method.equalsIgnoreCase("GET")) {
                        // FETCH ALL STUDENTS
                        List<Student> list = app.getAllStudents();
                        StringBuilder json = new StringBuilder("[");
                        for (int i = 0; i < list.size(); i++) {
                            Student s = list.get(i);
                            json.append(String.format("{\"student_id\":%d,\"name\":\"%s\",\"email\":\"%s\",\"course\":\"%s\",\"semester\":%d}",
                                    s.getStudentId(), s.getName(), s.getEmail(), s.getCourse(), s.getSemester()));
                            if (i < list.size() - 1) json.append(",");
                        }
                        json.append("]");
                        response = json.toString();
                        exchange.getResponseHeaders().set("Content-Type", "application/json");

                    } else if (method.equalsIgnoreCase("POST")) {
                        // ADD NEW STUDENT
                        String body = readRequestBody(exchange);
                        String name = parseJsonField(body, "name");
                        String email = parseJsonField(body, "email");
                        String course = parseJsonField(body, "course");
                        int semester = Integer.parseInt(parseJsonField(body, "semester"));
                        
                        app.addStudent(name, email, course, semester);
                        response = "{\"message\":\"Student created successfully\"}";

                    } else if (method.equalsIgnoreCase("PUT")) {
                        // UPDATE EXISTING STUDENT
                        String[] parts = path.split("/");
                        int id = Integer.parseInt(parts[parts.length - 1]);
                        String body = readRequestBody(exchange);
                        String name = parseJsonField(body, "name");
                        String email = parseJsonField(body, "email");
                        String course = parseJsonField(body, "course");
                        int semester = Integer.parseInt(parseJsonField(body, "semester"));

                        app.updateStudent(id, name, email, course, semester);
                        response = "{\"message\":\"Student updated successfully\"}";

                    } else if (method.equalsIgnoreCase("DELETE")) {
                        // DELETE STUDENT
                        String[] parts = path.split("/");
                        int id = Integer.parseInt(parts[parts.length - 1]);
                        app.deleteStudent(id);
                        response = "{\"message\":\"Student deleted successfully\"}";
                    }
                } catch (Exception e) {
                    statusCode = 500;
                    response = "{\"error\":\"" + e.getMessage() + "\"}";
                }

                byte[] bytes = response.getBytes("UTF-8");
                exchange.sendResponseHeaders(statusCode, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("✅ Backend Live! Server running at http://localhost:8080/api/students");
    }

    // Helper utilities to process data streams without external dependencies
    private static String readRequestBody(HttpExchange exchange) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) { sb.append(line); }
        return sb.toString();
    }

    private static String parseJsonField(String json, String field) {
        String key = "\"" + field + "\"";
        int start = json.indexOf(key);
        if (start == -1) return "";
        start = json.indexOf(":", start) + 1;
        while (json.charAt(start) == ' ' || json.charAt(start) == '"') start++;
        int end = start;
        while (end < json.length() && json.charAt(end) != '"' && json.charAt(end) != ',' && json.charAt(end) != '}') end++;
        return json.substring(start, end).trim();
    }
}

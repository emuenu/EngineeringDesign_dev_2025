package servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

@WebServlet("/DBmanager/*")
public class DBServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // MySQLサーバへの接続の情報 (characterEncoding=UTF-8 を追加)
    private static final String JDBC_URL = "jdbc:mysql://db:3306/tango?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "3710";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // リクエストの文字コードをUTF-8に設定
        request.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();

        if (pathInfo != null && pathInfo.equalsIgnoreCase("/GetAllTable")) {
            handleJsonResponse(request, response);
        } else if (pathInfo != null && pathInfo.equalsIgnoreCase("/Delete")) {
            handleDeleteRequest(request, response);
        } else if (pathInfo != null && pathInfo.equalsIgnoreCase("/CountIncrement")) {
            handleCountRequest(request, response);
        } else if (pathInfo == null || pathInfo.equals("/")) {
            handleHtmlResponse(request, response);
        } else {
            showError(request, response);
        }
    }

    private void handleHtmlResponse(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String English_word = request.getParameter("English_word");
        String Japanese_word = request.getParameter("Japanese_word");

        boolean gotAddValue = (English_word != null && !English_word.isEmpty())
                && (Japanese_word != null && !Japanese_word.isEmpty());

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {

                if (gotAddValue) {
                    int maxId = 0;
                    String maxIdSql = "SELECT MAX(ID) AS maxId FROM wordstorage";
                    try (Statement stmt = conn.createStatement();
                            ResultSet rs = stmt.executeQuery(maxIdSql)) {
                        if (rs.next()) {
                            maxId = rs.getInt("maxId");
                        }
                    }

                    int newId = maxId + 1;

                    String insertSql = "INSERT INTO wordstorage (ID, English, Japanese) VALUES (?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                        pstmt.setInt(1, newId);
                        pstmt.setString(2, English_word);
                        pstmt.setString(3, Japanese_word);
                        pstmt.executeUpdate();
                    }

                    String insertProgressSql = "INSERT INTO userprogress (userprogress_ID, QuestionCount, CorrectPickCount) VALUES (?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertProgressSql)) {
                        pstmt.setInt(1, newId);
                        pstmt.setInt(2, 0);
                        pstmt.setInt(3, 0);
                        pstmt.executeUpdate();
                    }
                }

                response.setContentType("text/html; charset=UTF-8");
                PrintWriter out = response.getWriter();

                out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>テーブル内容</title></head><body>");

                out.println("<h1>wordstorage テーブルの内容</h1>");
                out.println("<table border='1'><tr><th>ID</th><th>English</th><th>Japanese</th></tr>");

                String selectWordSql = "SELECT * FROM wordstorage";
                try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(selectWordSql)) {
                    while (rs.next()) {
                        out.println("<tr>");
                        out.println("<td>" + rs.getInt("ID") + "</td>");
                        out.println("<td>" + rs.getString("English") + "</td>");
                        out.println("<td>" + rs.getString("Japanese") + "</td>");
                        out.println("</tr>");
                    }
                }
                out.println("</table>");

                out.println("<h1>userprogress テーブルの内容</h1>");
                out.println("<table border='1'><tr><th>ID</th><th>その他のカラム</th></tr>");

                String selectUserSql = "SELECT * FROM userprogress";
                try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(selectUserSql)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();

                    out.println("<tr>");
                    for (int i = 1; i <= columnCount; i++) {
                        out.println("<th>" + meta.getColumnName(i) + "</th>");
                    }
                    out.println("</tr>");

                    while (rs.next()) {
                        out.println("<tr>");
                        for (int i = 1; i <= columnCount; i++) {
                            out.println("<td>" + rs.getString(i) + "</td>");
                        }
                        out.println("</tr>");
                    }
                }
                out.println("</table>");
                out.println("<p><a href='index.html'>戻る</a></p>");
                out.println("</body></html>");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError(request, response);
        }
    }

    private void handleJsonResponse(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            try (Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS)) {

                List<Map<String, Object>> wordstorageList = new ArrayList<>();
                List<Map<String, Object>> userprogressList = new ArrayList<>();

                try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT * FROM wordstorage")) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(meta.getColumnName(i), rs.getObject(i));
                        }
                        wordstorageList.add(row);
                    }
                }

                try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery("SELECT * FROM userprogress")) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(meta.getColumnName(i), rs.getObject(i));
                        }
                        userprogressList.add(row);
                    }
                }

                Map<String, Object> result = new HashMap<>();
                result.put("wordstorage", wordstorageList);
                result.put("userprogress", userprogressList);

                Gson gson = new Gson();
                String json = gson.toJson(result);
                out.print(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError(request, response);
        }
    }

    private void handleDeleteRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        Connection conn = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
            conn.setAutoCommit(false);

            String idParam = request.getParameter("id");
            if (idParam == null || idParam.isEmpty()) {
                throw new IllegalArgumentException("ID が指定されていません");
            }

            int id = Integer.parseInt(idParam);

            String checkWordSql = "SELECT COUNT(*) FROM wordstorage WHERE ID = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(checkWordSql)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        if (count == 0) {
                            out.println("<html><body>");
                            out.println("<h2>指定された ID は wordstorage に存在しません。</h2>");
                            out.println("<p><a href='/Servlet/DBmanager/'>戻る</a></p>");
                            out.println("</body></html>");
                            return;
                        }
                    }
                }
            }

            String deleteUserProgressSql = "DELETE FROM userprogress WHERE userprogress_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteUserProgressSql)) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
            }

            String deleteWordStorageSql = "DELETE FROM wordstorage WHERE ID = ?";
            int wordStorageDeleted;
            try (PreparedStatement pstmt = conn.prepareStatement(deleteWordStorageSql)) {
                pstmt.setInt(1, id);
                wordStorageDeleted = pstmt.executeUpdate();
            }

            if (wordStorageDeleted == 0) {
                throw new Exception("wordstorage テーブルの削除に失敗しました。");
            }

            conn.commit();

            out.println("<html><body>");
            out.println("<h2>ID " + id + " の削除が成功しました。</h2>");
            out.println("<p><a href='/Servlet/DBmanager/'>戻る</a></p>");
            out.println("</body></html>");
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            showError(request, response);
        } finally {
            if (conn != null)
                try {
                    conn.close();
                } catch (SQLException ignore) {
                }
        }
    }

    private void handleCountRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
            conn.setAutoCommit(false);

            String idParam = request.getParameter("id");
            String correctBoolParam = request.getParameter("correct");

            int id = Integer.parseInt(idParam);
            boolean wasCorrect = "1".equals(correctBoolParam);

            String sql;
            if (wasCorrect) {
                sql = "UPDATE userprogress SET QuestionCount = QuestionCount + 1, CorrectPickCount = CorrectPickCount + 1 WHERE userprogress_ID = ?";
            } else {
                sql = "UPDATE userprogress SET QuestionCount = QuestionCount + 1 WHERE userprogress_ID = ?";
            }

            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.executeUpdate();

            conn.commit();

            response.setContentType("text/html; charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("<html><body>");
            out.println("<h2>更新が成功しました。</h2>");
            out.println("<p>ID: " + id + " のカウントを更新しました。</p>");
            out.println("<p><a href='/Servlet/DBmanager/'>戻る</a></p>");
            out.println("</body></html>");

        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            showError(request, response);
        } finally {
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (SQLException ignore) {
                }
            if (conn != null)
                try {
                    conn.close();
                } catch (SQLException ignore) {
                }
        }
    }

    private void showError(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>エラー</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>エラーが発生しました</h1>");
        out.println("</body>");
        out.println("</html>");
    }
}
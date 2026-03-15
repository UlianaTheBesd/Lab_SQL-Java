import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SocialnetCalls {

    // 1. Создание БД.
    public static void createDatabase() throws SQLException {
        // Используем PreparedStatement и прямой CALL
        try (Connection connection = DatabaseConnection.getAdminConnection();
             PreparedStatement pstmt = connection.prepareStatement("CALL create_db_socialnet()")) {
            pstmt.execute();
            System.out.println("БД создана (/существует).");
        }
    }

    // 2. Удаление БД.
    public static void dropDatabase() throws SQLException {
        try (Connection connection = DatabaseConnection.getAdminConnection();
             PreparedStatement pstmt = connection.prepareStatement("CALL drop_db_socialnet()")) {
            pstmt.execute();
            System.out.println("БД удалена.");
        }
    }

    // 3. Создание таблицы.
    public static void createTable() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             // Здесь у вас уже почти правильно, но лучше использовать PreparedStatement для единообразия
             PreparedStatement pstmt = connection.prepareStatement("CALL create_posts_table()")) {
            pstmt.execute();
            System.out.println("Таблица создана.");
        }
    }

    // 4. Очистка таблицы.
    public static void clearTable() throws SQLException {
        checkAdmin();
        try (Connection connection = DatabaseConnection.getConnection();
             // Убираем фигурные скобки
             PreparedStatement pstmt = connection.prepareStatement("CALL clear_table_socialpost()")) {
            pstmt.execute();
            System.out.println("Таблица очищена.");
        }
    }

    // 5. Добавление поста.
    public static void addPost(String content, String author, boolean isPrivate) throws SQLException {
        String sql = "CALL add_post(?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             CallableStatement stmt = connection.prepareCall(sql)) {

            stmt.setString(1, content);
            stmt.setString(2, author);
            stmt.setBoolean(3, isPrivate);
            stmt.execute(); // НЕ executeQuery, а execute

            System.out.println("Пост успешно добавлен");
        } catch (SQLException e) {
            System.err.println("Ошибка при добавлении поста: " + e.getMessage());
            throw e;
        }
    }

    /*
    // 6. Поиск по автору.
    public static List<Post> searchPostsByAuthor(String author) throws SQLException {
        List<Post> posts_list = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             // Первый "?" - возвращаемый параметр.
             // Второй "?" - плейсхолдер.
             CallableStatement cs = connection.prepareCall("{? = call search_post_by_author(?)}")) {

            // Types.OTHER - в PostgreSQL для возврата курсора (набора строк).
            // (курсор не существует сам по себе — он всегда привязан к конкретному SELECT запросу).
            cs.registerOutParameter(1, Types.OTHER); // cs.registerOutParameter(1, Types.OTHER) - место, куда вернётся курсор из первого "?".
            cs.setString(2, author); // cs.setString(2, author) - переменная на вход второму "?".
            cs.execute();

            // ResultSet — весь набор найденных строк.
            // Приводим данные (объект), на которые ссылается курсор к ResultSet - как бы, создавая иллюзию, что мы передали весь список, хотя это только ссылка.
            ResultSet resset = (ResultSet) cs.getObject(1);
            while (resset.next()) { // Из таблицы (сделанной только из курсора) берём новые и новые строки, добавляя в список найденных постов.
                posts_list.add(extractPostFromResultSet(resset));
            }
            System.out.println("Найдено постов: " + posts_list.size());
        }
        return posts_list;
    }
    */

    // 6. Поиск по автору.
    public static List<Post> searchPostsByAuthor(String author) throws SQLException {
        List<Post> posts_list = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT * FROM search_post_by_author(?)"
             )) {
            ps.setString(1, author);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts_list.add(extractPostFromResultSet(rs));
                }
            }
        }
        return posts_list;
    }


    // 7. Получение всех постов.
    public static List<Post> getAllPosts() throws SQLException {
        List<Post> posts = new ArrayList<>();

        // Диагностика
        System.out.println("Метод getAllPosts() вызван");
        System.out.println("Попытка подключения...");

        try (Connection connection = DatabaseConnection.getConnection()) {
            System.out.println("Подключение успешно: " + connection);

            // Проверим, какие функции есть
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                        "SELECT proname FROM pg_proc WHERE proname LIKE '%post%'"
                );
                System.out.println("Доступные функции:");
                while (rs.next()) {
                    System.out.println("  - " + rs.getString(1));
                }
            }

            // Теперь вызываем see_all_posts
            try (Statement stmt = connection.createStatement()) {

                ResultSet rs = stmt.executeQuery(
                        "SELECT * FROM see_all_posts()"
                );
                while (rs.next()) {
                    posts.add(extractPostFromResultSet(rs));
                }
                System.out.println("Всего постов: " + posts.size());
            }
        } catch (SQLException e) {
            System.err.println("ОШИБКА в getAllPosts: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        return posts;
    }

    // 8. Обновление поста.
    public static void updatePost(int id, String content, boolean isPrivate)
            throws SQLException {
        checkAdmin();
        // Прямой вызов процедуры через CALL без фигурных скобок
        String sql = "CALL update_post(?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.setString(2, content);
            pstmt.setBoolean(3, isPrivate);

            pstmt.execute();
            System.out.println("Пост обновлен.");
        }
    }

    // 9. Лайк.
    public static void likePost(int postId) throws SQLException {
        // Убираем фигурные скобки и слово call внутри них, пишем просто CALL
        String sql = "CALL change_likes_amount(?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setInt(1, postId);
            pstmt.execute();
            System.out.println("+ Лайк.");
        }
    }


    // 10. Удаление постов автора.
    public static void deletePostsByAuthor(String author) throws SQLException {
        checkAdmin();
        // Прямой вызов процедуры
        String sql = "CALL delete_post_by_author(?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, author);
            pstmt.execute();
            System.out.println("Посты автора " + author + " удалены.");
        }
    }


    // 11. Создание пользователя.
    public static void createUser(String username, String password, boolean isAdmin) throws SQLException {
        checkAdmin();
        // Используем ПРЯМОЙ CALL и обычное соединение с lab_5_6
        String sql = "CALL create_user_socialnet(?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection(); // Подключаемся к lab_5_6
             PreparedStatement pstmt = connection.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setBoolean(3, isAdmin);
            pstmt.execute();
            System.out.println("Пользователь " + username + " создан в lab_5_6.");
        }
    }


    // 12. Удаление пользователя.
    public static void dropUser(String username) throws SQLException {
        checkAdmin();
        try (Connection connection = DatabaseConnection.getAdminConnection();
             CallableStatement cs = connection.prepareCall("{call drop_user_socialnet(?)}")) {
            cs.setString(1, username);
            cs.execute();
            System.out.println("Пользователь " + username + " удалён.");
        }
    }

    // Проверка прав администратора.
    private static void checkAdmin() throws SQLException {
        if (!DatabaseConnection.isCurrentUserAdmin()) {
            throw new SQLException("Недостаточно прав у пользователя. Пользователь обязан иметь права Администратора.");
        }
    }

    // Извлечение поста из ResultSet.
    private static Post extractPostFromResultSet(ResultSet rs) throws SQLException {
        return new Post(
                rs.getInt("id"),
                rs.getString("content_text"),
                rs.getString("author"),
                rs.getInt("likes_counter"),
                rs.getBoolean("is_private")
        );
    }
}

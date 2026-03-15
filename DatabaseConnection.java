import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String url = "jdbc:postgresql://localhost:5432/";
    // jdbc:... - JDBC (Java Database Connectivity) - программный интерфейс (API) для языка Java, который позволяет приложениям взаимодействовать с СУБД (MySQL, PostgreSQL...).
    // (то есть, предоставляет все для этого необходимое - классы, методы, т.п., "инструменты" для такого взаимодействия).
    // :postgresql:... - Выбор драйвера языка для PostgreSQL.
    // localhost:5432 - Порт, по которому можно подлючиться к БД (обычно на этом порту, но можно удостовериться через cmd, насолько я помню).

    private static final String admin_url = "jdbc:postgresql://localhost:5432/postgres";
    private static final String user = "MY_USERNAME_WAS_HERE";
    private static final String password = "MY_PASSWORD_WAS_HERE"; // Указать мой пароль.

    private static String currentUser = "MY_USERNAME_WAS_HERE";
    private static String currentPassword = "MY_PASSWORD_WAS_HERE"; // Указать мой пароль (ещё раз).
    private static boolean isAdmin = true;

    public static Connection getConnection() throws SQLException {
        // Если подключение идет от ДЕФОЛТНОГО пользователя - подключаемся просто к postgres.
        // Если заходит пользователь - подключаемся lab_5_6 (БД social network).
        // УПРОЩЕНО: Всегда подключаемся к lab_5_6, потому что именно там наша таблица
        String url_making = url + "lab_5_6";
        System.out.println("Подключение к: " + url_making + " как " + currentUser);

        // DriverManager - это сервисный класс, который управляет набором драйверов баз данных и помогает устанавливать соединения с ними.
        // Он ведёт список доступных драйверов и выбирает подходящий для какой-либо БД на основе URL (например, jdbc:postgresql).
        // Через статический метод getConnection() он открывает физическое соединение с базой данных, возвращая объект типа Connection.
        // (Объект Connection - TCP/IP соединение, через которое информация (байты данных) передаются сервер БД).
        return DriverManager.getConnection(url_making, currentUser, currentPassword);
    }

    // Получить соединение с postgres как админ.
    public static Connection getAdminConnection() throws SQLException {
        System.out.println("Админ. подключение к: " + admin_url);
        return DriverManager.getConnection(admin_url, user, password);
    }

    // Установить имя и пароль для пользователя (+ админку).
    public static void setCurrentUser(String username, String password, boolean admin) {
        currentUser = (username != null) ? username.trim() : null;
        currentPassword = (password != null) ? password.trim() : null;
        isAdmin = admin;
        System.out.println("Логин: [" + currentUser + "], Пароль: [" + currentPassword + "]");
    }

    // Проверка пользователя на админа.
    public static boolean isCurrentUserAdmin() {
        return isAdmin;
    }

    // Получить имя текущей БД (добавлено для отладки)
    public static String getCurrentDatabase() {
        return "lab_5_6";
    }
}

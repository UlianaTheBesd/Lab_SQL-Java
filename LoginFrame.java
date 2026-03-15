import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton adminLoginButton;
    private JButton guestLoginButton;
    private JLabel statusLabel;

    public LoginFrame() {
        setTitle("Вход");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setResizable(false);

        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10)); // margins (отступы) между панельками.
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // (у BorderFactory есть множество других настроек для объектов).

        // Заголовок.
        JLabel titleLabel = new JLabel("Вход в систему", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Times New Roman", Font.BOLD, 20));
        titleLabel.setForeground(new Color(0, 102, 204));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // panelForForms: Панель с полями для ввода.
        JPanel panelForForms = new JPanel(new GridBagLayout()); // GridBagLayout - более умная версия GridLayout, умеет больше функций.
        GridBagConstraints gbc = new GridBagConstraints(); // GridBagConstraints - набор инструментов для типа расположения объектов GridBagLayout.
        gbc.insets = new Insets(5, 5, 5, 5); // "inset" - вкладка, вкладывать.
        // Значит, что в панельке panelForForms "рабочая зона" (куда смогут поместиться все объекты-виджеты)
        // будет начинаться с отступами в 5 пикселей от всех концов панельки.
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // panelForForms: Поле для ввода имени.
        gbc.gridx = 0;
        gbc.gridy = 0;
        panelForForms.add(new JLabel("Имя пользователя:"), gbc);

        usernameField = new JTextField(20); // Ширина объекта (20 * средняя ширина буквы в пикселях).
        gbc.gridx = 1;
        panelForForms.add(usernameField, gbc);

        // panelForForms: Поле для ввода пароля.
        gbc.gridx = 0;
        gbc.gridy = 1;
        panelForForms.add(new JLabel("Пароль:"), gbc);

        passwordField = new JPasswordField(20);
        gbc.gridx = 1;
        panelForForms.add(passwordField, gbc);

        mainPanel.add(panelForForms, BorderLayout.CENTER);

        // panelForButtons: Панель для кнопок.
        JPanel panelForButtons = new JPanel(new GridLayout(2, 1, 10, 10));

        adminLoginButton = new JButton("Войти от имени администратора");
        adminLoginButton.setBackground(new Color(0, 153, 76));
        adminLoginButton.setForeground(Color.WHITE);
        adminLoginButton.setFont(new Font("Times New Roman", Font.BOLD, 12));
        adminLoginButton.addActionListener(e -> loginAsAdmin()); // ВОЙТИ КАК АДМИНИСТРАТОР - ФУНКЦИЯ.

        guestLoginButton = new JButton("Войти как гость");
        guestLoginButton.setBackground(new Color(0, 102, 204));
        guestLoginButton.setForeground(Color.WHITE);
        guestLoginButton.setFont(new Font("Times New Roman", Font.BOLD, 12));
        guestLoginButton.addActionListener(e -> loginAsGuest()); // ВОЙТИ КАК ГОСТЬ - ФУНКЦИЯ.

        panelForButtons.add(adminLoginButton);
        panelForButtons.add(guestLoginButton);

        mainPanel.add(panelForButtons, BorderLayout.SOUTH);

        statusLabel = new JLabel(" ", SwingConstants.CENTER); // Тут будет написано, например, какое-то послание для пользователя.
        statusLabel.setForeground(Color.RED);
        mainPanel.add(statusLabel, BorderLayout.NORTH);

        add(mainPanel);
    }

    /*
    // ВОЙТИ КАК АДМИНИСТРАТОР - ФУНКЦИЯ.
    private void loginAsAdmin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) { // Поля не должны быть пустыми.
            showStatus("Введите имя пользователя и пароль."); // Ниже есть определение функции. Она меняет написанное в statusLabel на другое сообщение.
            return;
        }

        setButtonsEnabled(false); // (пока идёт проверка подключения - кнопки не будут работать).
        statusLabel.setForeground(Color.BLUE);
        showStatus("Идёт проверка подключения...");

        // Выполняем в отдельном потоке, чтобы не останавливать работу главного потока EDT
        // (Отвечает за все графически окна - писала об этом в Main.java).
        // (Если у нас все делает один поток, в нашем случае, этим потоком вызовется быть EDT - программа самого окна моет зависнуть.
        // Поэтому, даём задачу подключения другому потоку).
        new Thread(() -> { // Новый поток позже, получив данные при подключении с БД, вызывает SwingUtilities.invokeLater() - что передаёт задачу EDT.
            try {
                DatabaseConnection.setCurrentUser(username, password, true);

                // ПРОБУЕМ ПОЛУЧИТЬ ПОСТЫ
                try {
                    SocialnetCalls.getAllPosts(); // Если таблица есть - всё хорошо
                } catch (SQLException e) {
                    // Если ошибка "relation does not exist" - создаём таблицу
                    if (e.getMessage().contains("relation") && e.getMessage().contains("does not exist")) {
                        System.out.println("Таблица не найдена. Создаём...");
                        showStatus("Создание таблицы...");

                        SocialnetCalls.createTable(); // Создаём таблицу через хранимую процедуру

                        // Добавляем тестовый пост (чтобы было что показать)
                        SocialnetCalls.addPost(
                                "Добро пожаловать в соцсеть!",
                                username,
                                null,
                                false
                        );

                        System.out.println("Таблица и тестовый пост созданы.");
                    } else {
                        throw e; // Если другая ошибка - пробрасываем дальше
                    }
                }

                // Проверяем, что теперь всё работает
                SocialnetCalls.getAllPosts(); // Для проверки

                SwingUtilities.invokeLater(() -> {
                    showStatus("Вход осуществлён успешно. Загрузка...");
                    openPostMakingFrame();
                });

            } catch (SQLException ex) {
                SwingUtilities.invokeLater(() -> { // (разбирала ранее в Main.java).
                    setButtonsEnabled(true);
                    statusLabel.setForeground(Color.RED);
                    showStatus("ОШИБКА: " + ex.getMessage());

                    DatabaseConnection.setCurrentUser("postgres", "postgres", true); // Возврат настроек по умолчанию.
                });
            }
        }).start();
    }
    */

    // ВОЙТИ КАК АДМИНИСТРАТОР - ФУНКЦИЯ.
    private void loginAsAdmin() {


        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Введите имя пользователя и пароль.");
            return;
        }

        setButtonsEnabled(false);
        statusLabel.setForeground(Color.BLUE);
        showStatus("Идёт проверка подключения...");

        new Thread(() -> {
            try {
                DatabaseConnection.setCurrentUser(username, password, true);
                SocialnetCalls.createTable();
                SocialnetCalls.getAllPosts();

                // ОТКРЫВАЕМ ГЛАВНОЕ ОКНО
                SwingUtilities.invokeLater(() -> {
                    showStatus("Вход осуществлён успешно. Загрузка...");
                    openPostMakingFrame();
                });

            } catch (SQLException ex) {
                SwingUtilities.invokeLater(() -> {
                    setButtonsEnabled(true);
                    statusLabel.setForeground(Color.RED);
                    showStatus("ОШИБКА: " + ex.getMessage());
                    DatabaseConnection.setCurrentUser("postgres", "postgres", true);
                });
                ex.printStackTrace();
            }
        }).start();
    }

    // ВОЙТИ КАК ГОСТЬ - ФУНКЦИЯ.
    private void loginAsGuest() {
        setButtonsEnabled(false);
        statusLabel.setForeground(Color.BLUE);
        showStatus("Идёт обновление входа для гостя...");

        new Thread(() -> {
            // 1. Сначала заходим под админом, чтобы иметь права на создание/изменение ролей
            DatabaseConnection.setCurrentUser("postgres", "uliana645", true);

            try {
                SocialnetCalls.createUser("guest", "guest", false);
                System.out.println("Создан новый гость.");
            } catch (SQLException e) {
                System.out.println("Гость уже был, обновляем ему пароль на 'guest'...");
                // Выполните прямой SQL запрос через соединение админа:
                try (Connection conn = DatabaseConnection.getAdminConnection();
                     Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER ROLE guest WITH PASSWORD 'guest'");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }

            // 2. Теперь переключаемся на гостя с подтвержденным паролем
            DatabaseConnection.setCurrentUser("guest", "guest", false);

            SwingUtilities.invokeLater(() -> {
                showStatus("Выполнен вход как гостя.");
                openPostMakingFrame();
            });

        }).start();
    }
    private void showStatus(String message) {
        statusLabel.setText(message);
    }

    private void openPostMakingFrame() { // Переключаемся на PostMakingFrame.
        PostMakingFrame mainFrame = new PostMakingFrame();
        mainFrame.setVisible(true);

        this.dispose(); // Закрываем окно LoginFrame.
    }

    // Включение / выключение кнопок.
    private void setButtonsEnabled(boolean flag) {
        adminLoginButton.setEnabled(flag);
        guestLoginButton.setEnabled(flag);
        usernameField.setEnabled(flag);
        passwordField.setEnabled(flag);
    }
}

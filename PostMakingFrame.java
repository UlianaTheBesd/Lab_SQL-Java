import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

public class PostMakingFrame extends JFrame {
    private JTable postsTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton likeButton;
    private JButton searchButton;
    private JButton refreshButton;
    private JButton clearButton;
    private JButton initButton;
    private JButton createUserButton;

    private boolean isAdmin;

    public PostMakingFrame() {
        this.isAdmin = DatabaseConnection.isCurrentUserAdmin();

        setTitle("Социальная сеть - " + (isAdmin ? "Администратор" : "Гость"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        initComponents();
        refreshPosts();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Верхняя панель с кнопками.
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // Кнопки для всех пользователей
        refreshButton = createButton("Обновить", "refresh.png");
        refreshButton.addActionListener(e -> refreshPosts());
        toolBar.add(refreshButton);

        toolBar.addSeparator();

        searchButton = createButton("Поиск по автору", "search.png");
        searchButton.addActionListener(e -> searchByAuthor());
        toolBar.add(searchButton);

        likeButton = createButton("Лайк", "like.png");
        likeButton.addActionListener(e -> likeSelectedPost());
        toolBar.add(likeButton);

        // Кнопки только для администратора
        if (isAdmin) {
            toolBar.addSeparator();

            initButton = createButton("Создать БД и таблицу", "database.png");
            initButton.addActionListener(e -> initDatabase());
            toolBar.add(initButton);

            clearButton = createButton("Очистить все посты", "clear.png");
            clearButton.addActionListener(e -> clearAllPosts());
            toolBar.add(clearButton);

            toolBar.addSeparator();

            addButton = createButton("Новый пост", "add.png");
            addButton.addActionListener(e -> showAddDialog());
            toolBar.add(addButton);

            editButton = createButton("Редактировать", "edit.png");
            editButton.addActionListener(e -> showEditDialog());
            toolBar.add(editButton);

            deleteButton = createButton("Удалить по автора", "delete.png");
            deleteButton.addActionListener(e -> deleteByAuthor());
            toolBar.add(deleteButton);

            toolBar.addSeparator();

            createUserButton = createButton("Создать пользователя", "user.png");
            createUserButton.addActionListener(e -> showCreateUserDialog());
            toolBar.add(createUserButton);
        }

        add(toolBar, BorderLayout.NORTH);

        // Таблица с постами.
        String[] columns = {
                "ID", "Автор", "Текст поста",
                "Лайки", "Приватный"
        };  // kolonka "Создано" udalena

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        postsTable = new JTable(tableModel);
        postsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        postsTable.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
        postsTable.getColumnModel().getColumn(1).setPreferredWidth(100);  // Автор
        postsTable.getColumnModel().getColumn(2).setPreferredWidth(500);  // Текст
        postsTable.getColumnModel().getColumn(3).setPreferredWidth(70);   // Лайки
        postsTable.getColumnModel().getColumn(4).setPreferredWidth(80);   // Приватность

        JScrollPane scrollPane = new JScrollPane(postsTable);
        add(scrollPane, BorderLayout.CENTER);

        // Нижняя панель статуса.
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(BorderFactory.createEtchedBorder());

        statusLabel = new JLabel("Готов к работе. Режим: " + (isAdmin ? "Администратор" : "Гость"));
        statusPanel.add(statusLabel);

        add(statusPanel, BorderLayout.SOUTH);
    }

    // Вспомогательный метод для создания кнопок
    private JButton createButton(String text, String iconName) {
        JButton button = new JButton(text);
        button.setToolTipText(text);
        return button;
    }

    // Методы работы с постами.

    // Обновление таблицы.
    private void refreshPosts() {
        setButtonsEnabled(false);
        statusLabel.setText("Загрузка постов...");

        new Thread(() -> {
            try {
                List<Post> posts = SocialnetCalls.getAllPosts();
                SwingUtilities.invokeLater(() -> {
                    updateTable(posts);
                    statusLabel.setText("Загружено постов: " + posts.size() +
                            " | Режим: " + (isAdmin ? "Администратор" : "Гость"));
                    setButtonsEnabled(true);
                });
            } catch (SQLException ex) {
                SwingUtilities.invokeLater(() -> {
                    showError("Ошибка загрузки", ex);
                    statusLabel.setText("Ошибка загрузки данных");
                    setButtonsEnabled(true);
                });
            }
        }).start();
    }

    // Поиск по автору.
    private void searchByAuthor() {
        String author = JOptionPane.showInputDialog(this,
                "Введите никнейм автора для поиска:",
                "Поиск по автору",
                JOptionPane.QUESTION_MESSAGE);

        if (author != null && !author.trim().isEmpty()) {
            setButtonsEnabled(false);
            statusLabel.setText("Поиск...");

            new Thread(() -> {
                try {
                    List<Post> posts = SocialnetCalls.searchPostsByAuthor(author);
                    SwingUtilities.invokeLater(() -> {
                        updateTable(posts);
                        statusLabel.setText("Найдено постов: " + posts.size() +
                                " (поиск: " + author + ")");
                        setButtonsEnabled(true);
                    });
                } catch (SQLException ex) {
                    SwingUtilities.invokeLater(() -> {
                        showError("Ошибка поиска", ex);
                        setButtonsEnabled(true);
                    });
                }
            }).start();
        }
    }

    // Лайк поста.
    private void likeSelectedPost() {
        int selectedRow = postsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Выберите пост для лайка",
                    "Предупреждение",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int postId = (int) tableModel.getValueAt(selectedRow, 0);

        setButtonsEnabled(false);
        statusLabel.setText("Ставим лайк...");

        new Thread(() -> {
            try {
                SocialnetCalls.likePost(postId);
                SwingUtilities.invokeLater(() -> {
                    refreshPosts();
                    statusLabel.setText("Лайк поставлен!");
                });
            } catch (SQLException ex) {
                SwingUtilities.invokeLater(() -> {
                    showError("Не удалось поставить лайк", ex);
                    setButtonsEnabled(true);
                });
            }
        }).start();
    }

    // Инициализация БД (админ).
    private void initDatabase() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Это создаст новую базу данных social_network и таблицу posts.\n" +
                        "Все существующие данные будут потеряны. Продолжить?",
                "Инициализация БД",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            setButtonsEnabled(false);
            statusLabel.setText("Создание базы данных...");

            new Thread(() -> {
                try {
                    SocialnetCalls.createDatabase();
                    SocialnetCalls.createTable();

                    SwingUtilities.invokeLater(() -> {
                        refreshPosts();
                        statusLabel.setText("База данных успешно создана");
                        JOptionPane.showMessageDialog(this,
                                "База данных успешно инициализирована!");
                    });
                } catch (SQLException ex) {
                    SwingUtilities.invokeLater(() -> {
                        showError("Ошибка инициализации", ex);
                        setButtonsEnabled(true);
                    });
                }
            }).start();
        }
    }

    // Очистка всех постов (админ).
    private void clearAllPosts() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Вы уверены, что хотите удалить ВСЕ посты?",
                "Очистка таблицы",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            setButtonsEnabled(false);
            statusLabel.setText("Очистка...");

            new Thread(() -> {
                try {
                    SocialnetCalls.clearTable();
                    SwingUtilities.invokeLater(() -> {
                        refreshPosts();
                        statusLabel.setText("Таблица очищена");
                    });
                } catch (SQLException ex) {
                    SwingUtilities.invokeLater(() -> {
                        showError("Ошибка очистки", ex);
                        setButtonsEnabled(true);
                    });
                }
            }).start();
        }
    }

    // Показать диалог добавления поста (админ).
    private void showAddDialog() {
        PostDialogue dialog = new PostDialogue(this, "Создание нового поста", null);
        dialog.setVisible(true);

        Post newPost = dialog.getPost();
        if (dialog.isConfirmed() && newPost != null) {
            setButtonsEnabled(false);
            statusLabel.setText("Добавление поста...");

            new Thread(() -> {
                try {
                    SocialnetCalls.addPost(
                            newPost.getContentText(),
                            newPost.getAuthor(),
                            newPost.isPrivate()
                    );
                    SwingUtilities.invokeLater(() -> {
                        refreshPosts();
                        statusLabel.setText("Пост добавлен");
                    });
                } catch (SQLException ex) {
                    SwingUtilities.invokeLater(() -> {
                        showError("Ошибка добавления", ex);
                        setButtonsEnabled(true);
                    });
                }
            }).start();
        }
    }

    // Показать диалог редактирования поста (админ).
    private void showEditDialog() {
        int selectedRow = postsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Выберите пост для редактирования.",
                    "Предупреждение",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Извлекаем значения через toString() и парсим в нужный тип
        int id = Integer.parseInt(tableModel.getValueAt(selectedRow, 0).toString());
        String content = tableModel.getValueAt(selectedRow, 2).toString();
        String author = tableModel.getValueAt(selectedRow, 1).toString();
        int likes = Integer.parseInt(tableModel.getValueAt(selectedRow, 3).toString());

        // БЕЗОПАСНОЕ ПРЕВРАЩЕНИЕ В BOOLEAN (вместо (boolean))
        boolean isPrivate = Boolean.parseBoolean(tableModel.getValueAt(selectedRow, 4).toString());

        Post post = new Post(id, content, author, likes, isPrivate);


        PostDialogue dialog = new PostDialogue(this, "Редактировние поста", post);
        dialog.setVisible(true);

        Post updatedPost = dialog.getPost();
        if (dialog.isConfirmed() && updatedPost != null) {
            setButtonsEnabled(false);
            statusLabel.setText("Идёт обновление поста...");

            new Thread(() -> {
                try {
                    SocialnetCalls.updatePost(
                            updatedPost.getId(),
                            updatedPost.getContentText(),
                            updatedPost.isPrivate()
                    );
                    SwingUtilities.invokeLater(() -> {
                        refreshPosts();
                        statusLabel.setText("Пост обновлён.");
                    });
                } catch (SQLException ex) {
                    SwingUtilities.invokeLater(() -> {
                        showError("Ошибка обновления:", ex);
                        setButtonsEnabled(true);
                    });
                }
            }).start();
        }
    }

    // Удаление постов автора (админ).
    private void deleteByAuthor() {
        String author = JOptionPane.showInputDialog(this,
                "Введите никнейм автора, все посты которого нужно удалить:",
                "Удаление постов автора",
                JOptionPane.WARNING_MESSAGE);

        if (author != null && !author.trim().isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Вы уверены, что хотите удалить все посты автора " + author + "?",
                    "Подтверждение удаления",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                setButtonsEnabled(false);
                statusLabel.setText("Удаление постов...");

                new Thread(() -> {
                    try {
                        SocialnetCalls.deletePostsByAuthor(author);
                        SwingUtilities.invokeLater(() -> {
                            refreshPosts();
                            statusLabel.setText("Посты автора " + author + " удалены");
                        });
                    } catch (SQLException ex) {
                        SwingUtilities.invokeLater(() -> {
                            showError("Ошибка удаления", ex);
                            setButtonsEnabled(true);
                        });
                    }
                }).start();
            }
        }
    }

    // Создание нового пользователя (админ).
    private void showCreateUserDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.add(new JLabel("Имя пользователя:"));
        JTextField userField = new JTextField();
        panel.add(userField);

        panel.add(new JLabel("Пароль:"));
        JPasswordField passField = new JPasswordField();
        panel.add(passField);

        panel.add(new JLabel("Права:"));
        JCheckBox adminCheck = new JCheckBox("Администратор");
        panel.add(adminCheck);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Создание нового пользователя БД",
                JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Заполните все поля",
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            setButtonsEnabled(false);
            statusLabel.setText("Создание пользователя...");

            new Thread(() -> {
                try {
                    SocialnetCalls.createUser(username, password, adminCheck.isSelected());
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Пользователь " + username + " создан");
                        JOptionPane.showMessageDialog(this,
                                "Пользователь " + username + " успешно создан!\n" +
                                        "Теперь вы можете войти под этим пользователем.");
                        setButtonsEnabled(true);
                    });
                } catch (SQLException ex) {
                    SwingUtilities.invokeLater(() -> {
                        showError("Ошибка создания пользователя", ex);
                        setButtonsEnabled(true);
                    });
                }
            }).start();
        }
    }

    // Заполнение таблицы данными.
    // Zapolnenie tablicy dannymi.
    private void updateTable(List<Post> posts) {
        tableModel.setRowCount(0);

        for (Post post : posts) {
            tableModel.addRow(new Object[]{
                    post.getId(),
                    post.getAuthor(),
                    post.getContentText(),
                    post.getLikesCounter(),
                    post.isPrivate() ? "Da" : "Net"
            });
        }
    }

    // Включение / выключение кнопок.
    private void setButtonsEnabled(boolean enabled) {
        refreshButton.setEnabled(enabled);
        searchButton.setEnabled(enabled);
        likeButton.setEnabled(enabled);

        if (isAdmin) {
            initButton.setEnabled(enabled);
            clearButton.setEnabled(enabled);
            addButton.setEnabled(enabled);
            editButton.setEnabled(enabled);
            deleteButton.setEnabled(enabled);
            createUserButton.setEnabled(enabled);
        }
    }

    // Отображение ошибки.
    private void showError(String message, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this,
                message + ":\n" + e.getMessage(),
                "ОШИБКА",
                JOptionPane.ERROR_MESSAGE);
    }
}

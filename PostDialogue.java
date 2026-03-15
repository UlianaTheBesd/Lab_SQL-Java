import javax.swing.*;
import java.awt.*;

public class PostDialogue extends JDialog {
    private JTextArea contentArea;
    private JTextField authorField;
    private JCheckBox privateCheck;
    private boolean confirmed = false;
    private Post post;

    public PostDialogue(Frame owner, String title, Post existingPost) {
        super(owner, title, true);

        if (existingPost != null) {
            this.post = existingPost;
        } else {
            this.post = new Post("", "", false);
        }

        setSize(400, 300);
        setLocationRelativeTo(owner);

        initComponents();
        if (existingPost != null) {
            loadPostData();
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Текст поста
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Текст поста:"), gbc);

        contentArea = new JTextArea(5, 20);
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(contentArea);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(scrollPane, gbc);

        // Автор
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(new JLabel("Автор:"), gbc);

        authorField = new JTextField(20);
        gbc.gridx = 1;
        formPanel.add(authorField, gbc);

        // Приватность
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Приватность:"), gbc);

        privateCheck = new JCheckBox("Приватный пост");
        gbc.gridx = 1;
        formPanel.add(privateCheck, gbc);

        add(formPanel, BorderLayout.CENTER);

        // Кнопки
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton("Сохранить");
        okButton.addActionListener(e -> {
            if (savePost()) {
                confirmed = true;
                dispose();
            }
        });

        JButton cancelButton = new JButton("Отмена");
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadPostData() {
        contentArea.setText(post.getContentText());
        authorField.setText(post.getAuthor());
        authorField.setEnabled(post.getId() == 0); // Можно менять автора только для нового поста
        privateCheck.setSelected(post.isPrivate());
    }

    private boolean savePost() {
        String content = contentArea.getText().trim();
        if (content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Текст поста не может быть пустым",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String author = authorField.getText().trim();
        if (author.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Укажите автора",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        post.setContentText(content);
        post.setAuthor(author);
        post.setPrivate(privateCheck.isSelected());

        return true;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Post getPost() {
        return post;
    }
}

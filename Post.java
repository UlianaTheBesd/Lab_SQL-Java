import java.sql.Timestamp;

public class Post {
    private int id;
    private String contentText;
    private String author;
    private int likesCounter;
    private boolean isPrivate;

    // Конструктор для нового поста.
    public Post(String contentText, String author, boolean isPrivate) {
        this.contentText = contentText;
        this.author = author;
        this.isPrivate = isPrivate;
        this.likesCounter = 0;
    }

    // Конструктор для поста из БД (БЕЗ timeCreated)
    public Post(int id, String contentText, String author, int likesCounter, boolean isPrivate) {
        this.id = id;
        this.contentText = contentText;
        this.author = author;
        this.likesCounter = likesCounter;
        this.isPrivate = isPrivate;
    }

    // Геттеры и сеттеры.
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public int getLikesCounter() { return likesCounter; }
    public void setLikesCounter(int likesCounter) { this.likesCounter = likesCounter; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }

    @Override
    public String toString() {
        String preview = contentText.length() > 30 ?
                contentText.substring(0, 30) + "..." : contentText;
        return author + ": " + preview + " (" + likesCounter + ")";
    }
}

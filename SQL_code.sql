-- 1. Создание базы данных (однотабличная).
CREATE OR REPLACE PROCEDURE create_db_socialnet()
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM * FROM pg_database WHERE datname = 'social_network';
    IF NOT FOUND THEN
        CREATE DATABASE social_network;
        RAISE NOTICE 'БД social_network создана.';
    ELSE
        RAISE NOTICE 'БД social_network уже существует.';
    END IF;
END;
$$;

-- 2. Удаление базы данных.
CREATE OR REPLACE PROCEDURE drop_db_socialnet()
LANGUAGE plpgsql
AS $$
BEGIN
    PERFORM pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'social_network';
    DROP DATABASE IF EXISTS social_network;
    RAISE NOTICE 'БД social_network удалена.';
END;
$$;

-- 3. Создание таблицы.
CREATE OR REPLACE PROCEDURE create_posts_table()
LANGUAGE plpgsql
AS $$
BEGIN
    CREATE TABLE IF NOT EXISTS Posts (
        id SERIAL PRIMARY KEY,
        content_text TEXT NOT NULL CHECK (length(content_text) > 0),
        author VARCHAR(50) NOT NULL,
        likes_counter INTEGER DEFAULT 0 CHECK (likes_counter >= 0),
        is_private BOOLEAN DEFAULT FALSE
    );
    
    CREATE INDEX IF NOT EXISTS idx_author ON Posts(author);
    RAISE NOTICE 'Таблица Posts создана.';
END;
$$;

-- 4. Очистка таблицы.
CREATE OR REPLACE PROCEDURE clear_table_socialpost()
LANGUAGE plpgsql
AS $$
BEGIN
    TRUNCATE TABLE Posts RESTART IDENTITY;
    RAISE NOTICE 'Таблица очищена.';
END;
$$;

-- 5. Добавление новых данных.
CREATE OR REPLACE PROCEDURE add_post(
    temp_content TEXT,
    temp_author VARCHAR,
    temp_is_private BOOLEAN DEFAULT FALSE
)
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO Posts (content_text, author, is_private)
    VALUES (temp_content, temp_author, temp_is_private);

    RAISE NOTICE 'Пост добавлен пользователем: %.', temp_author;
END;
$$;

-- 6. Поиск по автору.
CREATE OR REPLACE FUNCTION search_post_by_author(temp_author VARCHAR)
RETURNS TABLE(
    id INTEGER,
    content_text TEXT,
    author VARCHAR,
    likes_counter INTEGER,
    is_private BOOLEAN
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT Posts.id, Posts.content_text, Posts.author, Posts.likes_counter, Posts.is_private
    FROM Posts
    WHERE Posts.author ILIKE '%' || temp_author || '%'
    ORDER BY Posts.id DESC;

    RAISE NOTICE 'Выполнен поиск по автору: %', temp_author;
END;
$$;

-- 7. Просмотр всех постов.
CREATE OR REPLACE FUNCTION see_all_posts()
RETURNS TABLE(
    id INTEGER,
    content_text TEXT,
    author VARCHAR,
    likes_counter INTEGER,
    is_private BOOLEAN
)
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN QUERY
    SELECT Posts.id, Posts.content_text, Posts.author, Posts.likes_counter, Posts.is_private
    FROM Posts
    ORDER BY Posts.id DESC;
END;
$$;

-- 8. Обновление поста.
CREATE OR REPLACE PROCEDURE update_post(
    temp_id INTEGER,
    temp_content_text TEXT,
    temp_is_private BOOLEAN
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE Posts 
    SET content_text = temp_content_text, is_private = temp_is_private 
    WHERE id = temp_id;

    RAISE NOTICE 'Пост с ID = % обновлен.', temp_id;
END;
$$;

-- 9. Изменение количества лайков (+1).
CREATE OR REPLACE PROCEDURE change_likes_amount(temp_post_id INTEGER)
LANGUAGE plpgsql
SECURITY DEFINER -- (добавление SECURITY DEFINER, чтобы guest мог менять счетчик лайков, испольуя "права" Администратора).
AS $$
BEGIN
    UPDATE Posts SET likes_counter = likes_counter + 1 WHERE id = temp_post_id;
    RAISE NOTICE '+1 Лайк на пост %.', temp_post_id;
END;
$$;

-- 10. Удаление по автору.
CREATE OR REPLACE PROCEDURE delete_post_by_author(temp_author VARCHAR)
LANGUAGE plpgsql
AS $$
DECLARE
    deleted_counter INTEGER;
BEGIN
    WITH temptable_deleted AS (
        DELETE FROM Posts WHERE author = temp_author RETURNING *
    )
    SELECT COUNT(*) INTO deleted_counter FROM temptable_deleted;

    RAISE NOTICE 'Удалено % постов автора: %.', deleted_counter, temp_author;
END;
$$;

-- 11. Создание пользователя.
CREATE OR REPLACE PROCEDURE create_user_socialnet(
    temp_username VARCHAR,
    temp_password VARCHAR,
    temp_is_admin BOOLEAN
)
LANGUAGE plpgsql
AS $$
DECLARE
    existence_marker INTEGER;
    current_db_name TEXT; -- Переменная для хранения имени базы
BEGIN
    -- Получаем имя текущей базы данных динамически
    current_db_name := current_database();

    SELECT COUNT(*) INTO existence_marker FROM pg_roles WHERE rolname = temp_username;
    
    IF existence_marker > 0 THEN
        RAISE EXCEPTION 'Пользователь с ником % уже существует.', temp_username;
    END IF;
    
    EXECUTE format('CREATE USER %I WITH PASSWORD %L', temp_username, temp_password);
    
    -- ИСПРАВЛЕНО: Теперь используем динамическое имя базы вместо social_network
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO %I', current_db_name, temp_username);
    
    EXECUTE format('GRANT USAGE ON SCHEMA public TO %I', temp_username);

    IF temp_is_admin THEN
        EXECUTE format('GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO %I', temp_username);
        EXECUTE format('GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO %I', temp_username);
        EXECUTE format('GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO %I', temp_username);
        EXECUTE format('GRANT EXECUTE ON ALL PROCEDURES IN SCHEMA public TO %I', temp_username);
        RAISE NOTICE 'Создан пользователь-администратор: %.', temp_username;
    ELSE
        EXECUTE format('GRANT SELECT ON ALL TABLES IN SCHEMA public TO %I', temp_username);
        RAISE NOTICE 'Создан пользователь-гость: %.', temp_username;
    END IF;
END;
$$;

-- 12. Удаление пользователя.
CREATE OR REPLACE PROCEDURE drop_user_socialnet(temp_username VARCHAR)
LANGUAGE plpgsql
AS $$
BEGIN
    EXECUTE format('REVOKE ALL PRIVILEGES ON DATABASE social_network FROM %I', temp_username);
    EXECUTE format('REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM %I', temp_username);
    EXECUTE format('REVOKE ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public FROM %I', temp_username);
    EXECUTE format('REVOKE ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public FROM %I', temp_username);

    EXECUTE format('DROP USER IF EXISTS %I', temp_username);
    RAISE NOTICE 'Пользователь % удален', temp_username;
END;
$$;

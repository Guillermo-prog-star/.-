ALTER TABLE chat_messages
ADD COLUMN created_at DATETIME NULL;

CREATE INDEX idx_chat_messages_created_at
    ON chat_messages (created_at);
-- Alexa Account Linking: authorization codes (one-time use, 5 min TTL)
CREATE TABLE IF NOT EXISTS alexa_oauth_codes (
    code             VARCHAR(128) PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    family_id        BIGINT       NOT NULL,
    redirect_uri     TEXT         NOT NULL,
    code_challenge   VARCHAR(256) NOT NULL,
    expires_at       BIGINT       NOT NULL,
    used             TINYINT(1)   NOT NULL DEFAULT 0,
    created_at       BIGINT       NOT NULL
);

-- Alexa Account Linking: access + refresh tokens
CREATE TABLE IF NOT EXISTS alexa_oauth_tokens (
    access_token     VARCHAR(128) PRIMARY KEY,
    refresh_token    VARCHAR(128) NOT NULL UNIQUE,
    user_id          BIGINT       NOT NULL,
    family_id        BIGINT       NOT NULL,
    expires_at       BIGINT       NOT NULL,
    revoked          TINYINT(1)   NOT NULL DEFAULT 0,
    created_at       BIGINT       NOT NULL
);

CREATE INDEX idx_alexa_tokens_refresh ON alexa_oauth_tokens(refresh_token);
CREATE INDEX idx_alexa_tokens_family  ON alexa_oauth_tokens(family_id);

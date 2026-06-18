CREATE TABLE IF NOT EXISTS alexa_oauth_codes (
    code        VARCHAR(128) NOT NULL,
    user_id     BIGINT       NOT NULL,
    family_id   BIGINT       NOT NULL,
    redirect_uri TEXT        NOT NULL,
    code_challenge VARCHAR(256) NOT NULL,
    expires_at  BIGINT       NOT NULL,
    used        TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  BIGINT       NOT NULL,
    PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS alexa_oauth_tokens (
    access_token  VARCHAR(128) NOT NULL,
    refresh_token VARCHAR(128) NOT NULL,
    user_id       BIGINT       NOT NULL,
    family_id     BIGINT       NOT NULL,
    expires_at    BIGINT       NOT NULL,
    revoked       TINYINT(1)   NOT NULL DEFAULT 0,
    created_at    BIGINT       NOT NULL,
    PRIMARY KEY (access_token),
    UNIQUE KEY uq_alexa_refresh_token (refresh_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

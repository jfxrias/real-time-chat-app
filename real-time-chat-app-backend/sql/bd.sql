-- Criação do Banco de Dados
CREATE DATABASE IF NOT EXISTS uaizap_db;
USE uaizap_db;

-- 1. Entidade: user

CREATE TABLE user (
                      user_uuid VARCHAR(36) PRIMARY KEY,
                      cpf VARCHAR(11) NOT NULL,
                      name VARCHAR(100) NOT NULL,
                      password_hash VARCHAR(255) NOT NULL,
                      is_active BOOLEAN DEFAULT TRUE,
                      is_admin BOOLEAN DEFAULT FALSE,
                      created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- índice para consulta rápida por cpf
CREATE INDEX idx_user_cpf ON user(cpf);

-- 2. Entidade Associativa: friendship (Relacionamento: has)
CREATE TABLE friendship (
    first_user_uuid VARCHAR(36) NOT NULL,
    second_user_uuid VARCHAR(36) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (first_user_uuid, second_user_uuid),
    FOREIGN KEY (first_user_uuid) REFERENCES user(user_uuid) ON DELETE CASCADE,
    FOREIGN KEY (second_user_uuid) REFERENCES user(user_uuid) ON DELETE CASCADE
);

-- 3. Entidade: group (Relacionamento: manages)
-- Usamos crases porque 'group' é uma palavra reservada no MySQL
CREATE TABLE `group` (
    group_uuid VARCHAR(36) PRIMARY KEY,
    group_name VARCHAR(100) NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    creator_uuid VARCHAR(36) NOT NULL,
    FOREIGN KEY (creator_uuid) REFERENCES user(user_uuid)
);

-- 4. Entidade Associativa: group_member (Relacionamento: links_user / contains_member)
CREATE TABLE group_member (
    group_uuid VARCHAR(36) NOT NULL,
    user_uuid VARCHAR(36) NOT NULL,
    role ENUM('MEMBER', 'ADMIN') DEFAULT 'MEMBER',
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_uuid, user_uuid),
    FOREIGN KEY (group_uuid) REFERENCES `group`(group_uuid) ON DELETE CASCADE,
    FOREIGN KEY (user_uuid) REFERENCES user(user_uuid) ON DELETE CASCADE
);

-- 5. Entidade: report (Relacionamento: reports)
CREATE TABLE report (
    report_uuid VARCHAR(36) PRIMARY KEY,
    reporter_uuid VARCHAR(36) NOT NULL,
    reported_uuid VARCHAR(36) NOT NULL,
    group_uuid VARCHAR(36) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    reported_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reporter_uuid) REFERENCES user(user_uuid) ON DELETE CASCADE,
    FOREIGN KEY (reported_uuid) REFERENCES user(user_uuid) ON DELETE CASCADE,
    FOREIGN KEY (group_uuid) REFERENCES `group`(group_uuid) ON DELETE CASCADE
);

-- 6. Entidade Principal: message (Relacionamento: sends)
CREATE TABLE message (
    message_uuid VARCHAR(36) PRIMARY KEY,
    sender_uuid VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    edited_at DATETIME NULL,
    is_deleted_by_sender BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (sender_uuid) REFERENCES user(user_uuid) ON DELETE CASCADE
);

-- 7. Entidade Especializada: private_message (Relacionamentos: refers_to / received_by)
CREATE TABLE private_message (
    message_uuid VARCHAR(36) PRIMARY KEY,
    receiver_uuid VARCHAR(36) NOT NULL,
    is_deleted_by_receiver BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (message_uuid) REFERENCES message(message_uuid) ON DELETE CASCADE,
    FOREIGN KEY (receiver_uuid) REFERENCES user(user_uuid) ON DELETE CASCADE
);

-- 8. Entidade Especializada: group_message (Relacionamentos: refers_to2 / sent_to)
CREATE TABLE group_message (
    message_uuid VARCHAR(36) PRIMARY KEY,
    target_group_uuid VARCHAR(36) NOT NULL,
    FOREIGN KEY (message_uuid) REFERENCES message(message_uuid) ON DELETE CASCADE,
    FOREIGN KEY (target_group_uuid) REFERENCES `group`(group_uuid) ON DELETE CASCADE
);
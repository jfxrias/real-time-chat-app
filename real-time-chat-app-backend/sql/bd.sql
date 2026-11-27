CREATE DATABASE IF NOT EXISTS uaizap_db;

USE uaizap_db;

-- usuarios do sistema
CREATE TABLE
    user (
        user_uuid VARCHAR(36) PRIMARY KEY,
        cpf VARCHAR(11) NOT NULL,
        name VARCHAR(100) NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        is_active BOOLEAN DEFAULT TRUE,
        is_admin BOOLEAN DEFAULT FALSE,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

-- busca no login e sempre pelo cpf
CREATE INDEX idx_user_cpf ON user (cpf);

-- amizades (fica uma linha so por par de usuarios)
CREATE TABLE
    friendship (
        first_user_uuid VARCHAR(36) NOT NULL,
        second_user_uuid VARCHAR(36) NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (first_user_uuid, second_user_uuid),
        FOREIGN KEY (first_user_uuid) REFERENCES user (user_uuid) ON DELETE CASCADE,
        FOREIGN KEY (second_user_uuid) REFERENCES user (user_uuid) ON DELETE CASCADE
    );

-- grupos (crase obrigatoria pq group e palavra reservada no mysql)
CREATE TABLE
    `group` (
        group_uuid VARCHAR(36) PRIMARY KEY,
        group_name VARCHAR(100) NOT NULL,
        status ENUM ('ACTIVE', 'INACTIVE') DEFAULT 'ACTIVE',
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        creator_uuid VARCHAR(36) NOT NULL,
        FOREIGN KEY (creator_uuid) REFERENCES user (user_uuid)
    );

-- membros de cada grupo e o papel deles la dentro
CREATE TABLE
    group_member (
        group_uuid VARCHAR(36) NOT NULL,
        user_uuid VARCHAR(36) NOT NULL,
        role ENUM ('MEMBER', 'ADMIN') DEFAULT 'MEMBER',
        joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (group_uuid, user_uuid),
        FOREIGN KEY (group_uuid) REFERENCES `group` (group_uuid) ON DELETE CASCADE,
        FOREIGN KEY (user_uuid) REFERENCES user (user_uuid) ON DELETE CASCADE
    );

-- denuncias feitas por usuarios dentro de grupos
CREATE TABLE
    report (
        report_uuid VARCHAR(36) PRIMARY KEY,
        reporter_uuid VARCHAR(36) NOT NULL,
        reported_uuid VARCHAR(36) NOT NULL,
        group_uuid VARCHAR(36) NOT NULL,
        reason VARCHAR(255) NOT NULL,
        reported_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (reporter_uuid) REFERENCES user (user_uuid) ON DELETE CASCADE,
        FOREIGN KEY (reported_uuid) REFERENCES user (user_uuid) ON DELETE CASCADE,
        FOREIGN KEY (group_uuid) REFERENCES `group` (group_uuid) ON DELETE CASCADE
    );

-- tabela base de mensagens (privadas e de grupo herdam daqui)
CREATE TABLE
    message (
        message_uuid VARCHAR(36) PRIMARY KEY,
        sender_uuid VARCHAR(36) NOT NULL,
        content TEXT NOT NULL,
        sent_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        edited_at DATETIME NULL,
        is_deleted_by_sender BOOLEAN DEFAULT FALSE,
        FOREIGN KEY (sender_uuid) REFERENCES user (user_uuid) ON DELETE CASCADE
    );

-- mensagens privadas
CREATE TABLE
    private_message (
        message_uuid VARCHAR(36) PRIMARY KEY,
        receiver_uuid VARCHAR(36) NOT NULL,
        is_deleted_by_receiver BOOLEAN DEFAULT FALSE,
        FOREIGN KEY (message_uuid) REFERENCES message (message_uuid) ON DELETE CASCADE,
        FOREIGN KEY (receiver_uuid) REFERENCES user (user_uuid) ON DELETE CASCADE
    );

-- mensagens enviadas em grupo
CREATE TABLE
    group_message (
        message_uuid VARCHAR(36) PRIMARY KEY,
        target_group_uuid VARCHAR(36) NOT NULL,
        FOREIGN KEY (message_uuid) REFERENCES message (message_uuid) ON DELETE CASCADE,
        FOREIGN KEY (target_group_uuid) REFERENCES `group` (group_uuid) ON DELETE CASCADE
    );
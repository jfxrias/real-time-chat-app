USE uaizap_db;

-- 1. porcentagem de mensagens recebidas por usuario
-- pega o total geral antes pra nao ficar repetindo conta no select
SET @total_recebidas = (
    (SELECT COUNT(*) FROM private_message) +
    (
        SELECT COUNT(*)
        FROM group_member gmbr
        JOIN group_message gm ON gm.target_group_uuid = gmbr.group_uuid
        JOIN message m ON m.message_uuid = gm.message_uuid
        WHERE m.sender_uuid != gmbr.user_uuid
    )
);

SELECT
    u.user_uuid AS userUuid,
    u.name,
    (IFNULL(pm.qtd, 0) + IFNULL(gm.qtd, 0)) AS receivedCount,
    CASE
        WHEN @total_recebidas = 0 THEN 0
        ELSE ROUND(((IFNULL(pm.qtd, 0) + IFNULL(gm.qtd, 0)) * 100.0) / @total_recebidas, 2)
        END AS percentage
FROM `user` u
         LEFT JOIN (
    SELECT receiver_uuid, COUNT(*) AS qtd
    FROM private_message
    GROUP BY receiver_uuid
) pm ON pm.receiver_uuid = u.user_uuid
         LEFT JOIN (
    -- em grupo nao conta as msgs que o proprio usuario mandou
    SELECT gmbr.user_uuid, COUNT(*) AS qtd
    FROM group_member gmbr
             JOIN group_message gm ON gm.target_group_uuid = gmbr.group_uuid
             JOIN message m ON m.message_uuid = gm.message_uuid
    WHERE m.sender_uuid != gmbr.user_uuid
    GROUP BY gmbr.user_uuid
) gm ON gm.user_uuid = u.user_uuid
ORDER BY percentage DESC, u.name ASC;


-- 2. usuarios com mais amigos
-- checa as duas colunas pq a amizade fica salva numa linha so
SELECT
    u.user_uuid AS userUuid,
    u.name,
    COUNT(f.first_user_uuid) AS totalFriends
FROM `user` u
         LEFT JOIN friendship f
                   ON f.first_user_uuid = u.user_uuid
                       OR f.second_user_uuid = u.user_uuid
GROUP BY u.user_uuid, u.name
ORDER BY totalFriends DESC, u.name ASC;


-- 3. quantidade de usuarios por grupo
SELECT
    g.group_uuid AS groupUuid,
    g.group_name AS groupName,
    COUNT(gm.user_uuid) AS totalUsers
FROM `group` g
         LEFT JOIN group_member gm ON gm.group_uuid = g.group_uuid
GROUP BY g.group_uuid, g.group_name
ORDER BY totalUsers DESC, groupName ASC;


-- 4. usuarios com mais de 3 denuncias
SELECT
    u.user_uuid AS userUuid,
    u.name,
    COUNT(r.report_uuid) AS totalReports
FROM `user` u
         JOIN report r ON r.reported_uuid = u.user_uuid
GROUP BY u.user_uuid, u.name
HAVING COUNT(r.report_uuid) > 3
ORDER BY totalReports DESC, u.name ASC;


-- 5. motivos mais comuns de denuncia
SELECT
    reason,
    COUNT(*) AS totalReports
FROM report
GROUP BY reason
ORDER BY totalReports DESC, reason ASC;


-- 6. usuarios em mais grupos
SELECT
    u.user_uuid AS userUuid,
    u.name,
    COUNT(gm.group_uuid) AS totalGroups
FROM `user` u
         LEFT JOIN group_member gm ON gm.user_uuid = u.user_uuid
GROUP BY u.user_uuid, u.name
ORDER BY totalGroups DESC, u.name ASC;


-- 7. usuario ha mais tempo sem mandar mensagem
-- se nunca mandou nada, usa a data de criacao da conta
SELECT
    u.user_uuid AS userUuid,
    u.name,
    IFNULL(MAX(m.sent_at), u.created_at) AS lastMessageAt,
    u.created_at AS createdAt
FROM `user` u
         LEFT JOIN message m ON m.sender_uuid = u.user_uuid
GROUP BY u.user_uuid, u.name, u.created_at
ORDER BY lastMessageAt ASC, u.name ASC
    LIMIT 1;


-- 8. mensagens de um usuario num periodo (mudar o uuid pra testar)
SET @user_uuid  = '00000000-0000-0000-0000-000000000000';
SET @start_date = '2026-01-01 00:00:00';
SET @end_date   = '2026-12-31 23:59:59';

SELECT
    userUuid,
    name,
    sentCount,
    receivedCount,
    (sentCount + receivedCount) AS totalCount
FROM (
         SELECT
             u.user_uuid AS userUuid,
             u.name,
             (
                 SELECT COUNT(*)
                 FROM message m
                 WHERE m.sender_uuid = u.user_uuid
                   AND m.sent_at BETWEEN @start_date AND @end_date
             ) AS sentCount,
             (
                 SELECT COUNT(*)
                 FROM private_message pm
                          JOIN message m ON m.message_uuid = pm.message_uuid
                 WHERE pm.receiver_uuid = u.user_uuid
                   AND m.sent_at BETWEEN @start_date AND @end_date
             ) + (
                 SELECT COUNT(*)
                 FROM group_member gmbr
                          JOIN group_message gm ON gm.target_group_uuid = gmbr.group_uuid
                          JOIN message m ON m.message_uuid = gm.message_uuid
                 WHERE gmbr.user_uuid = u.user_uuid
                   AND m.sender_uuid != u.user_uuid
             AND m.sent_at BETWEEN @start_date AND @end_date
     ) AS receivedCount
    FROM `user` u
WHERE u.user_uuid = @user_uuid
    ) AS resumo;
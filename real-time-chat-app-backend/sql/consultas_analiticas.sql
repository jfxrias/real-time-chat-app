USE uaizap_db;

-- 1) Percentual de mensagens recebidas por usuário
SELECT
    u.user_uuid AS userUuid,
    u.name AS name,
    (
        COALESCE(pm_counts.private_received_count, 0) + COALESCE(gm_counts.group_received_count, 0)
    ) AS receivedCount,
    CASE
        WHEN totals.total_received = 0 THEN 0
        ELSE ROUND(
            ((COALESCE(pm_counts.private_received_count, 0) + COALESCE(gm_counts.group_received_count, 0)) * 100.0)
            / totals.total_received,
            2
        )
    END AS percentage
FROM `user` u
LEFT JOIN (
    SELECT
        pm.receiver_uuid AS user_uuid,
        COUNT(*) AS private_received_count
    FROM private_message pm
    GROUP BY pm.receiver_uuid
) pm_counts ON pm_counts.user_uuid = u.user_uuid
LEFT JOIN (
    SELECT
        gmbr.user_uuid,
        COUNT(*) AS group_received_count
    FROM group_member gmbr
    JOIN group_message gm ON gm.target_group_uuid = gmbr.group_uuid
    JOIN message m ON m.message_uuid = gm.message_uuid
    WHERE m.sender_uuid <> gmbr.user_uuid
    GROUP BY gmbr.user_uuid
) gm_counts ON gm_counts.user_uuid = u.user_uuid
CROSS JOIN (
    SELECT
        COALESCE((SELECT COUNT(*) FROM private_message), 0)
        + COALESCE((
            SELECT COUNT(*)
            FROM group_member gmbr
            JOIN group_message gm ON gm.target_group_uuid = gmbr.group_uuid
            JOIN message m ON m.message_uuid = gm.message_uuid
            WHERE m.sender_uuid <> gmbr.user_uuid
        ), 0) AS total_received
) totals
ORDER BY percentage DESC, name ASC;

-- 2) Usuários com mais amigos
SELECT
    u.user_uuid AS userUuid,
    u.name AS name,
    COALESCE(f.total_friends, 0) AS totalFriends
FROM `user` u
LEFT JOIN (
    SELECT
        f_union.user_uuid,
        COUNT(*) AS total_friends
    FROM (
        SELECT first_user_uuid AS user_uuid FROM friendship
        UNION ALL
        SELECT second_user_uuid AS user_uuid FROM friendship
    ) f_union
    GROUP BY f_union.user_uuid
) f ON f.user_uuid = u.user_uuid
ORDER BY totalFriends DESC, name ASC;

-- 3) Quantidade de usuários por grupo
SELECT
    g.group_uuid AS groupUuid,
    g.group_name AS groupName,
    COUNT(gm.user_uuid) AS totalUsers
FROM `group` g
LEFT JOIN group_member gm ON gm.group_uuid = g.group_uuid
GROUP BY g.group_uuid, g.group_name
ORDER BY totalUsers DESC, groupName ASC;

-- 4) Usuários com mais de 3 denúncias
SELECT
    u.user_uuid AS userUuid,
    u.name AS name,
    COUNT(r.report_uuid) AS totalReports
FROM `user` u
JOIN report r ON r.reported_uuid = u.user_uuid
GROUP BY u.user_uuid, u.name
HAVING COUNT(r.report_uuid) > 3
ORDER BY totalReports DESC, name ASC;

-- 5) Motivos mais comuns de denúncia
SELECT
    r.reason AS reason,
    COUNT(r.report_uuid) AS totalReports
FROM report r
GROUP BY r.reason
ORDER BY totalReports DESC, reason ASC;

-- 6) Usuários em mais grupos
SELECT
    u.user_uuid AS userUuid,
    u.name AS name,
    COUNT(gm.group_uuid) AS totalGroups
FROM `user` u
LEFT JOIN group_member gm ON gm.user_uuid = u.user_uuid
GROUP BY u.user_uuid, u.name
ORDER BY totalGroups DESC, name ASC;

-- 7) Usuário há mais tempo sem enviar mensagens
SELECT
    u.user_uuid AS userUuid,
    u.name AS name,
    COALESCE(MAX(m.sent_at), u.created_at) AS lastMessageAt,
    u.created_at AS createdAt
FROM `user` u
LEFT JOIN message m ON m.sender_uuid = u.user_uuid
GROUP BY u.user_uuid, u.name, u.created_at
ORDER BY COALESCE(MAX(m.sent_at), u.created_at) ASC, u.name ASC
LIMIT 1;

-- 8) Estatísticas de mensagens por usuário em período
SET @user_uuid = '00000000-0000-0000-0000-000000000000';
SET @start_date = '2026-01-01 00:00:00';
SET @end_date = '2026-12-31 23:59:59';

SELECT
    u.user_uuid AS userUuid,
    u.name AS name,
    (
        SELECT COUNT(*)
        FROM message m
        WHERE m.sender_uuid = u.user_uuid
          AND m.sent_at BETWEEN @start_date AND @end_date
    ) AS sentCount,
    (
        COALESCE((
            SELECT COUNT(*)
            FROM private_message pm
            JOIN message m ON m.message_uuid = pm.message_uuid
            WHERE pm.receiver_uuid = u.user_uuid
              AND m.sent_at BETWEEN @start_date AND @end_date
        ), 0)
        + COALESCE((
            SELECT COUNT(*)
            FROM group_member gmbr
            JOIN group_message gm ON gm.target_group_uuid = gmbr.group_uuid
            JOIN message m ON m.message_uuid = gm.message_uuid
            WHERE gmbr.user_uuid = u.user_uuid
              AND m.sender_uuid <> u.user_uuid
              AND m.sent_at BETWEEN @start_date AND @end_date
        ), 0)
    ) AS receivedCount,
    (
        (
            SELECT COUNT(*)
            FROM message m
            WHERE m.sender_uuid = u.user_uuid
              AND m.sent_at BETWEEN @start_date AND @end_date
        )
        +
        (
            COALESCE((
                SELECT COUNT(*)
                FROM private_message pm
                JOIN message m ON m.message_uuid = pm.message_uuid
                WHERE pm.receiver_uuid = u.user_uuid
                  AND m.sent_at BETWEEN @start_date AND @end_date
            ), 0)
            + COALESCE((
                SELECT COUNT(*)
                FROM group_member gmbr
                JOIN group_message gm ON gm.target_group_uuid = gmbr.group_uuid
                JOIN message m ON m.message_uuid = gm.message_uuid
                WHERE gmbr.user_uuid = u.user_uuid
                  AND m.sender_uuid <> u.user_uuid
                  AND m.sent_at BETWEEN @start_date AND @end_date
            ), 0)
        )
    ) AS totalCount
FROM `user` u
WHERE u.user_uuid = @user_uuid;

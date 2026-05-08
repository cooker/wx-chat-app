INSERT OR IGNORE INTO images (id, title, image_url, author_name, created_at)
VALUES
    (1, '春日街拍', 'https://example.com/image-1.jpg', 'Grant', 1715000001),
    (2, '城市夜景', 'https://example.com/image-2.jpg', 'Grant', 1715000002);

INSERT OR IGNORE INTO access_events (id, album_id, visitor_id, ip, device, device_version, viewed_at)
VALUES
    (1, 1, 'user-a', '10.0.0.1', 'iPhone', 'iOS 17.0', strftime('%s', 'now', '-1 hour')),
    (2, 1, 'user-b', '10.0.0.2', 'Android', 'Android 14', strftime('%s', 'now', '-30 minutes')),
    (3, 1, 'user-a', '10.0.0.1', 'iPhone', 'iOS 17.0', strftime('%s', 'now', '-20 minutes')),
    (4, 2, 'user-c', '10.0.0.3', 'Mac', 'macOS 14.2', strftime('%s', 'now', '-10 minutes')),
    (5, 2, 'user-d', '10.0.0.4', 'Windows', 'Windows 10.0', strftime('%s', 'now', '-5 minutes')),
    (6, 2, 'user-d', '10.0.0.4', 'Windows', 'Windows 10.0', strftime('%s', 'now', '-1 minutes')),
    (7, 3, 'user-e', '10.0.0.5', 'Android', 'Android 13', strftime('%s', 'now', '-1 day'));

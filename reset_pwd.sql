UPDATE users SET password_hash='$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi' WHERE email='william@integrity.family';
SELECT email, LEFT(password_hash,10) as hash_preview FROM users WHERE email='william@integrity.family';

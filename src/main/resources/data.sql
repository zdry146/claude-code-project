-- Initial test data for Post API
-- This will only run once when schema is created

INSERT INTO posts (title, content, author_name, cover_image, view_count, like_count, is_published, is_deleted, created_at, updated_at) VALUES
('Welcome Post', 'This is a sample welcome post for the API testing.', 'Admin', NULL, 100, 10, true, false, CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP - INTERVAL '60 days'),
('Draft Post 1', 'This is an old unpublished draft that should be cleaned up.', 'User1', NULL, 0, 0, false, false, CURRENT_TIMESTAMP - INTERVAL '45 days', CURRENT_TIMESTAMP - INTERVAL '45 days'),
('Draft Post 2', 'Another old unpublished draft for cleanup testing.', 'User2', NULL, 0, 0, false, false, CURRENT_TIMESTAMP - INTERVAL '35 days', CURRENT_TIMESTAMP - INTERVAL '35 days'),
('Recent Draft', 'A recent draft that should NOT be cleaned up.', 'User3', NULL, 5, 0, false, false, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),
('Published Article', 'This is a published article that will remain.', 'Author A', NULL, 50, 5, true, false, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days');

-- One current path per learner and skill. Archived/completed history remains unlimited.
CREATE UNIQUE INDEX uq_learning_paths_active_user_skill
ON learning_paths(user_id, skill)
WHERE status = 'ACTIVE';

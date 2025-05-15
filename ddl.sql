CREATE TABLE deploy_tasks (
    task_id VARCHAR(36) PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    git_repo VARCHAR(255),
    branch VARCHAR(50),
    logs TEXT,
    start_time TIMESTAMP,
    end_time TIMESTAMP
);


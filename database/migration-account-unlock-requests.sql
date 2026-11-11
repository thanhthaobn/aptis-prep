/* Migration for Account Unlock Requests and User Removal Reason */
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF COL_LENGTH('dbo.users', 'removal_reason') IS NULL
BEGIN
    ALTER TABLE dbo.users
        ADD removal_reason nvarchar(1000) NULL;
END;

IF OBJECT_ID('dbo.account_unlock_requests', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.account_unlock_requests (
        id int IDENTITY(1,1) NOT NULL PRIMARY KEY,
        user_id int NOT NULL,
        message nvarchar(1000) NOT NULL,
        status varchar(30) NOT NULL,
        admin_note nvarchar(1000) NULL,
        handled_by int NULL,
        created_at datetime2 NOT NULL,
        handled_at datetime2 NULL,
        CONSTRAINT fk_account_unlock_requests_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
        CONSTRAINT fk_account_unlock_requests_handled_by FOREIGN KEY (handled_by) REFERENCES dbo.users(id)
    );
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'uq_account_unlock_requests_pending_user' 
      AND object_id = OBJECT_ID('dbo.account_unlock_requests')
)
BEGIN
    CREATE UNIQUE INDEX uq_account_unlock_requests_pending_user 
    ON dbo.account_unlock_requests(user_id) 
    WHERE status = 'PENDING';
END;

COMMIT TRANSACTION;


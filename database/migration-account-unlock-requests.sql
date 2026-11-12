/* Adds the account unlock workflow without changing existing user data. */
IF OBJECT_ID('dbo.account_unlock_requests', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.account_unlock_requests (
        id INT IDENTITY(1,1) NOT NULL CONSTRAINT pk_account_unlock_requests PRIMARY KEY,
        user_id INT NOT NULL,
        message NVARCHAR(1000) NOT NULL,
        status NVARCHAR(20) NOT NULL CONSTRAINT df_account_unlock_requests_status DEFAULT ('PENDING'),
        admin_note NVARCHAR(1000) NULL,
        handled_by INT NULL,
        created_at DATETIME2(7) NOT NULL CONSTRAINT df_account_unlock_requests_created_at DEFAULT (SYSDATETIME()),
        handled_at DATETIME2(7) NULL,
        CONSTRAINT fk_account_unlock_requests_user FOREIGN KEY (user_id) REFERENCES dbo.users(id),
        CONSTRAINT fk_account_unlock_requests_handled_by FOREIGN KEY (handled_by) REFERENCES dbo.users(id),
        CONSTRAINT ck_account_unlock_requests_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
    );
END;
GO

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
GO

$ErrorActionPreference = 'Stop'

$connectionString = 'Server=localhost;Database=aptisprep_db;Integrated Security=True;TrustServerCertificate=True'
$connection = [System.Data.SqlClient.SqlConnection]::new($connectionString)
$connection.Open()
$transaction = $connection.BeginTransaction()

try {
  $metadataCommand = $connection.CreateCommand()
  $metadataCommand.Transaction = $transaction
  $metadataCommand.CommandText = @'
SELECT c.TABLE_SCHEMA, c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE,
       kcu.COLUMN_NAME AS PK_COLUMN
FROM INFORMATION_SCHEMA.COLUMNS c
LEFT JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
  ON kcu.TABLE_SCHEMA = c.TABLE_SCHEMA
 AND kcu.TABLE_NAME = c.TABLE_NAME
 AND kcu.COLUMN_NAME = c.COLUMN_NAME
 AND EXISTS (
   SELECT 1
   FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
   WHERE tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
     AND tc.TABLE_SCHEMA = kcu.TABLE_SCHEMA
     AND tc.TABLE_NAME = kcu.TABLE_NAME
     AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'
 )
WHERE c.TABLE_NAME NOT IN ('flyway_schema_history')
ORDER BY c.TABLE_SCHEMA, c.TABLE_NAME, c.ORDINAL_POSITION;
'@
  $metadata = New-Object System.Data.DataTable
  $reader = $metadataCommand.ExecuteReader()
  $metadata.Load($reader)
  $reader.Close()

  $cp1252 = [System.Text.Encoding]::GetEncoding(
    1252,
    [System.Text.EncoderExceptionFallback]::new(),
    [System.Text.DecoderExceptionFallback]::new())
  $utf8 = [System.Text.UTF8Encoding]::new($false, $true)
  $markers = @('Ã', 'Â', 'Ä', 'Æ', 'Ð', 'á»', 'áº', 'â€', 'â€“', 'â€”', 'â€œ', 'â€', 'â€¦')

  function Get-MojibakeScore([string] $value) {
    $score = 0
    foreach ($marker in $markers) {
      $score += ([regex]::Matches($value, [regex]::Escape($marker))).Count
    }
    return $score
  }

  function Convert-Mojibake([string] $value) {
    if ([string]::IsNullOrEmpty($value) -or (Get-MojibakeScore $value) -eq 0) {
      return $null
    }
    try {
      $candidate = $utf8.GetString($cp1252.GetBytes($value))
    } catch {
      return $null
    }
    if ($candidate.Contains([char]0xFFFD)) {
      return $null
    }
    if ((Get-MojibakeScore $candidate) -lt (Get-MojibakeScore $value)) {
      return $candidate
    }
    return $null
  }

  $changed = 0
  $groups = $metadata | Group-Object TABLE_SCHEMA, TABLE_NAME
  foreach ($group in $groups) {
    $rows = @($group.Group)
    $pkColumns = @(
      $rows |
        Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_.PK_COLUMN) } |
        Select-Object -ExpandProperty PK_COLUMN -Unique
    )
    $textColumns = @($rows | Where-Object { $_.DATA_TYPE -in @('nvarchar', 'varchar', 'ntext', 'text') } | Select-Object -ExpandProperty COLUMN_NAME -Unique)
    if ($pkColumns.Count -ne 1 -or $textColumns.Count -eq 0) { continue }

    $schema = $rows[0].TABLE_SCHEMA
    $table = $rows[0].TABLE_NAME
    $pk = [string]$pkColumns[0]
    $textColumns = @($textColumns | ForEach-Object { [string]$_ } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ([string]::IsNullOrWhiteSpace($schema) -or [string]::IsNullOrWhiteSpace($table) -or [string]::IsNullOrWhiteSpace($pk)) { continue }
    foreach ($column in $textColumns) {
      $select = $connection.CreateCommand()
      $select.Transaction = $transaction
      $select.CommandText = "SELECT [$pk], [$column] FROM [$schema].[$table] WHERE [$column] IS NOT NULL"
      $data = New-Object System.Data.DataTable
      $reader = $select.ExecuteReader()
      $data.Load($reader)
      $reader.Close()

      foreach ($row in $data.Rows) {
        $original = [string]$row[$column]
        $fixed = Convert-Mojibake $original
        if ($null -eq $fixed) { continue }

        $update = $connection.CreateCommand()
        $update.Transaction = $transaction
        $update.CommandText = "UPDATE [$schema].[$table] SET [$column] = @value WHERE [$pk] = @id"
        $null = $update.Parameters.AddWithValue('@value', $fixed)
        $null = $update.Parameters.AddWithValue('@id', $row[$pk])
        $null = $update.ExecuteNonQuery()
        $changed++
      }
    }
  }

  $transaction.Commit()
  Write-Output "Repaired $changed text values."
} catch {
  $transaction.Rollback()
  throw
} finally {
  $connection.Close()
}

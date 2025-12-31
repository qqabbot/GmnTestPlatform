#!/bin/bash

# Database Migration Script
# Execute migration to allow duplicate cases in test plan

DB_HOST="10.48.0.13"
DB_USER="insp_dev"
DB_PASS="insp_dev@123"
DB_NAME="TestPlatform"
MIGRATION_FILE="doc/sql/migration_allow_duplicate_cases.sql"

echo "=========================================="
echo "Test Plan Migration: Allow Duplicate Cases"
echo "=========================================="
echo ""
echo "Database: $DB_NAME"
echo "Host: $DB_HOST"
echo "Migration File: $MIGRATION_FILE"
echo ""

# Check if mysql command exists
if ! command -v mysql &> /dev/null; then
    echo "❌ MySQL client not found. Please install MySQL client or run the migration manually."
    echo ""
    echo "To run manually, execute the SQL commands in: $MIGRATION_FILE"
    echo "Or use a MySQL client like MySQL Workbench, DBeaver, etc."
    exit 1
fi

# Execute migration
echo "Executing migration..."
mysql -h "$DB_HOST" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$MIGRATION_FILE"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Migration completed successfully!"
    echo ""
    echo "The test_plan_cases table now supports:"
    echo "  - Adding the same case multiple times to a plan"
    echo "  - Each instance can have different parameter_overrides"
    echo "  - Each instance can have different case_order"
else
    echo ""
    echo "❌ Migration failed. Please check the error messages above."
    echo ""
    echo "Common issues:"
    echo "  1. Table structure may have already been modified"
    echo "  2. Check if id column already exists"
    echo "  3. Check if primary key structure is different"
    echo ""
    echo "You can run the migration steps manually from: $MIGRATION_FILE"
    exit 1
fi


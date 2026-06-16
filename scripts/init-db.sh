#!/bin/bash
# Database Initialization Script for Post API
# Run this ONCE to create the database schema

set -e

DB_NAME="${1:-testdb}"
DB_USER="${2:-postgres}"
DB_PASS="${3:-postgres}"

echo "=== Initializing PostgreSQL Database: $DB_NAME ==="

# Create database if not exists
psql -h localhost -U "$DB_USER" -c "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" | grep -q 1 || \
  psql -h localhost -U "$DB_USER" -c "CREATE DATABASE $DB_NAME" || \
  echo "Database $DB_NAME may already exist"

echo "Database $DB_NAME ready"

# Create schema and data
echo "Creating schema..."
psql -h localhost -U "$DB_USER" -d "$DB_NAME" -f ../src/main/resources/schema-postgres.sql

echo ""
echo "=== Database initialization complete ==="
echo "You can now start the API with: java -jar target/post-api-1.0.0.jar"

"""
=============================================================================
SCRIPT: init_test_db.py
PROJECT: NGO Donation Management System
LOCATION: D:/project/ngo-donation-management-system/scratch/init_test_db.py
PURPOSE: Safely initialize 'ngo_donation_test_db' using scripts 02 through 07.
STRICT SAFETY RULES:
  1. Never connects to, touches, drops, alters, or references 'ngo_donation_db'.
  2. Never executes '01_create_database.sql' (which drops the dev db).
  3. Never executes '08_test_queries.sql'.
  4. Dynamically substitutes 'USE ngo_donation_db;' with 'USE ngo_donation_test_db;'.
  5. Strictly validates credentials and halts if missing or invalid.
=============================================================================
"""

import os
import re
import sys

# Try importing mysql.connector (preferred)
try:
    import mysql.connector
    HAS_MYSQL_CONNECTOR = True
except ImportError:
    HAS_MYSQL_CONNECTOR = False

import subprocess
import tempfile

def split_sql_statements(sql_text):
    """
    Splits SQL script into individual executable statements,
    handling DELIMITER $$ ... DELIMITER ; blocks properly.
    """
    statements = []
    current_delimiter = ';'
    current_stmt = []

    lines = sql_text.splitlines()
    for line in lines:
        stripped = line.strip()

        # Check for DELIMITER change
        if re.match(r'^DELIMITER\s+', stripped, re.IGNORECASE):
            parts = stripped.split()
            if len(parts) >= 2:
                current_delimiter = parts[1]
            continue

        if not stripped:
            continue

        current_stmt.append(line)

        # Check if the line ends with the active delimiter
        if stripped.endswith(current_delimiter):
            full_stmt = "\n".join(current_stmt).strip()
            # remove the trailing delimiter
            if full_stmt.endswith(current_delimiter):
                full_stmt = full_stmt[:-len(current_delimiter)].strip()
            if full_stmt:
                statements.append(full_stmt)
            current_stmt = []

    if current_stmt:
        remainder = "\n".join(current_stmt).strip()
        if remainder:
            statements.append(remainder)

    return statements


def execute_via_connector(db_host, db_port, db_user, db_pass, scripts, script_dir):
    print("Using 'mysql.connector' library for database initialization...")

    # 1. Connect without database to create ngo_donation_test_db safely
    conn = mysql.connector.connect(
        host=db_host,
        port=int(db_port),
        user=db_user,
        password=db_pass,
        autocommit=True
    )
    cursor = conn.cursor()

    # Rule 2: Clean slate - Drop and recreate ONLY ngo_donation_test_db (never touch dev db)
    drop_recreate_sql = (
        "DROP DATABASE IF EXISTS ngo_donation_test_db;\n"
        "CREATE DATABASE ngo_donation_test_db "
        "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    )
    cursor.execute("DROP DATABASE IF EXISTS ngo_donation_test_db")
    cursor.execute(
        "CREATE DATABASE ngo_donation_test_db "
        "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
    )
    print("-> Clean database 'ngo_donation_test_db' dropped and recreated successfully.")
    cursor.close()
    conn.close()

    # 2. Reconnect directly targeting ngo_donation_test_db
    conn = mysql.connector.connect(
        host=db_host,
        port=int(db_port),
        user=db_user,
        password=db_pass,
        database="ngo_donation_test_db",
        autocommit=True
    )
    cursor = conn.cursor()

    for script_name in scripts:
        script_path = os.path.join(script_dir, script_name)
        if not os.path.exists(script_path):
            print(f"[ERROR] Required SQL script missing: {script_path}", file=sys.stderr)
            sys.exit(1)

        with open(script_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # Rule 4: Redirect every 'USE ngo_donation_db;' to 'USE ngo_donation_test_db;'
        transformed = re.sub(
            r'\bUSE\s+ngo_donation_db\s*;',
            'USE ngo_donation_test_db;',
            content,
            flags=re.IGNORECASE
        )
        transformed = transformed.replace("'ngo_donation_db'", "'ngo_donation_test_db'")

        # Rule 6: Safety check - ensure no active SQL statement targets 'ngo_donation_db'
        for line_no, line in enumerate(transformed.splitlines(), start=1):
            stripped = line.strip()
            if not stripped.startswith('--') and 'ngo_donation_db' in line:
                print(
                    f"[CRITICAL SAFETY VIOLATION] {script_name}:{line_no} "
                    f"contains reference to 'ngo_donation_db': {line}",
                    file=sys.stderr
                )
                sys.exit(1)

        print(f"Executing {script_name} against 'ngo_donation_test_db'...")
        statements = split_sql_statements(transformed)
        for stmt in statements:
            # Skip empty or delimiter commands
            stmt_clean = stmt.strip()
            if not stmt_clean or stmt_clean.startswith('--'):
                continue
            try:
                cursor.execute(stmt_clean)
            except Exception as e:
                print(f"[ERROR] Execution failed for {script_name}:", file=sys.stderr)
                print(f"Statement: {stmt_clean[:200]}...", file=sys.stderr)
                print(f"Error: {e}", file=sys.stderr)
                sys.exit(1)

        print(f"-> Successfully executed {script_name}.")

    # Verify tables
    cursor.execute("SELECT table_name FROM information_schema.tables WHERE table_schema = 'ngo_donation_test_db'")
    tables = [row[0] for row in cursor.fetchall()]
    print("\nInitialization Complete. Tables in 'ngo_donation_test_db':")
    for t in tables:
        print(f"  - {t}")

    cursor.close()
    conn.close()


def execute_via_cli_secure(db_host, db_port, db_user, db_pass, scripts, script_dir):
    """
    Secure fallback when mysql.connector is not installed:
    Uses a temporary client option file (--defaults-extra-file) to avoid passing
    passwords in process command line arguments. The temporary file is securely
    deleted immediately after execution.
    """
    print("mysql.connector not installed. Using secure CLI client config (--defaults-extra-file)...")

    # Create temporary options file
    with tempfile.NamedTemporaryFile('w', delete=False, suffix='.cnf', encoding='utf-8') as f:
        f.write("[client]\n")
        f.write(f"host={db_host}\n")
        f.write(f"port={db_port}\n")
        f.write(f"user={db_user}\n")
        f.write(f"password=\"{db_pass}\"\n")
        cnf_path = f.name

    try:
        base_cmd = [
            'mysql',
            f'--defaults-extra-file={cnf_path}',
            '--default-character-set=utf8mb4'
        ]

        # Rule 2: Clean slate - Drop and recreate ONLY ngo_donation_test_db (never touch dev db)
        drop_recreate_sql = (
            "DROP DATABASE IF EXISTS ngo_donation_test_db;\n"
            "CREATE DATABASE ngo_donation_test_db "
            "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
        )
        res = subprocess.run(
            base_cmd + ['-e', drop_recreate_sql],
            capture_output=True,
            text=False
        )
        if res.returncode != 0:
            stderr_text = res.stderr.decode('utf-8', errors='replace')
            print("[ERROR] Failed to drop/recreate 'ngo_donation_test_db':", file=sys.stderr)
            print(stderr_text, file=sys.stderr)
            sys.exit(res.returncode)
        print("-> Clean database 'ngo_donation_test_db' dropped and recreated successfully.")

        for script_name in scripts:
            script_path = os.path.join(script_dir, script_name)
            if not os.path.exists(script_path):
                print(f"[ERROR] Required SQL script missing: {script_path}", file=sys.stderr)
                sys.exit(1)

            with open(script_path, 'r', encoding='utf-8') as f:
                content = f.read()

            transformed = re.sub(
                r'\bUSE\s+ngo_donation_db\s*;',
                'USE ngo_donation_test_db;',
                content,
                flags=re.IGNORECASE
            )
            transformed = transformed.replace("'ngo_donation_db'", "'ngo_donation_test_db'")

            for line_no, line in enumerate(transformed.splitlines(), start=1):
                stripped = line.strip()
                if not stripped.startswith('--') and 'ngo_donation_db' in line:
                    print(
                        f"[CRITICAL SAFETY VIOLATION] {script_name}:{line_no} "
                        f"contains reference to 'ngo_donation_db': {line}",
                        file=sys.stderr
                    )
                    sys.exit(1)

            print(f"Executing {script_name} against 'ngo_donation_test_db'...")
            p = subprocess.Popen(
                base_cmd + ['--database=ngo_donation_test_db'],
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=False
            )
            stdout_bytes, stderr_bytes = p.communicate(input=transformed.encode('utf-8'))
            stdout_text = stdout_bytes.decode('utf-8', errors='replace') if stdout_bytes else ''
            stderr_text = stderr_bytes.decode('utf-8', errors='replace') if stderr_bytes else ''

            if p.returncode != 0:
                print(f"[ERROR] Execution failed for {script_name}:", file=sys.stderr)
                print(stderr_text, file=sys.stderr)
                sys.exit(p.returncode)
            print(f"-> Successfully executed {script_name}.")

        # Verify tables
        verify_sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'ngo_donation_test_db';"
        res = subprocess.run(
            base_cmd + ['-e', verify_sql],
            capture_output=True,
            text=False
        )
        if res.returncode == 0:
            print("\nInitialization Complete. Tables in 'ngo_donation_test_db':")
            print(res.stdout.decode('utf-8', errors='replace'))
        else:
            print("[WARNING] Could not list tables in 'ngo_donation_test_db'.", file=sys.stderr)

    finally:
        if os.path.exists(cnf_path):
            os.remove(cnf_path)


def main():
    # Rule 1: Read credentials directly from os.environ
    db_user = os.environ.get('DB_USERNAME', 'root')
    db_pass = os.environ.get('DB_PASSWORD')
    db_host = os.environ.get('DB_HOST', 'localhost')
    db_port = os.environ.get('DB_PORT', '3306')

    # Rule 7: Stop immediately if credentials are missing
    if not db_pass:
        print("[ERROR] DB_PASSWORD environment variable is not set.", file=sys.stderr)
        print("Please set DB_PASSWORD and DB_USERNAME before running this script.", file=sys.stderr)
        sys.exit(1)

    # Rule 2: Do NOT print or log the password
    print(f"Connecting to MySQL server at {db_host}:{db_port} as user '{db_user}'...")

    # Rule 3 & 5: Initialize using scripts 02 through 07 only (never 01 or 08)
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    script_dir = os.path.join(base_dir, "database")

    scripts = [
        "02_create_tables.sql",
        "03_create_indexes.sql",
        "04_create_views.sql",
        "05_create_procedures.sql",
        "06_create_triggers.sql",
        "07_insert_sample_data.sql"
    ]

    # Rule 4: Prefer mysql.connector if available; otherwise use secure temporary config file
    if HAS_MYSQL_CONNECTOR:
        execute_via_connector(db_host, db_port, db_user, db_pass, scripts, script_dir)
    else:
        execute_via_cli_secure(db_host, db_port, db_user, db_pass, scripts, script_dir)


if __name__ == '__main__':
    main()

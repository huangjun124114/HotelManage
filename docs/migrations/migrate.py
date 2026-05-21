#!/usr/bin/env python3
"""Production DB migration - V20260521"""
import sqlite3
import sys

DB_PATH = "/opt/hotelmanage/data/linxi.db"

def main():
    conn = sqlite3.connect(DB_PATH)
    conn.execute("PRAGMA writable_schema = ON")
    cur = conn.cursor()

    print("=== Migration Start ===")

    # 1. Delete role_menu for menus 12, 42
    cur.execute("DELETE FROM sys_role_menu WHERE menu_id IN (12, 42)")
    print("1. Deleted role_menu 12/42:", cur.rowcount)

    # 2. Delete menus 12, 42
    cur.execute("DELETE FROM sys_menu WHERE id IN (12, 42)")
    print("2. Deleted menus 12/42:", cur.rowcount)

    # 3. Move investor list to store management
    cur.execute("UPDATE sys_menu SET parent_id = 10, sort_no = 2 WHERE id = 41")
    print("3. Updated menu 41:", cur.rowcount)

    # 4. Delete role_menu for menu 40
    cur.execute("DELETE FROM sys_role_menu WHERE menu_id = 40")
    print("4. Deleted role_menu 40:", cur.rowcount)

    # 5. Delete investor management menu
    cur.execute("DELETE FROM sys_menu WHERE id = 40")
    print("5. Deleted menu 40:", cur.rowcount)

    # 6. Fix investment ratio
    cur.execute("UPDATE investor_store SET investment_ratio = investment_ratio * 100 WHERE investment_ratio < 1 AND investment_ratio > 0")
    print("6. Fixed ratios:", cur.rowcount)

    conn.commit()
    print("\n=== Migration Committed ===")

    # Verify
    cur.execute("SELECT id, parent_id, menu_name FROM sys_menu WHERE id IN (12, 40, 41, 42)")
    print("Menus 12/40/41/42:", cur.fetchall())

    cur.execute("SELECT id, menu_name, sort_no FROM sys_menu WHERE parent_id = 10 ORDER BY sort_no")
    print("Store management children:", cur.fetchall())

    cur.execute("SELECT COUNT(*) FROM investor_store WHERE investment_ratio > 0 AND investment_ratio < 1")
    print("Small ratio count:", cur.fetchone()[0])

    conn.close()
    print("=== Done ===")

if __name__ == "__main__":
    main()

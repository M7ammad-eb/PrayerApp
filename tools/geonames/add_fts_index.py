#!/usr/bin/env python3
"""Adds or rebuilds the city-name FTS4 index in an existing places.db asset."""

import sqlite3
import sys


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: python add_fts_index.py <places.db>")

    database_path = sys.argv[1]
    connection = sqlite3.connect(database_path)
    try:
        cursor = connection.cursor()
        cursor.execute(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS places_fts USING fts4(
                nameEn,
                asciiName,
                nameAr,
                content='places',
                tokenize=unicode61
            )
            """
        )
        cursor.execute("INSERT INTO places_fts(places_fts) VALUES('rebuild')")
        cursor.execute("CREATE TABLE IF NOT EXISTS places_search_meta(version INTEGER NOT NULL)")
        cursor.execute("DELETE FROM places_search_meta")
        cursor.execute("INSERT INTO places_search_meta(version) VALUES(1)")
        connection.commit()
        cursor.execute("VACUUM")
        count = cursor.execute("SELECT count(*) FROM places_fts").fetchone()[0]
        print(f"Indexed {count} places in {database_path}")
    finally:
        connection.close()


if __name__ == "__main__":
    main()

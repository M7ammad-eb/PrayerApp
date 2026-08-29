#!/usr/bin/env python3
"""Builds app/src/main/assets/places.db from the raw GeoNames dumps.

Not run as part of the app build - a one-time (or "rerun when GeoNames data is refreshed")
dev-time step. Requires a Room-schema-correct empty seed database as a starting point (see
SeedSchemaGeneratorTest, which produces one with the correct room_master_table identity hash for
PlaceEntity/PlacesDatabase) so the populated result opens cleanly via Room's createFromAsset.

Usage:
    python build_places_db.py <cities5000.zip> <alternateNamesV2.zip> <seed_places.db> <output places.db>

Data source: GeoNames (https://download.geonames.org/export/dump/), licensed CC BY 4.0.
"""
import shutil
import sqlite3
import sys
import zipfile

CITIES_COLUMNS = 19  # geonameid..modification date, per GeoNames cities5000.txt format


def load_cities(cities_zip_path):
    """Returns {geonameid: (nameEn, asciiName, countryCode, lat, lon, timeZoneId, population)}."""
    places = {}
    with zipfile.ZipFile(cities_zip_path) as zf:
        name = next(n for n in zf.namelist() if n.endswith(".txt"))
        with zf.open(name) as f:
            for raw in f:
                line = raw.decode("utf-8").rstrip("\n")
                if not line:
                    continue
                cols = line.split("\t")
                if len(cols) < CITIES_COLUMNS:
                    continue
                geonameid = int(cols[0])
                name_en = cols[1]
                ascii_name = cols[2]
                lat = float(cols[4])
                lon = float(cols[5])
                country_code = cols[8]
                population = int(cols[14]) if cols[14] else 0
                timezone = cols[17]
                places[geonameid] = (name_en, ascii_name, country_code, lat, lon, timezone, population)
    return places


def load_arabic_names(alt_names_zip_path, wanted_ids):
    """Returns {geonameid: nameAr}, preferring isPreferredName rows, else the first seen."""
    names = {}
    preferred = set()
    with zipfile.ZipFile(alt_names_zip_path) as zf:
        name = next(n for n in zf.namelist() if n.lower().startswith("alternatenamesv2"))
        with zf.open(name) as f:
            for raw in f:
                line = raw.decode("utf-8", errors="replace").rstrip("\n")
                if not line:
                    continue
                cols = line.split("\t")
                if len(cols) < 5:
                    continue
                if cols[2] != "ar":
                    continue
                geonameid = int(cols[1])
                if geonameid not in wanted_ids:
                    continue
                is_preferred = len(cols) > 4 and cols[4] == "1"
                if geonameid in preferred and not is_preferred:
                    continue
                names[geonameid] = cols[3]
                if is_preferred:
                    preferred.add(geonameid)
    return names


def main():
    if len(sys.argv) != 5:
        print(__doc__)
        sys.exit(1)
    cities_zip, alt_names_zip, seed_db, out_db = sys.argv[1:5]

    print("Parsing cities5000...")
    places = load_cities(cities_zip)
    print(f"  {len(places)} places loaded")

    print("Filtering Arabic alternate names (streaming, this takes a while)...")
    arabic_names = load_arabic_names(alt_names_zip, set(places.keys()))
    print(f"  {len(arabic_names)} Arabic names matched")

    shutil.copyfile(seed_db, out_db)
    con = sqlite3.connect(out_db)
    cur = con.cursor()
    rows = [
        (
            geonameid,
            name_en,
            arabic_names.get(geonameid),
            ascii_name,
            country_code,
            lat,
            lon,
            timezone,
            population,
        )
        for geonameid, (name_en, ascii_name, country_code, lat, lon, timezone, population) in places.items()
    ]
    cur.executemany(
        """
        INSERT INTO places
            (geonameId, nameEn, nameAr, asciiName, countryCode, latitude, longitude, timeZoneId, population)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        rows,
    )
    cur.execute("CREATE INDEX idx_places_lat ON places(latitude)")
    cur.execute("CREATE INDEX idx_places_lon ON places(longitude)")
    cur.execute(
        """
        CREATE VIRTUAL TABLE places_fts USING fts4(
            nameEn,
            asciiName,
            nameAr,
            content='places',
            tokenize=unicode61
        )
        """
    )
    cur.execute("INSERT INTO places_fts(places_fts) VALUES('rebuild')")
    cur.execute("CREATE TABLE places_search_meta(version INTEGER NOT NULL)")
    cur.execute("INSERT INTO places_search_meta(version) VALUES(1)")
    con.commit()
    cur.execute("VACUUM")
    con.close()
    print(f"Wrote {len(rows)} rows to {out_db}")


if __name__ == "__main__":
    main()

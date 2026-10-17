import Foundation
import SQLite3
import SalatiCore

struct Place: Identifiable, Sendable {
    let id: Int64
    let name: String
    let arabicName: String?
    let country: String
    let latitude: Double
    let longitude: Double
    let zone: String
    var location: SavedLocation { .init(name:name,arabicName:arabicName,latitude:latitude,longitude:longitude,zone:zone) }
}
actor OfflinePlaces {
    private var database: OpaquePointer?
    private func open() throws -> OpaquePointer {
        if let database { return database }
        guard let url = Bundle.main.url(forResource:"places",withExtension:"db") else { throw CocoaError(.fileNoSuchFile) }
        var handle: OpaquePointer?
        guard sqlite3_open_v2(url.path,&handle,SQLITE_OPEN_READONLY,nil) == SQLITE_OK, let handle else { throw CocoaError(.fileReadCorruptFile) }
        database = handle
        return handle
    }
    private func rows(_ sql: String, text: [String] = [], numbers: [Double] = []) throws -> [Place] {
        let db = try open()
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(db,sql,-1,&statement,nil) == SQLITE_OK, let statement else { throw CocoaError(.fileReadCorruptFile) }
        defer { sqlite3_finalize(statement) }
        let transient = unsafeBitCast(-1,to:sqlite3_destructor_type.self)
        for (i,value) in text.enumerated() { sqlite3_bind_text(statement,Int32(i+1),value,-1,transient) }
        for (i,value) in numbers.enumerated() { sqlite3_bind_double(statement,Int32(i+1),value) }
        func string(_ column: Int32) -> String { guard let c = sqlite3_column_text(statement,column) else { return "" }; return String(cString:c) }
        var result: [Place] = []
        var status = sqlite3_step(statement)
        while status == SQLITE_ROW {
            result.append(Place(id:sqlite3_column_int64(statement,0),name:string(1),arabicName:string(2).isEmpty ? nil : string(2),country:string(4),latitude:sqlite3_column_double(statement,5),longitude:sqlite3_column_double(statement,6),zone:string(7)))
            status = sqlite3_step(statement)
        }
        guard status == SQLITE_DONE else { throw CocoaError(.fileReadCorruptFile) }
        return result
    }
    func search(_ query: String) throws -> [Place] {
        let trimmed = query.trimmingCharacters(in:.whitespacesAndNewlines)
        let tokens = SalatiEngine().searchTokens(query:trimmed)
        guard !tokens.isEmpty else { return [] }
        return try rows("""
        SELECT p.* FROM places p JOIN places_fts ON places_fts.docid=p.geonameId
        WHERE places_fts MATCH ?
        ORDER BY CASE WHEN lower(p.nameEn)=lower(?) OR lower(p.asciiName)=lower(?) OR p.nameAr=? THEN 0
        WHEN lower(p.nameEn) LIKE lower(?) || '%' OR lower(p.asciiName) LIKE lower(?) || '%' OR p.nameAr LIKE ? || '%' THEN 1 ELSE 2 END, p.population DESC LIMIT 50
        """,text:[tokens,trimmed,trimmed,trimmed,trimmed,trimmed,trimmed])
    }
    func nearest(latitude: Double, longitude: Double) throws -> (Place,Double)? {
        var candidates: [Place] = []
        for delta in [2.0,5.0] {
            candidates = try rows("SELECT * FROM places WHERE latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?",numbers:[latitude-delta,latitude+delta,longitude-delta,longitude+delta])
            if !candidates.isEmpty { break }
        }
        if candidates.isEmpty { candidates = try rows("SELECT * FROM places") }
        return candidates.map { ($0,GeoUtils.shared.haversineDistanceKm(lat1:latitude,lon1:longitude,lat2:$0.latitude,lon2:$0.longitude)) }.min { $0.1 < $1.1 }
    }
}

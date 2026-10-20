import Foundation

// iOS owns language selection. Adding a localization does not require adding a
// picker case or a second persisted preference that overrides Settings.app.
enum AppLanguage {
    static var current:String {
        resolved(supported:Bundle.main.localizations,
                 preferred:Bundle.main.preferredLocalizations,
                 fallback:Bundle.main.developmentLocalization ?? "en")
    }
    static func resolved(supported:[String],preferred:[String],fallback:String="en")->String {
        let available=supported.filter {$0 != "Base"}
        return Bundle.preferredLocalizations(from:available,forPreferences:preferred).first ?? fallback
    }
    static func isRightToLeft(_ identifier:String)->Bool {
        Locale.Language(identifier:identifier).characterDirection == .rightToLeft
    }
    static func locale(_ identifier:String)->Locale {Locale(identifier:identifier+"@numbers=latn")}
    static func text(_ key:String,language:String)->String {
        let selected=resolved(supported:Bundle.main.localizations,preferred:[language])
        let bundle=Bundle.main.path(forResource:selected,ofType:"lproj").flatMap(Bundle.init(path:)) ?? .main
        return bundle.localizedString(forKey:key,value:key,table:nil)
    }
    static func time(_ date:Date,language:String,zone:TimeZone,use24Hour:Bool)->String {
        let formatter=DateFormatter()
        formatter.locale=locale(language);formatter.calendar=Calendar(identifier:.gregorian);formatter.timeZone=zone
        formatter.dateFormat=use24Hour ? "HH:mm":"h:mm a"
        return formatter.string(from:date)
    }
    static func name(_ identifier:String)->String {
        Locale(identifier:identifier).localizedString(forIdentifier:identifier) ?? identifier
    }
}

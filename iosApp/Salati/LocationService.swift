import CoreLocation
import Combine
import SalatiCore

struct LocationFix {
    let latitude:Double
    let longitude:Double
    let accuracy:Double
    let timestamp:Date
}
@MainActor final class LocationService:NSObject,ObservableObject,CLLocationManagerDelegate {
    private let manager=CLLocationManager()
    private var waitingForAuthorization=false
    private var explicitRequest=true
    private var timeout:Task<Void,Never>?
    @Published var heading:Double?
    @Published var accuracy:Double?
    @Published var message:String?
    @Published var headingMessage:String?
    @Published var isAcquiring=false
    @Published var lastUpdate:Date?
    var onFix:((LocationFix,Bool)->Void)?
    private var fixture:Bool {ProcessInfo.processInfo.arguments.contains("-uiTestingHeading90")}
    override init() {
        super.init();manager.delegate=self;manager.desiredAccuracy=kCLLocationAccuracyHundredMeters;manager.headingFilter=2
        if fixture {heading=90;accuracy=3}
    }
    func request(explicit:Bool=true) {
        guard !isAcquiring else {return}
        if ProcessInfo.processInfo.arguments.contains("-uiTestingGPSDenied") {message="Location access is unavailable. Choose a city or enter coordinates.";return}
        explicitRequest=explicit;isAcquiring=true;message="Finding your location…"
        switch manager.authorizationStatus {
        case .notDetermined:
            waitingForAuthorization=true;message="Requesting location permission…";manager.requestWhenInUseAuthorization()
        case .authorizedAlways,.authorizedWhenInUse:acquire()
        default:finish("Location access is unavailable. Choose a city or enter coordinates.")
        }
    }
    private func acquire() {
        manager.requestLocation()
        timeout?.cancel();timeout=Task { [weak self] in
            do {try await Task.sleep(for:.seconds(25));try Task.checkCancellation();self?.finish("Location request timed out. Try again or choose a city.")}
            catch {}
        }
    }
    func cancel() {timeout?.cancel();manager.stopUpdatingLocation();waitingForAuthorization=false;isAcquiring=false}
    private func finish(_ text:String) {cancel();message=text}
    func status(_ text:String,date:Date?=nil) {message=text;if let date {lastUpdate=date}}
    func startHeading() {
        if fixture {heading=90;accuracy=3;return}
        guard CLLocationManager.headingAvailable() else {headingMessage="This device has no compass. The Qibla bearing is still available.";return}
        headingMessage=nil
        manager.startUpdatingHeading()
    }
    func stopHeading() {manager.stopUpdatingHeading()}
    nonisolated func locationManagerDidChangeAuthorization(_ manager:CLLocationManager) {
        let authorized=manager.authorizationStatus == .authorizedWhenInUse || manager.authorizationStatus == .authorizedAlways
        Task {@MainActor [weak self] in
            guard let self,self.waitingForAuthorization else {return}
            self.waitingForAuthorization=false
            if authorized {self.message="Finding your location…";self.acquire()}
            else {self.finish("Location access is unavailable. Choose a city or enter coordinates.")}
        }
    }
    nonisolated func locationManager(_ manager:CLLocationManager,didUpdateLocations locations:[CLLocation]) {
        guard let fix=locations.last else {return}
        let sample=LocationFix(latitude:fix.coordinate.latitude,longitude:fix.coordinate.longitude,accuracy:fix.horizontalAccuracy,timestamp:fix.timestamp)
        Task {@MainActor [weak self] in
            guard let self,self.isAcquiring else {return}
            guard LocationAcceptance.shared.usableFix(accuracyMeters:sample.accuracy,fixEpoch:sample.timestamp.timeIntervalSince1970,nowEpoch:Date().timeIntervalSince1970,explicit:self.explicitRequest) else {
                self.finish("The location is stale or imprecise. Try again or choose a city.");return
            }
            let explicit=self.explicitRequest;self.cancel();self.lastUpdate=sample.timestamp
            self.message="Resolving your offline place…";self.onFix?(sample,explicit)
        }
    }
    nonisolated func locationManager(_ manager:CLLocationManager,didUpdateHeading value:CLHeading) {
        let accuracy=value.headingAccuracy,heading=value.trueHeading
        Task {@MainActor [weak self] in self?.accuracy=accuracy;self?.heading=accuracy>=0 && heading>=0 ? heading:nil}
    }
    nonisolated func locationManager(_ manager:CLLocationManager,didFailWithError error:Error) {
        Task {@MainActor [weak self] in self?.finish("Location could not be found. Try again or choose a city.")}
    }
    nonisolated func locationManagerShouldDisplayHeadingCalibration(_ manager:CLLocationManager)->Bool {true}
}

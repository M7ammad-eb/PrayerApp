import CoreLocation
import Combine

@MainActor final class LocationService: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    @Published var heading: Double?
    @Published var accuracy: Double?
    @Published var message: String?
    var onFix: ((CLLocation) -> Void)?
    override init() { super.init(); manager.delegate=self; manager.desiredAccuracy=kCLLocationAccuracyHundredMeters }
    func request() {
        switch manager.authorizationStatus {
        case .notDetermined: manager.requestWhenInUseAuthorization()
        case .authorizedAlways,.authorizedWhenInUse: manager.requestLocation()
        default: message="Location access is unavailable. Choose a city or enter coordinates."
        }
    }
    func startHeading() {
        guard CLLocationManager.headingAvailable() else { message="This device has no compass. The Qibla bearing is still available."; return }
        manager.startUpdatingHeading()
    }
    func stopHeading() { manager.stopUpdatingHeading() }
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        if manager.authorizationStatus == .authorizedWhenInUse || manager.authorizationStatus == .authorizedAlways { manager.requestLocation() }
    }
    func locationManager(_ manager: CLLocationManager,didUpdateLocations locations: [CLLocation]) {
        guard let fix=locations.last, fix.horizontalAccuracy >= 0, abs(fix.timestamp.timeIntervalSinceNow)<120 else { return }
        onFix?(fix)
    }
    func locationManager(_ manager: CLLocationManager,didUpdateHeading newHeading: CLHeading) {
        accuracy=newHeading.headingAccuracy
        heading=newHeading.headingAccuracy >= 0 && newHeading.trueHeading >= 0 ? newHeading.trueHeading : nil
    }
    func locationManager(_ manager: CLLocationManager,didFailWithError error: Error) { message=error.localizedDescription }
    func locationManagerShouldDisplayHeadingCalibration(_ manager: CLLocationManager) -> Bool { true }
}

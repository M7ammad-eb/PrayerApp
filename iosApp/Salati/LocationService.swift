import CoreLocation
import Combine

@MainActor final class LocationService: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var waitingForAuthorization=false
    @Published var heading: Double?
    @Published var accuracy: Double?
    @Published var message: String?
    var onFix: ((Double,Double) -> Void)?
    override init() { super.init(); manager.delegate=self; manager.desiredAccuracy=kCLLocationAccuracyHundredMeters }
    func request() {
        switch manager.authorizationStatus {
        case .notDetermined: waitingForAuthorization=true; manager.requestWhenInUseAuthorization()
        case .authorizedAlways,.authorizedWhenInUse: manager.requestLocation()
        default: message="Location access is unavailable. Choose a city or enter coordinates."
        }
    }
    func startHeading() {
        guard CLLocationManager.headingAvailable() else { message="This device has no compass. The Qibla bearing is still available."; return }
        manager.startUpdatingHeading()
    }
    func stopHeading() { manager.stopUpdatingHeading() }
    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let authorized=manager.authorizationStatus == .authorizedWhenInUse || manager.authorizationStatus == .authorizedAlways
        Task { @MainActor [weak self] in
            guard let self,self.waitingForAuthorization else {return}
            self.waitingForAuthorization=false
            if authorized {self.manager.requestLocation()}
            else {self.message="Location access is unavailable. Choose a city or enter coordinates."}
        }
    }
    nonisolated func locationManager(_ manager: CLLocationManager,didUpdateLocations locations: [CLLocation]) {
        guard let fix=locations.last,fix.horizontalAccuracy>=0,abs(fix.timestamp.timeIntervalSinceNow)<120 else {return}
        let latitude=fix.coordinate.latitude,longitude=fix.coordinate.longitude
        Task { @MainActor [weak self] in self?.onFix?(latitude,longitude) }
    }
    nonisolated func locationManager(_ manager: CLLocationManager,didUpdateHeading newHeading: CLHeading) {
        let accuracy=newHeading.headingAccuracy,trueHeading=newHeading.trueHeading
        Task { @MainActor [weak self] in
            self?.accuracy=accuracy
            self?.heading=accuracy>=0 && trueHeading>=0 ? trueHeading : nil
        }
    }
    nonisolated func locationManager(_ manager: CLLocationManager,didFailWithError error: Error) {
        let message=error.localizedDescription
        Task { @MainActor [weak self] in self?.message=message }
    }
    nonisolated func locationManagerShouldDisplayHeadingCalibration(_ manager: CLLocationManager) -> Bool {true}
}

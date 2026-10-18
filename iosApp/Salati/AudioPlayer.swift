import AVFoundation
import Combine
import UIKit

@MainActor final class AdhanPlayer: NSObject, ObservableObject, AVAudioPlayerDelegate {
    static let shared=AdhanPlayer()
    @Published var isPlaying=false
    private var player: AVAudioPlayer?
    @Published var error: String?
    private var interrupted=false
    private var observers:[NSObjectProtocol]=[]
    override init() {
        super.init()
        observers.append(NotificationCenter.default.addObserver(forName:AVAudioSession.interruptionNotification,object:AVAudioSession.sharedInstance(),queue:.main) { [weak self] notification in
            let type=notification.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt
            let options=notification.userInfo?[AVAudioSessionInterruptionOptionKey] as? UInt ?? 0
            Task { @MainActor in self?.interruption(type:type,options:options) }
        })
        observers.append(NotificationCenter.default.addObserver(forName:AVAudioSession.routeChangeNotification,object:AVAudioSession.sharedInstance(),queue:.main) { [weak self] notification in
            let raw=notification.userInfo?[AVAudioSessionRouteChangeReasonKey] as? UInt
            Task { @MainActor in
                guard raw == AVAudioSession.RouteChangeReason.oldDeviceUnavailable.rawValue else {return}
                self?.stop(reason:"audio route removed; paused rather than switching unexpectedly to speaker")
            }
        })
        observers.append(NotificationCenter.default.addObserver(forName:AVAudioSession.mediaServicesWereResetNotification,object:AVAudioSession.sharedInstance(),queue:.main) { [weak self] _ in
            Task { @MainActor in self?.stop(reason:"audio media services reset; playback requires explicit restart") }
        })
    }
    deinit {for observer in observers {NotificationCenter.default.removeObserver(observer)}}
    func play(_ name: String) {
        do {
            error=nil
            guard let url=Bundle.main.url(forResource:name,withExtension:"mp3") else { throw CocoaError(.fileNoSuchFile) }
            player?.stop();interrupted=false
            try AVAudioSession.sharedInstance().setCategory(.playback,mode:.default)
            try AVAudioSession.sharedInstance().setActive(true)
            player=try AVAudioPlayer(contentsOf:url)
            player?.delegate=self
            player?.numberOfLoops=0
            guard player?.play() == true else { throw CocoaError(.fileReadUnknown) }
            isPlaying=true
            PrayerScheduler.shared.recordAudio("user-initiated original recording started; \(name); duration \(Int(player?.duration ?? 0))s; app state \(UIApplication.shared.applicationState.rawValue)")
        } catch {isPlaying=false;self.error=error.localizedDescription;PrayerScheduler.shared.recordAudio("playback failed: "+error.localizedDescription)}
    }
    nonisolated func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer,successfully flag: Bool) {
        let identity=ObjectIdentifier(player)
        Task { @MainActor [weak self] in
            guard let self,let current=self.player,ObjectIdentifier(current) == identity else {return}
            self.stop(reason:flag ? "recording completed" : "recording ended unsuccessfully")
        }
    }
    nonisolated func audioPlayerDecodeErrorDidOccur(_ player:AVAudioPlayer,error:Error?) {
        let message=error?.localizedDescription ?? "Audio decoding failed"
        let identity=ObjectIdentifier(player)
        Task { @MainActor [weak self] in
            guard let self,let current=self.player,ObjectIdentifier(current) == identity else {return}
            self.error=message;self.stop(reason:message)
        }
    }
    private func interruption(type:UInt?,options:UInt) {
        guard let type,let kind=AVAudioSession.InterruptionType(rawValue:type) else {return}
        if kind == .began {
            interrupted=isPlaying;player?.pause();isPlaying=false
            PrayerScheduler.shared.recordAudio("system audio interruption began; user-initiated player paused")
        } else {
            let mayResume=AVAudioSession.InterruptionOptions(rawValue:options).contains(.shouldResume)
            PrayerScheduler.shared.recordAudio("system audio interruption ended; shouldResume \(mayResume)")
            guard interrupted,mayResume else {interrupted=false;return}
            do {try AVAudioSession.sharedInstance().setActive(true);isPlaying=player?.play() == true;interrupted=false}
            catch {self.error=error.localizedDescription;stop(reason:"could not resume after system interruption")}
        }
    }
    func stop(reason:String="user stopped playback") {
        isPlaying=false;interrupted=false;player?.stop();player=nil
        try? AVAudioSession.sharedInstance().setActive(false,options:.notifyOthersOnDeactivation)
        PrayerScheduler.shared.recordAudio(reason)
    }
}

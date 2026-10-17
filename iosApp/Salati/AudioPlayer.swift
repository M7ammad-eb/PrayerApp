import AVFoundation
import Combine

@MainActor final class AdhanPlayer: NSObject, ObservableObject, AVAudioPlayerDelegate {
    static let shared=AdhanPlayer()
    @Published var isPlaying=false
    private var player: AVAudioPlayer?
    @Published var error: String?
    func play(_ name: String) {
        do {
            guard let url=Bundle.main.url(forResource:name,withExtension:"mp3") else { throw CocoaError(.fileNoSuchFile) }
            try AVAudioSession.sharedInstance().setCategory(.playback,mode:.default)
            try AVAudioSession.sharedInstance().setActive(true)
            player=try AVAudioPlayer(contentsOf:url)
            player?.delegate=self
            guard player?.play() == true else { throw CocoaError(.fileReadUnknown) }
            isPlaying=true
        } catch { self.error=error.localizedDescription }
    }
    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer,successfully flag: Bool) {stop()}
    func stop() { isPlaying=false; player?.stop(); try? AVAudioSession.sharedInstance().setActive(false,options:.notifyOthersOnDeactivation) }
}

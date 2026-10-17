import AVFoundation
import Combine

@MainActor final class AdhanPlayer: ObservableObject {
    private var player: AVAudioPlayer?
    @Published var error: String?
    func play(_ name: String) {
        do {
            guard let url=Bundle.main.url(forResource:name,withExtension:"mp3") else { throw CocoaError(.fileNoSuchFile) }
            try AVAudioSession.sharedInstance().setCategory(.playback,mode:.default)
            try AVAudioSession.sharedInstance().setActive(true)
            player=try AVAudioPlayer(contentsOf:url)
            guard player?.play() == true else { throw CocoaError(.fileReadUnknown) }
        } catch { self.error=error.localizedDescription }
    }
    func stop() { player?.stop(); try? AVAudioSession.sharedInstance().setActive(false,options:.notifyOthersOnDeactivation) }
}

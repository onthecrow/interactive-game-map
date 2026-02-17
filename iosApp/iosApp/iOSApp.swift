import SwiftUI
import GoogleMaps
import ComposeApp

@main
struct iOSApp: App {

    init() {
        let key = Bundle.main.object(forInfoDictionaryKey: "GMSApiKey") as? String ?? ""
        GMSServices.provideAPIKey(key)
        AppInitializer.shared.initialize(platform: IOSPlatform())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

import SwiftUI
import GoogleMaps
import FirebaseCore
import ComposeApp

@main
struct iOSApp: App {

    init() {
        let key = Bundle.main.object(forInfoDictionaryKey: "GMSApiKey") as? String ?? ""
        GMSServices.provideAPIKey(key)
        FirebaseApp.configure()
        AppInitializer.shared.initialize(platform: IOSPlatform())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

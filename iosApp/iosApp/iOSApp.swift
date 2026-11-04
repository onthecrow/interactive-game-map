import SwiftUI
import GoogleMaps
import FirebaseAnalytics
import FirebaseCore
import FirebaseCrashlytics
import ComposeApp

@main
struct iOSApp: App {

    init() {
        let key = Bundle.main.object(forInfoDictionaryKey: "GMSApiKey") as? String ?? ""
        GMSServices.provideAPIKey(key)
        FirebaseApp.configure()
        // Collection is off in Info.plist, the value set here also replaces the one saved by another build
        #if DEBUG
        let isFirebaseCollectionEnabled = false
        #else
        let isFirebaseCollectionEnabled = true
        #endif
        Analytics.setAnalyticsCollectionEnabled(isFirebaseCollectionEnabled)
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(isFirebaseCollectionEnabled)
        AppInitializer.shared.initialize(platform: IOSPlatform())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

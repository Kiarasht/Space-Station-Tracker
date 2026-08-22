import SwiftUI

struct RootView: View {
    @StateObject private var model = AppModel()
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.colorScheme) private var systemColorScheme
    @AppStorage(
        "theme",
        store: UserDefaults(suiteName: "settings")
    ) private var selectedTheme = "Follow System"
    @State private var isReturningFromBackground = false

    var body: some View {
        ComposeRootViewController(model: model)
            .ignoresSafeArea()
            .background(appBackground.ignoresSafeArea())
            .preferredColorScheme(preferredColorScheme)
            .task {
                model.start()
            }
            .onChange(of: scenePhase) { phase in
                if phase == .background {
                    isReturningFromBackground = true
                } else if phase == .active, isReturningFromBackground {
                    isReturningFromBackground = false
                    model.appDidReturnFromBackground()
                }
            }
    }

    private var preferredColorScheme: ColorScheme? {
        switch selectedTheme {
        case "Dark": .dark
        case "Light": .light
        default: nil
        }
    }

    private var usesDarkColors: Bool {
        selectedTheme == "Dark" ||
            (selectedTheme == "Follow System" && systemColorScheme == .dark)
    }

    private var appBackground: Color {
        usesDarkColors
            ? Color(red: 0, green: 0, blue: 32.0 / 255.0)
            : Color(red: 240.0 / 255.0, green: 242.0 / 255.0, blue: 245.0 / 255.0)
    }

}

private struct ComposeRootViewController: UIViewControllerRepresentable {
    @ObservedObject var model: AppModel

    func makeUIViewController(context: Context) -> UIViewController {
        model.makeComposeViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        uiViewController.view.backgroundColor = .clear
    }
}

#Preview {
    RootView()
}

import SwiftUI

struct CurrencySettingsView: View {
    @AppStorage("preferredCurrency") private var selectedCurrency: String = "USD"

    let supportedCurrencies = ["USD ($)", "KRW (₩)"]

    var body: some View {
        Form {
            Section(header: Text("settings.currency.select".localized())) {
                ForEach(supportedCurrencies, id: \.self) { currency in
                    HStack {
                        Text(currency)
                        Spacer()
                        if selectedCurrency == currency {
                            Image(systemName: "checkmark")
                                .foregroundColor(.blue)
                        }
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        selectedCurrency = currency
                    }
                }
            }
        }
        .navigationTitle("settings.option.currency".localized())
    }
}

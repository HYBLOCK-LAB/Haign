import SwiftUI

struct WalletInfoCard: View {
    let wallet: Wallet

    var totalValueUSD: Double {
        wallet.balance * wallet.coin.pricePerUnit
    }

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            Image(wallet.coin.iconName)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 30, height: 30)
                .clipShape(Circle())
                .background(Color.white)

            VStack(alignment: .leading, spacing: 4) {
                Text("\(wallet.network.displayName)")
                    .font(.caption)
                    .foregroundColor(.gray)

                Text(wallet.coin.symbol)
                    .font(.headline)

                Text("$\(wallet.coin.pricePerUnit, specifier: "%.2f") (▲1.25%)")
                    .font(.caption)
                    .foregroundColor(.green)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 4) {
                Text(wallet.address)
                    .font(.caption)
                    .foregroundColor(.gray)
                    .lineLimit(1)
                    .truncationMode(.tail)
                Text("\(wallet.balance, specifier: "%.4f") \(wallet.coin.symbol)")
                Text("$\(totalValueUSD, specifier: "%.2f")")
                    .bold()
            }
        }
        .font(.subheadline)
        .padding(.vertical, 8)
    }
}

import SwiftUI

struct MyWalletView: View {
    @StateObject private var walletViewModel = WalletViewModel()
    @StateObject private var nfcViewModel = NFCViewModel()

    // MARK: UI states

    @State private var isShowNFCWait = false
    @State private var isShowAddWallet = false
    @State private var selectedTab: WalletType = .app
    @State private var highlightedWalletId: UUID? = nil

    var body: some View {
        NavigationView {
            VStack {
                Picker("Wallet Type", selection: $selectedTab) {
                    ForEach(WalletType.allCases, id: \.self) { type in
                        Text(type.localizedName).tag(type)
                    }
                }
                .pickerStyle(SegmentedPickerStyle())
                .padding()

                if walletViewModel.isLoading && walletViewModel.wallets.isEmpty {
                    ProgressView()
                        .padding()
                }

                if walletViewModel.filteredWallets(for: selectedTab).isEmpty {
                    Spacer()
                    Text("wallet.not.found".localized())
                        .font(.title3)
                        .foregroundColor(.secondary)

                    Button(action: {
                        isShowAddWallet = true
                    }) {
                        VStack {
                            Image(systemName: "plus")
                                .font(.system(size: 30, weight: .bold))
                                .foregroundColor(.gray)
                                .padding()
                        }
                        .frame(maxWidth: .infinity, minHeight: 60)
                        .background(Color.secondary.opacity(0.1))
                        .cornerRadius(12)
                        .padding(.horizontal, 40)
                    }
                    .buttonStyle(PlainButtonStyle())
                    Spacer()
                } else {
                    List {
                        ForEach(walletViewModel.filteredWallets(for: selectedTab)) { wallet in
                            Button(action: {
                                // TODO: detail page
                            }) {
                                VStack(spacing: 0) {
                                    WalletInfoCard(wallet: wallet)
                                        .padding(.vertical, 8)
                                        .contentShape(Rectangle())
                                    Divider()
                                        .background(Color.gray.opacity(0.3))
                                }
                                .background(
                                    highlightedWalletId == wallet.id ? Color.gray.opacity(0.1) : Color.clear
                                )
                            }
                            .buttonStyle(.plain)
                            .listRowSeparator(.hidden)
                            .listRowBackground(Color.clear)
                        }
                    }
                    .listStyle(.plain)

                    Button(action: {
                        isShowNFCWait = true
                    }) {
                        VStack {
                            Image(systemName: "plus")
                                .font(.system(size: 24, weight: .bold))
                                .foregroundColor(.gray)
                                .padding()
                        }
                        .frame(maxWidth: .infinity, minHeight: 40)
                        .background(Color.secondary.opacity(0.1))
                        .cornerRadius(12)
                        .padding(.horizontal, 20)
                        .padding(.bottom, 30)
                    }
                    .buttonStyle(.plain)
                }
            }
            .navigationTitle("main.tab.wallet".localized())
            .task {
                walletViewModel.load()
            }.refreshable {
                await walletViewModel.refreshAll()
            }
            .sheet(isPresented: $isShowNFCWait) {
                if let cmd = APDUCommand.verifyPIN("1234") {
                    NFCWaitingView(
                        isPresented: $isShowNFCWait,
                        viewModel: nfcViewModel,
                        command: cmd
                    )
                } else {
                    Text("Invalid PIN format")
                        .onAppear { isShowNFCWait = false }
                }
            }
            .fullScreenCover(isPresented: $isShowAddWallet) {
                WalletAddView(walletType: selectedTab)
            }
        }
    }
}

//
//  SignatureViewModel.swift
//  Wallet
//
//  Created by jiseop9083 on 7/13/25.
//

import Combine
import Foundation

class SignatureViewModel: ObservableObject {
    @Published var message: String = ""
    @Published var signature: String?

    func signMessage() {
        // TODO: CardCommunicationService.sign(message:)
        signature = "SignedDataBase64String=="
    }
}

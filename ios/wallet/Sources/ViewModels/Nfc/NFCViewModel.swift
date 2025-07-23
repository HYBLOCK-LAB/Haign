import Combine
import CoreNFC
import Foundation

// ViewModel that manages NFC scanning and APDU communication
final class NFCViewModel: NSObject, ObservableObject {
    @Published var isScanning: Bool = false
    @Published var responseData: Data?
    @Published var errorMessage: String?

    private var session: NFCTagReaderSession?
    private var pendingCommand: APDUCommand?

    // Starts an NFC scan and prepares to send the given APDU command
    func startScanning(command: APDUCommand) {
        guard NFCTagReaderSession.readingAvailable else {
            errorMessage = "NFC is not supported on this device."
            return
        }
        pendingCommand = command
        session = NFCTagReaderSession(pollingOption: .iso14443, delegate: self, queue: nil)
        session?.alertMessage = "Please tap your card."
        session?.begin()
        isScanning = true
    }

    // Stops the current NFC scanning session
    func stopScanning() {
        session?.invalidate()
        session = nil
        isScanning = false
    }
}

// MARK: - NFCTagReaderSessionDelegate

extension NFCViewModel: NFCTagReaderSessionDelegate {
    func tagReaderSessionDidBecomeActive(_: NFCTagReaderSession) {
        // Session became active
    }

    func tagReaderSession(_: NFCTagReaderSession, didInvalidateWithError error: Error) {
        DispatchQueue.main.async {
            self.isScanning = false
            self.errorMessage = error.localizedDescription
        }
    }

    func tagReaderSession(_ session: NFCTagReaderSession, didDetect tags: [NFCTag]) {
        guard let firstTag = tags.first else { return }
        session.connect(to: firstTag) { connectError in
            if let connectError = connectError {
                session.invalidate(errorMessage: "Failed to connect to tag: \(connectError.localizedDescription)")
                return
            }
            // Ensure this is an ISO7816 tag
            if case let .iso7816(isoTag) = firstTag {
                self.sendAPDU(to: isoTag)
            } else {
                session.invalidate(errorMessage: "Unsupported tag type.")
            }
        }
    }

    // Sends the pending APDU command to the connected ISO7816 tag
    private func sendAPDU(to tag: NFCISO7816Tag) {
        guard let command = pendingCommand else {
            session?.invalidate(errorMessage: "No APDU command to send.")
            return
        }

        let apduRequest = NFCISO7816APDU(
            instructionClass: command.cla,
            instructionCode: command.ins,
            p1Parameter: command.p1,
            p2Parameter: command.p2,
            data: command.data ?? Data(),
            expectedResponseLength: Int(command.le ?? 0)
        )

        tag.sendCommand(apdu: apduRequest) { response, sw1, sw2, error in
            DispatchQueue.main.async {
                self.isScanning = false
                if let error = error {
                    self.errorMessage = "APDU error: \(error.localizedDescription)"
                } else {
                    var fullResponse = response
                    fullResponse.append(contentsOf: [sw1, sw2])
                    self.responseData = fullResponse
                }
                self.session?.invalidate()
            }
        }
    }
}

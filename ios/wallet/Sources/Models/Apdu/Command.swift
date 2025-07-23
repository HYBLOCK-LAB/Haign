import Foundation

struct APDUCommand {
    let cla: UInt8
    let ins: UInt8
    let p1: UInt8
    let p2: UInt8
    let data: Data?
    let le: UInt8?

    var apdu: Data {
        var packet = Data([cla, ins, p1, p2])
        if let d = data {
            packet.append(UInt8(d.count))
            packet.append(d)
        }
        if let le = le {
            packet.append(le)
        }
        return packet
    }
}

extension APDUCommand {
    static func verifyPIN(_ pin: String) -> APDUCommand? {
        guard let pinData = Data(pin: pin) else { return nil }
        return APDUCommand(
            cla: 0x00,
            ins: 0x20,
            p1: 0x00,
            p2: 0x00,
            data: pinData,
            le: nil
        )
    }
}

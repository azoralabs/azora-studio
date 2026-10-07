// Rasterises the SVGs build-icons.mjs wrote into one atlas: each icon white on
// transparent in a square cell, row by row, in the order icons.azon lists them.
// Usage: swift build-icons.swift <icons.azon> <svg dir> <atlas.png> <index.azon>
import AppKit
import Foundation

let args = CommandLine.arguments
let list = try JSONSerialization.jsonObject(with: Data(contentsOf: URL(fileURLWithPath: args[1]))) as! [String: Any]
let names = list["icons"] as! [String]
let cell = list["cell"] as! Int
let columns = 16
let rows = (names.count + columns - 1) / columns
let width = columns * cell
let height = rows * cell
let space = CGColorSpaceCreateDeviceRGB()
let context = CGContext(data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: width * 4,
                        space: space, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
NSGraphicsContext.current = NSGraphicsContext(cgContext: context, flipped: false)
// A small inset keeps antialiased strokes off the cell edges.
let inset = CGFloat(cell) / 16
for (index, name) in names.enumerated() {
    guard let image = NSImage(contentsOfFile: "\(args[2])/\(name).svg") else {
        FileHandle.standardError.write(Data("cannot read \(name).svg\n".utf8))
        exit(1)
    }
    let column = index % columns
    let row = index / columns
    // CoreGraphics' origin is the bottom-left; rows are laid out from the top.
    let rect = CGRect(x: CGFloat(column * cell) + inset, y: CGFloat(height - (row + 1) * cell) + inset,
                      width: CGFloat(cell) - 2 * inset, height: CGFloat(cell) - 2 * inset)
    image.draw(in: rect)
}
let png = CGImageDestinationCreateWithURL(URL(fileURLWithPath: args[3]) as CFURL, "public.png" as CFString, 1, nil)!
CGImageDestinationAddImage(png, context.makeImage()!, nil)
CGImageDestinationFinalize(png)
let quoted = names.map { "\"\($0)\"" }.joined(separator: ", ")
let index = "{\n    \"cell\": \(cell),\n    \"columns\": \(columns),\n    \"width\": \(width),\n    \"height\": \(height),\n    \"icons\": [\(quoted)]\n}\n"
try Data(index.utf8).write(to: URL(fileURLWithPath: args[4]))
print("atlas \(width)x\(height), \(names.count) icons")

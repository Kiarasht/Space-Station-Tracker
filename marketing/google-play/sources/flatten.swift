import AppKit

// Writes an opaque RGB PNG (no alpha), optionally cropping first.
// Usage: swift flatten.swift in.png out.png [x y width height]
let a = CommandLine.arguments
guard a.count >= 3, let source = NSImage(contentsOfFile: a[1])?.cgImage(forProposedRect: nil, context: nil, hints: nil) else {
    fputs("usage: flatten.swift in.png out.png [x y w h]\n", stderr); exit(1)
}
var image = source
if a.count == 7, let v = Optional(a[3...6].compactMap { Int($0) }), v.count == 4 {
    guard let cropped = source.cropping(to: CGRect(x: v[0], y: v[1], width: v[2], height: v[3])) else { exit(1) }
    image = cropped
}
let w = image.width, h = image.height
guard let ctx = CGContext(data: nil, width: w, height: h, bitsPerComponent: 8, bytesPerRow: w * 4,
    space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue) else { exit(1) }
ctx.setFillColor(NSColor.black.cgColor); ctx.fill(CGRect(x: 0, y: 0, width: w, height: h))
ctx.draw(image, in: CGRect(x: 0, y: 0, width: w, height: h))
let rep = NSBitmapImageRep(cgImage: ctx.makeImage()!)
try rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: a[2]))
print("\(a[2]) \(w)x\(h)")

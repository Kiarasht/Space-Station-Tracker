import AppKit
import CoreImage

// Blurs rectangles (top-left pixel coordinates) in a capture so private information
// never appears in marketing art. Usage: swift redact.swift in.png out.png x,y,w,h [...]
let args = CommandLine.arguments
guard args.count >= 4, let image = CIImage(contentsOf: URL(fileURLWithPath: args[1])) else {
    fputs("usage: redact.swift in.png out.png x,y,w,h ...\n", stderr); exit(1)
}
let height = image.extent.height
var output = image
for spec in args.dropFirst(3) {
    let v = spec.split(separator: ",").compactMap { Double($0) }
    guard v.count == 4 else { continue }
    let rect = CGRect(x: v[0], y: height - v[1] - v[3], width: v[2], height: v[3])
    // A soft-edged mask so the blurred patch fades into the card around it.
    // Fully opaque inside the rect; only a narrow 6 px feather outside it.
    let blurred = image.clampedToExtent().applyingGaussianBlur(sigma: 40).cropped(to: image.extent)
    let mask = CIImage(color: .white).cropped(to: rect).applyingGaussianBlur(sigma: 6)
        .composited(over: CIImage(color: .black)).cropped(to: image.extent)
    output = blurred.applyingFilter("CIBlendWithMask", parameters: [kCIInputBackgroundImageKey: output, kCIInputMaskImageKey: mask])
}
let context = CIContext()
guard let cg = context.createCGImage(output, from: image.extent) else { exit(1) }
let rep = NSBitmapImageRep(cgImage: cg)
try rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: args[2]))
print("redacted \(args[2])")

import AppKit
import Foundation
let input = URL(fileURLWithPath: CommandLine.arguments[1])
let entries = try JSONSerialization.jsonObject(with: Data(contentsOf: input)) as! [[String: Any]]
for entry in entries {
    let width = 1100, height = 700
    let context = CGContext(data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: width * 4, space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    context.setFillColor(NSColor.white.cgColor); context.fill(CGRect(x: 0,y: 0,width: width,height: height))
    NSGraphicsContext.saveGraphicsState(); NSGraphicsContext.current = NSGraphicsContext(cgContext: context, flipped: false)
    func text(_ value: String, _ x: CGFloat, _ y: CGFloat, _ size: CGFloat) {
        (value as NSString).draw(at: NSPoint(x: x,y: y),withAttributes: [.font:NSFont.systemFont(ofSize:size),.foregroundColor:NSColor.black])
    }
    let title = entry["title"] as! String
    let labels = entry["labels"] as! [String]
    let years = entry["years"] as! [Int]
    let values = entry["values"] as! [[Int]]
    text(title, 80, 625, 27); text("Percentage (%)",80,590,17)
    let colors = [NSColor(calibratedRed:0.16,green:0.38,blue:0.77,alpha:1),NSColor(calibratedRed:0.1,green:0.59,blue:0.43,alpha:1)]
    for tick in stride(from:0,through:100,by:20) {
        let y = CGFloat(145 + tick * 4)
        context.setStrokeColor(NSColor.lightGray.cgColor);context.setLineWidth(1)
        context.move(to:CGPoint(x:100,y:y));context.addLine(to:CGPoint(x:1030,y:y));context.strokePath()
        text(String(tick),55,y-8,17)
    }
    for i in 0..<3 {
        let x = CGFloat(200 + i * 280)
        for j in 0..<2 {
            let bx = x + CGFloat(j * 78)
            context.setFillColor(colors[j].cgColor)
            context.fill(CGRect(x:bx,y:145,width:60,height:CGFloat(values[j][i]*4)))
            text(String(values[j][i]), bx+12, CGFloat(155+values[j][i]*4), 19)
        }
        text(labels[i],x-12,110,20)
    }
    for j in 0..<2 {
        context.setFillColor(colors[j].cgColor);context.fill(CGRect(x:250+j*240,y:65,width:22,height:22))
        text(String(years[j]),CGFloat(285+j*240),65,20)
    }
    text("Fictional data created for a course project; each year totals 100%.",100,25,15)
    NSGraphicsContext.restoreGraphicsState()
    let bitmap=NSBitmapImageRep(cgImage:context.makeImage()!)
    let png=bitmap.representation(using:.png,properties:[:])!
    try png.write(to:URL(fileURLWithPath:entry["output"] as! String))
}

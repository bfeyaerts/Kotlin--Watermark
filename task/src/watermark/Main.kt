package watermark

import java.awt.Color
import java.awt.Point
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    // Input and Watermark image
    val inputImage = getImage(ImageType.IMAGE) ?: return
    val watermarkImage = getImage(ImageType.WATERMARK) ?: return
    if ((inputImage.width < watermarkImage.width) || (inputImage.height < watermarkImage.height)) {
        println("The watermark's dimensions are larger.")
        return
    }

    // Transparency settings
    val useWatermarksAlphaChannel: Boolean
    val transparencyColor: Color?
    if (watermarkImage.colorModel.transparency == Transparency.TRANSLUCENT) {
        println("Do you want to use the watermark's Alpha channel?")
        val yesNo = YesNo.valueOfOrNull(readLine()!!)
        useWatermarksAlphaChannel = yesNo == YesNo.YES
        transparencyColor = null
    } else {
        useWatermarksAlphaChannel = false
        println("Do you want to set a transparency color?")
        val yesNo = YesNo.valueOfOrNull(readLine()!!)
        transparencyColor = if (yesNo == YesNo.YES) {
            getTransparencyColor() ?: return
        } else null
    }
    val weight = getWatermarkTransparencyPercentage() ?: return

    //
    println("Choose the position method (single, grid):")
    val positionMethod = PositionMethod.valueOfOrNull(readLine()!!) ?: return
    val inRange: (Point) -> Boolean
    val getWatermarkOffset: (Point) -> Point
    when (positionMethod) {
        PositionMethod.SINGLE -> {
            val diffX = inputImage.width - watermarkImage.width
            val diffY = inputImage.height - watermarkImage.height
            println("Input the watermark position ([x 0-$diffX] [y 0-$diffY]):")
            val position = getWatermarkPosition(diffX, diffY) ?: return
            val rangeX = position.x until  (position.x + watermarkImage.width)
            val rangeY = position.y until (position.y + watermarkImage.height)
            inRange = { it.x in rangeX && it.y in rangeY }
            getWatermarkOffset = { Point(it.x - position.x, it.y - position.y) }
        }
        PositionMethod.GRID -> {
            inRange = { true }
            getWatermarkOffset = { Point( it.x % watermarkImage.width, it.y % watermarkImage.height) }
        }
    }

    // Output image
    println("Input the output image filename (jpg or png extension):")
    val outputFilename = readLine()!!
    val outputExtension = OutputExtension.fromFilenameOrNull(outputFilename) ?: return
    val outputImage = BufferedImage(inputImage.width, inputImage.height, BufferedImage.TYPE_INT_RGB)

    val getWatermarkColor: (Point) -> Color
    val needsBlending: (Color) -> Boolean
    if (useWatermarksAlphaChannel) {
        getWatermarkColor = { point: Point -> Color(watermarkImage.getRGB(point), true) }
        needsBlending = { it.alpha == 255 }
    } else {
        getWatermarkColor = { point: Point -> Color(watermarkImage.getRGB(point))}
        needsBlending = { it != transparencyColor }
    }

    for (y in 0 until outputImage.height) {
        for (x in 0 until outputImage.width) {
            val i = Color(inputImage.getRGB(x, y))
            val point = Point(x, y)
            val color = if (inRange(point)) {
                val watermarkOffset = getWatermarkOffset(point)
                val w = getWatermarkColor(watermarkOffset)
                if (needsBlending(w)) Color(
                    (weight * w.red + (100 - weight) * i.red) / 100,
                    (weight * w.green + (100 - weight) * i.green) / 100,
                    (weight * w.blue + (100 - weight) * i.blue) / 100
                ) else i
            } else i
            outputImage.setRGB(x, y, color.rgb)
        }
    }

    ImageIO.write(outputImage, outputExtension.name.lowercase(), File(outputFilename))
    println("The watermarked image $outputFilename has been created.")
}

fun BufferedImage.getRGB(point: Point) = try {
    this.getRGB(point.x, point.y)
} catch (e: ArrayIndexOutOfBoundsException) {
    println("Coordinates $point are out of range (0..${this.width}, 0..${this.height})")
    throw e
}

enum class ImageType(val description: String) {
    IMAGE("image"),
    WATERMARK("watermark image")
}

enum class OutputExtension {
    JPG,
    PNG;
    companion object {
        fun fromFilenameOrNull(filename: String): OutputExtension? {
            for (e in values()) {
                if (filename.endsWith(".${e.name.lowercase()}")) {
                    return e
                }
            }
            println("The output file extension isn't \"jpg\" or \"png\".")
            return null
        }
    }
}

enum class YesNo {
    YES,
    NO;
    companion object {
        fun valueOfOrNull(string: String): YesNo? {
            for (e in values()) {
                if (string.uppercase() == e.name) {
                    return e
                }
            }
            return null
        }
    }
}

enum class PositionMethod {
    SINGLE,
    GRID;
    companion object {
        fun valueOfOrNull(string: String): PositionMethod? {
            for (e in values()) {
                if (string.uppercase() == e.name) {
                    return e
                }
            }
            println("The position method input is invalid.")
            return null
        }
    }
}

fun getWatermarkPosition(diffX: Int, diffY: Int): Point? {
    try {
        val coordinates = readLine()!!.split(" ").map { it.toInt() }
        require(coordinates.size == 2)
        val (x, y) = coordinates
        require(x in 0 .. diffX)
        require(y in 0 .. diffY)
        return Point(x, y)
    } catch (e: NumberFormatException) {
        println("The position input is invalid.")
    } catch (e: IllegalArgumentException) {
        println("The position input is out of range.")
    }
    return null
}

fun getTransparencyColor(): Color? {
    println("Input a transparency color ([Red] [Green] [Blue]):")
    return try {
        val colors = readLine()!!.split(" ").map { it.toInt() }
        require(colors.size == 3)
        val (r, g, b) = colors
        require(r in 0 .. 255)
        require(g in 0 .. 255)
        require(b in 0 .. 255)
        Color(r, g, b)
    } catch (e: Exception) {
        println("The transparency color input is invalid.")
        null
    }
}

fun getWatermarkTransparencyPercentage(): Int? {
    println("Input the watermark transparency percentage (Integer 0-100):")
    val weight: Int
    try {
        weight = readLine()!!.toInt()
    } catch (e: NumberFormatException) {
        println("The transparency percentage isn't an integer number.")
        return null
    }
    if (weight !in 0..100) {
        println("The transparency percentage is out of range.")
        return null
    }
    return weight
}

fun getImage(imageType: ImageType) : BufferedImage? {
    println("Input the ${imageType.description} filename:")
    val filename = readLine()!!
    val file = File(filename)
    if (!file.exists()) {
        println("The file $filename doesn't exist.")
        return null
    }
    val image = ImageIO.read(file)
    val colorModel = image.colorModel
    if (colorModel.numColorComponents != 3) {
        println("The number of ${imageType.name.lowercase()} color components isn't 3.")
        return null
    }
    if (colorModel.pixelSize !in arrayOf(24, 32)) {
        println("The ${imageType.name.lowercase()} isn't 24 or 32-bit.")
        return null
    }
    return image
}
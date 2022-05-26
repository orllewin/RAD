package orllewin.rad

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint


class RadImage(private val radImageStr: String) {

    private val black = Paint().apply {
        color = Color.BLACK
        isAntiAlias = false
    }

    private val white = Paint().apply{
        color = Color.WHITE
        isAntiAlias = false
    }

    fun build16x16(): Bitmap{
        val bitmap = Bitmap.createBitmap(16, 16,  Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawRect(0f, 0f, 16f, 16f, black)
        radImageStr.toCharArray().forEachIndexed { index, c ->
            when (c) {
                'X' -> canvas.drawPoint((index % 16).toFloat(), ((index / 16) % 16).toFloat(), white)
            }
        }
        canvas.drawPoint(8f, 8f, white)

        return bitmap
    }

    fun build256Circles(backgroundColour: Int? = null): Bitmap {
        println("RADIMG: build256Circles() from $radImageStr")
        val sections = radImageStr.split(":")
        val dimens = sections[0].split("x")
        val sourceWidth = dimens[0].toInt()
        val sourceHeight = dimens[1].toInt()
        println("RADIMG: sourceWidth $sourceWidth sourceHeight: $sourceHeight")

        if(sourceWidth != 16 || sourceHeight != 16){
            throw Exception("build256Circles() is only compatible with 16x16 RAD Images")
        }

        val rawColours = sections[1].split("#")

        val colours = mutableListOf<String>()

        rawColours.forEach { rawColour ->
            if(rawColour.length == 1){
                //f -> #ffffff
                colours.add("#${rawColour.repeat(6)}")
            }else if(rawColour.length == 3){
                //f0c -> #ff00cc
                val chars = rawColour.toCharArray()
                colours.add("#${chars[0].toString().repeat(2)}${chars[1].toString().repeat(2)}${chars[2].toString().repeat(2)}")
            }else if(rawColour.length == 6){
                //ff00cc -> #ff00cc
                colours.add("#$rawColour")
            }
        }

        println("RADIMG: colours: ${colours.joinToString()}")

        val imageData = sections[2].split("|")

        val bitmap = Bitmap.createBitmap(256, 256,  Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = false
        }

        backgroundColour?.let{
            paint.color = backgroundColour
        } ?: run {
            paint.color = Color.parseColor(colours.first())
        }

        canvas.drawRect(0f, 0f, 255f, 255f, paint)

        var x = 0
        var y = 0
        imageData.forEach { pixelData ->
            println("RADIMG: processing pixelData: $pixelData")

            if(pixelData.contains(".")){
                //is range
                val rangeData = pixelData.split(".")
                val dictIndex = rangeData[0].toInt()
                val pixelCount = rangeData[1].toInt()
                println("RADIMG: drawing range $pixelData")
                paint.color = Color.parseColor(colours[dictIndex])
                repeat(pixelCount){
                    canvas.drawCircle((x * 16).toFloat() + 8f, (y * 16).toFloat() + 8f, 8f, paint)
                    x++

                    if(x == 16){
                        x = 0
                        y++
                    }
                }

            }else{
                //dictionary refs
                println("RADIMG: drawing dictionary indexes $pixelData")

                pixelData.toCharArray().forEach { c ->
                    paint.color = Color.parseColor(colours[c.toString().toInt()])
                    canvas.drawCircle((x * 16).toFloat() + 8f, (y * 16).toFloat() + 8f, 8f, paint)
                    x++
                    if(x == 16){
                        x = 0
                        y++
                    }
                }
            }
        }

        return bitmap

    }

    fun build256CircleMatrix(): Bitmap{
        val bitmap = Bitmap.createBitmap(256, 256,  Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        canvas.drawRect(0f, 0f, 256f, 256f, black)
        radImageStr.toCharArray().forEachIndexed { index, c ->

            val x = ((index % 16) * 16).toFloat()
            val y = (((index / 16) % 16) * 16).toFloat()

            if(c == 'X'){
                canvas.drawCircle(x + 8, y + 8, 8f, white)
            }
        }

        return bitmap
    }

    fun build256SquareMatrix(): Bitmap{
        val small = build16x16()
        return Bitmap.createScaledBitmap(small, 256, 256, false)
    }
}
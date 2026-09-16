package ru.unet_app.unet.domain.usecases

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.unet_app.model.Tile
import kotlin.math.max
import kotlin.math.min

@RunWith(RobolectricTestRunner::class)
class SplitImageIntoTilesUseCaseTest {


    private val useCase = SplitImageIntoTilesUseCase()

    @Test
    fun testExactTileSize() {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val tiles = useCase(bitmap)

        assertEquals(1, tiles.size)
        assertEquals(0, tiles[0].startX)
        assertEquals(0, tiles[0].startY)
        assertEquals(256, tiles[0].endX)
        assertEquals(256, tiles[0].endY)
    }

    @Test
    fun testHorizontalSplit() {
        val bitmap = Bitmap.createBitmap(512, 256, Bitmap.Config.ARGB_8888)
        val tiles = useCase(bitmap)

        assertEquals(3, tiles.size)
        assertEquals(0, tiles[0].startX)
        assertEquals(256, tiles[0].endX)
        assertEquals(192, tiles[1].startX)
        assertEquals(448, tiles[1].endX)
        assertEquals(256, tiles[2].startX)
        assertEquals(512, tiles[2].endX)
    }

    @Test
    fun testVerticalSplit() {
        val bitmap = Bitmap.createBitmap(256, 512, Bitmap.Config.ARGB_8888)
        val tiles = useCase(bitmap)

        assertEquals(3, tiles.size)
        assertEquals(0, tiles[0].startY)
        assertEquals(256, tiles[0].endY)
        assertEquals(192, tiles[1].startY)
        assertEquals(448, tiles[1].endY)
        assertEquals(256, tiles[2].startY)
        assertEquals(512, tiles[2].endY)
    }

    @Test
    fun testGridSplit() {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val tiles = useCase(bitmap)

        assertEquals(9, tiles.size)
    }

    @Test
    fun testOverlapHandling() {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val tiles = useCase(bitmap)

        assertNotNull(tiles)
        assertTrue(tiles.size > 1)
    }

    @Test
    fun testNonMultipleOfTileSize() {
        val bitmap = Bitmap.createBitmap(1000, 800, Bitmap.Config.ARGB_8888)
        val tiles = useCase(bitmap)

        assertNotNull(tiles)
        assertEquals(1000, tiles.maxByOrNull { it.endX }?.endX)
        assertEquals(800, tiles.maxByOrNull { it.endY }?.endY)
    }

    @Test
    fun testSmallImage() {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val tiles = useCase(bitmap)

        assertEquals(1, tiles.size)
        assertEquals(0, tiles[0].startX)
        assertEquals(0, tiles[0].startY)
        assertEquals(256, tiles[0].endX)
        assertEquals(256, tiles[0].endY)
    }


    @Test
    fun testAllTilesHaveCorrectSize() {
        val bitmap = Bitmap.createBitmap(1000, 800, Bitmap.Config.ARGB_8888)
        val tiles = useCase(bitmap)

        for (tile in tiles) {
            val width = tile.endX - tile.startX
            val height = tile.endY - tile.startY
            assertEquals(SplitImageIntoTilesUseCase.TILE_SIZE, width)
            assertEquals(SplitImageIntoTilesUseCase.TILE_SIZE, height)
        }
    }

    @Test
    fun testTileBitmapNotNull() {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val tiles = useCase(bitmap)

        for (tile in tiles) {
            assertNotNull(tile.bitmap)
            assertEquals(256, tile.bitmap.width)
            assertEquals(256, tile.bitmap.height)
        }
    }
}
package ru.unet_app.transparency_settings.presentation

import android.graphics.Bitmap
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.ViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import ru.unet_app.datastore.domain.repository.ImageRepository
import ru.unet_app.model.InferenceMetadata
import ru.unet_app.model.PredictedClasses
import ru.unet_app.model.TransparencyState
import ru.unet_app.transparency_settings.domain.repository.TransparencyImageProcRepository
import java.io.File
import java.util.ArrayList

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TransparencyViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val imageProcessor = mockk<TransparencyImageProcRepository>()
    private val imageRepository = mockk<ImageRepository>()
    private val viewModel = TransparencyViewModel(imageProcessor, imageRepository)

    private val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    private val classMasks = PredictedClasses.entries.associateWith { testBitmap }.toMutableMap()
    private val resultPath = "/test/path"

    @org.junit.Before
    fun setup() {
        every { imageRepository.loadMetadata(any()) } returns InferenceMetadata(100, 100, 100L, 200L)
        every { imageProcessor.unitedMask(any(), any()) } returns testBitmap
    }

    @Test
    fun testSetupDataWithBitmapArray() = runTest {
        val bitmapArray = ArrayList<Bitmap>().apply {
            repeat(6) { add(testBitmap) }
        }

        viewModel.setupData(null, bitmapArray, resultPath)

        assertEquals(TransparencyState(), viewModel.transparencyState.value)
        assertEquals(InferenceMetadata(100, 100, 100L, 200L), viewModel.metadata.value)

        val preview = viewModel.getPreviewImage()
        assertNotNull(preview)
    }

    @Test
    fun testSetupDataWithBitmapPaths() = runTest {
        val paths = ArrayList<String>().apply {
            repeat(6) { add("/test/path/image_$it.png") }
        }

        every { imageRepository.loadMetadata(any()) } returns null

        // This will fail because files don't exist - testing the require
        try {
            viewModel.setupData(paths, null, resultPath)
            fail("Expected exception")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Файл изображения не найден"))
        }
    }

    @Test
    fun testUpdateTransparencyMitochondria() = runTest {
        val bitmapArray = ArrayList<Bitmap>().apply { repeat(6) { add(testBitmap) } }
        viewModel.setupData(null, bitmapArray, resultPath)

        viewModel.updateTransparency(PredictedClasses.MITOCHONDRIA, 50)

        val state = viewModel.transparencyState.value
        assertEquals(0.5f, state.mitochondria, 0.001f)
        assertEquals(0.7f, state.psd, 0.001f)
    }

    @Test
    fun testUpdateTransparencyPSD() = runTest {
        val bitmapArray = ArrayList<Bitmap>().apply { repeat(6) { add(testBitmap) } }
        viewModel.setupData(null, bitmapArray, resultPath)

        viewModel.updateTransparency(PredictedClasses.PSD, 80)

        val state = viewModel.transparencyState.value
        assertEquals(0.3f, state.mitochondria, 0.001f)
        assertEquals(0.8f, state.psd, 0.001f)
    }

    @Test
    fun testUpdateTransparencyAllClasses() = runTest {
        val bitmapArray = ArrayList<Bitmap>().apply { repeat(6) { add(testBitmap) } }
        viewModel.setupData(null, bitmapArray, resultPath)

        viewModel.updateTransparency(PredictedClasses.MITOCHONDRIA, 10)
        viewModel.updateTransparency(PredictedClasses.PSD, 20)
        viewModel.updateTransparency(PredictedClasses.VESICLES, 30)
        viewModel.updateTransparency(PredictedClasses.AXON, 40)
        viewModel.updateTransparency(PredictedClasses.BOUNDARIES, 50)
        viewModel.updateTransparency(PredictedClasses.MITOCHONDRIAL_BOUNDARIES, 60)

        val state = viewModel.transparencyState.value
        assertEquals(0.1f, state.mitochondria, 0.001f)
        assertEquals(0.2f, state.psd, 0.001f)
        assertEquals(0.3f, state.vesicles, 0.001f)
        assertEquals(0.4f, state.axon, 0.001f)
        assertEquals(0.5f, state.boundaries, 0.001f)
        assertEquals(0.6f, state.mitochondrialBoundaries, 0.001f)
    }

    @Test
    fun testGetPreviewImageCallsProcessor() = runTest {
        val bitmapArray = ArrayList<Bitmap>().apply { repeat(6) { add(testBitmap) } }
        val expectedPreview = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        coEvery { imageProcessor.unitedMask(any(), any()) } returns expectedPreview
        viewModel.setupData(null, bitmapArray, resultPath)

        val preview = viewModel.getPreviewImage()

        assertEquals(expectedPreview, preview)
        verify { imageProcessor.unitedMask(any(), any()) }
    }

    @Test
    fun testSavePreviewImage() = runTest {
        val bitmapArray = ArrayList<Bitmap>().apply { repeat(6) { add(testBitmap) } }
        val previewBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        coEvery { imageProcessor.unitedMask(any(), any()) } returns previewBitmap
        coEvery { imageRepository.saveImage(any(), any(), any()) } returns true
        viewModel.setupData(null, bitmapArray, resultPath)

        val result = viewModel.savePreviewImage()

        assertTrue(result)
        verify { imageRepository.saveImage(previewBitmap, File(resultPath), any()) }
    }

    @Test
    fun testSavePreviewImageFailure() = runTest {
        val bitmapArray = ArrayList<Bitmap>().apply { repeat(6) { add(testBitmap) } }
        val previewBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        coEvery { imageProcessor.unitedMask(any(), any()) } returns previewBitmap
        coEvery { imageRepository.saveImage(any(), any(), any()) } returns false
        viewModel.setupData(null, bitmapArray, resultPath)

        val result = viewModel.savePreviewImage()

        assertTrue(!result)
    }

    @Test
    fun testGenerateUniqueFileName() = runTest {
        val bitmapArray = ArrayList<Bitmap>().apply { repeat(6) { add(testBitmap) } }
        viewModel.setupData(null, bitmapArray, resultPath)

        // Test via reflection or by checking the file name generation logic
        // Since generateUniqueFileName is private, we test savePreviewImage which uses it

        coEvery { imageProcessor.unitedMask(any(), any()) } returns testBitmap
        coEvery { imageRepository.saveImage(any(), any(), any()) } returns true

        viewModel.savePreviewImage()

        verify { imageRepository.saveImage(any(), any(), any()) }
    }

    @Test
    fun testDefaultTransparencyState() = runTest {
        val bitmapArray = ArrayList<Bitmap>().apply { repeat(6) { add(testBitmap) } }
        viewModel.setupData(null, bitmapArray, resultPath)

        val state = viewModel.transparencyState.value
        assertEquals(0.3f, state.mitochondria, 0.001f)
        assertEquals(0.7f, state.psd, 0.001f)
        assertEquals(0.6f, state.vesicles, 0.001f)
        assertEquals(0.8f, state.axon, 0.001f)
        assertEquals(0.75f, state.boundaries, 0.001f)
        assertEquals(0.45f, state.mitochondrialBoundaries, 0.001f)
    }
}
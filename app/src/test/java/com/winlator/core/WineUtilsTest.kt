package com.winlator.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.winlator.container.Container
import com.winlator.xenvironment.ImageFs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.io.path.createTempDirectory

@RunWith(RobolectricTestRunner::class)
class WineUtilsTest {
    private lateinit var context: Context
    private lateinit var container: Container
    private lateinit var rootDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        container = mockk(relaxed = true)
        rootDir = createTempDirectory(prefix = "wine-utils-test").toFile()
        File(rootDir, ".wine/dosdevices").mkdirs()
    }

    @After
    fun tearDown() {
        unmockkAll()
        rootDir.deleteRecursively()
    }

    @Test
    fun createDosdevicesSymlinks_usesCanonicalImageFsRootForZDrive() {
        val imageFs = mockk<ImageFs>()
        val nonCanonicalRoot = File(rootDir, "imagefs/../imagefs-real")
        val canonicalRoot = nonCanonicalRoot.absoluteFile.canonicalPath
        val expectedDosdevicesPath = File(rootDir, ".wine/dosdevices").path

        every { container.rootDir } returns rootDir
        every { container.drives } returns "D:/downloads;E:/storage"
        every { container.drivesIterator() } returns emptyList<Array<String>>()

        mockkStatic(ImageFs::class)
        every { ImageFs.find(context) } returns imageFs
        every { imageFs.rootDir } returns nonCanonicalRoot

        mockkStatic(FileUtils::class)
        every { FileUtils.symlink(any<String>(), any<String>()) } just runs

        WineUtils.createDosdevicesSymlinks(context, container)

        verify { FileUtils.symlink("../drive_c", "$expectedDosdevicesPath/c:") }
        verify { FileUtils.symlink(canonicalRoot, "$expectedDosdevicesPath/z:") }
    }

    @Test
    fun normalizeManagedDrives_migratesLegacyDownloadRootToGameNativeDirectory() {
        val legacy = Container.LEGACY_DOWNLOADS_DRIVE_DIR.absolutePath
        val isolated = Container.GAMENATIVE_DOWNLOADS_DRIVE_DIR.absolutePath
        val current = "D:$legacy" + "E:/data/data/app.gamenative.joycontest/storage"

        val normalized = WineUtils.normalizeManagedDrives(current)

        assertEquals("D:$isolated" + "E:/data/data/app.gamenative.joycontest/storage", normalized)
    }

    @Test
    fun normalizeManagedDrives_preservesCustomDDrive() {
        val current = "D:/storage/emulated/0/MyGames" + "E:/data/data/app.gamenative.joycontest/storage"

        assertEquals(current, WineUtils.normalizeManagedDrives(current))
    }

    @Test
    fun normalizeManagedDrives_isIdempotentForIsolatedDownloadDirectory() {
        val isolated = Container.GAMENATIVE_DOWNLOADS_DRIVE_DIR.absolutePath
        val current = "D:$isolated" + "E:/data/data/app.gamenative.joycontest/storage"

        assertEquals(current, WineUtils.normalizeManagedDrives(current))
    }

    @Test
    fun defaultDrives_isolatesWineFilesUnderGameNativeDownloadDirectory() {
        val isolated = Container.GAMENATIVE_DOWNLOADS_DRIVE_DIR.absolutePath

        assertEquals(true, Container.DEFAULT_DRIVES.startsWith("D:$isolated"))
    }
}

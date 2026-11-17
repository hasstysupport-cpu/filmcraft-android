package studio.vantacut.app.engine

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A font the user can download: an SIL OFL family from Google Fonts' open repository. */
data class FontEntry(val family: String, val path: String, val arabic: Boolean = false) {
    val url: String get() = "https://raw.githubusercontent.com/google/fonts/main/ofl/$path"
}

/** The fonts the downloader offers, by what they are good at (all SIL OFL 1.1). */
object FontCatalog {
    val FONTS = listOf(
        // Arabic
        FontEntry("Almarai", "almarai/Almarai-Regular.ttf", arabic = true),
        FontEntry("Almarai Bold", "almarai/Almarai-Bold.ttf", arabic = true),
        FontEntry("Changa", "changa/Changa[wght].ttf", arabic = true),
        FontEntry("El Messiri", "elmessiri/ElMessiri[wght].ttf", arabic = true),
        FontEntry("IBM Plex Sans Arabic", "ibmplexsansarabic/IBMPlexSansArabic-Regular.ttf", arabic = true),
        FontEntry("Lateef", "lateef/Lateef-Regular.ttf", arabic = true),
        FontEntry("Noto Kufi Arabic", "notokufiarabic/NotoKufiArabic[wght].ttf", arabic = true),
        FontEntry("Noto Naskh Arabic", "notonaskharabic/NotoNaskhArabic[wght].ttf", arabic = true),
        FontEntry("Readex Pro", "readexpro/ReadexPro[HEXP,wght].ttf", arabic = true),
        FontEntry("Tajawal", "tajawal/Tajawal-Regular.ttf", arabic = true),
        FontEntry("Tajawal Bold", "tajawal/Tajawal-Bold.ttf", arabic = true),
        // Sans
        FontEntry("Barlow", "barlow/Barlow-Regular.ttf"),
        FontEntry("DM Sans", "dmsans/DMSans[opsz,wght].ttf"),
        FontEntry("Figtree", "figtree/Figtree[wght].ttf"),
        FontEntry("Kanit", "kanit/Kanit-Regular.ttf"),
        FontEntry("Karla", "karla/Karla[wght].ttf"),
        FontEntry("Lato", "lato/Lato-Regular.ttf"),
        FontEntry("Lato Bold", "lato/Lato-Bold.ttf"),
        FontEntry("Lexend", "lexend/Lexend[wght].ttf"),
        FontEntry("Manrope", "manrope/Manrope[wght].ttf"),
        FontEntry("Montserrat", "montserrat/Montserrat[wght].ttf"),
        FontEntry("Nunito", "nunito/Nunito[wght].ttf"),
        FontEntry("Open Sans", "opensans/OpenSans[wdth,wght].ttf"),
        FontEntry("Outfit", "outfit/Outfit[wght].ttf"),
        FontEntry("Poppins", "poppins/Poppins-Regular.ttf"),
        FontEntry("Poppins Bold", "poppins/Poppins-Bold.ttf"),
        FontEntry("Raleway", "raleway/Raleway[wght].ttf"),
        FontEntry("Rubik", "rubik/Rubik[wght].ttf"),
        FontEntry("Source Sans 3", "sourcesans3/SourceSans3[wght].ttf"),
        FontEntry("Urbanist", "urbanist/Urbanist[wght].ttf"),
        FontEntry("Work Sans", "worksans/WorkSans[wght].ttf"),
        // Serif and display
        FontEntry("Abril Fatface", "abrilfatface/AbrilFatface-Regular.ttf"),
        FontEntry("Acme", "acme/Acme-Regular.ttf"),
        FontEntry("Anton", "anton/Anton-Regular.ttf"),
        FontEntry("Archivo", "archivo/Archivo[wdth,wght].ttf"),
        FontEntry("Bebas Neue", "bebasneue/BebasNeue-Regular.ttf"),
        FontEntry("Bungee", "bungee/Bungee-Regular.ttf"),
        FontEntry("Caveat", "caveat/Caveat[wght].ttf"),
        FontEntry("Cinzel", "cinzel/Cinzel[wght].ttf"),
        FontEntry("Comfortaa", "comfortaa/Comfortaa[wght].ttf"),
        FontEntry("Courier Prime", "courierprime/CourierPrime-Regular.ttf"),
        FontEntry("Dancing Script", "dancingscript/DancingScript[wght].ttf"),
        FontEntry("Lora", "lora/Lora[wght].ttf"),
        FontEntry("Lobster", "lobster/Lobster-Regular.ttf"),
        FontEntry("Pacifico", "pacifico/Pacifico-Regular.ttf"),
        FontEntry("Playfair Display", "playfairdisplay/PlayfairDisplay[wght].ttf"),
        FontEntry("PT Serif", "ptserif/PT_Serif-Web-Regular.ttf"),
        FontEntry("Quicksand", "quicksand/Quicksand[wght].ttf"),
        FontEntry("Righteous", "righteous/Righteous-Regular.ttf"),
        FontEntry("Space Mono", "spacemono/SpaceMono-Regular.ttf"),
        FontEntry("Titan One", "titanone/TitanOne-Regular.ttf"),
        FontEntry("Varela Round", "varelaround/VarelaRound-Regular.ttf"),
    )
}

/**
 * Downloads the fonts the user asked for, from Google Fonts' open repository (raw GitHub, no
 * account, no key), into the app's fonts folder, and hands each file to the engine to register.
 * One download at a time.
 */
object FontDownloads {
    /** A download in progress (or just failed: [error]). */
    data class State(val family: String, val done: Long, val total: Long, val error: String? = null) {
        val fraction: Float get() = if (total > 0) (done.toDouble() / total).toFloat().coerceIn(0f, 1f) else 0f
    }

    var state by mutableStateOf<State?>(null)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    val busy: Boolean get() = state?.let { it.error == null } == true

    /** Where the downloaded fonts live. */
    fun fontsDir(ctx: Context): File = File(ctx.filesDir, "fonts").apply { mkdirs() }

    /** The families already downloaded. */
    fun installed(ctx: Context): List<String> =
        fontsDir(ctx).listFiles { f -> f.isFile && f.name.endsWith(".ttf") }?.map { it.name.removeSuffix(".ttf") }.orEmpty().sorted()

    /** The downloaded font files, ready to register with the engine. */
    fun files(ctx: Context): List<File> = fontsDir(ctx).listFiles { f -> f.isFile && f.name.endsWith(".ttf") }.orEmpty().sortedBy { it.name }

    /** Download [entry]; [onRegistered] runs on a background thread once the engine has the font. */
    fun start(ctx: Context, entry: FontEntry, onRegistered: () -> Unit = {}) {
        if (busy) return
        state = State(entry.family, 0, 0)
        job = scope.launch {
            try {
                val target = download(ctx, entry)
                register(ctx, target)
                state = null
                onRegistered()
            } catch (e: kotlinx.coroutines.CancellationException) {
                state = null
                throw e
            } catch (e: Exception) {
                state = state?.copy(error = e.message?.takeIf { it.isNotBlank() } ?: "The download failed.")
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        state = null
    }

    /** Forget a failed download's message. */
    fun dismiss() {
        if (state?.error != null) state = null
    }

    /** Delete a downloaded family's file; it leaves the engine on the next start. */
    fun delete(ctx: Context, family: String): Boolean = File(fontsDir(ctx), "$family.ttf").delete()

    private suspend fun download(ctx: Context, entry: FontEntry): File = withContext(Dispatchers.IO) {
        val target = File(fontsDir(ctx), "${entry.family}.ttf")
        val part = File(fontsDir(ctx), "${entry.family}.ttf.part")
        var done = part.length()
        val conn = (URL(entry.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            if (done > 0) setRequestProperty("Range", "bytes=$done-")
        }
        try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                throw IOException("The download failed (HTTP $code). Try again later.")
            }
            val total = conn.contentLengthLong.takeIf { it > 0 }?.let { it + done } ?: 0L
            state = state?.copy(done = done, total = total)
            conn.inputStream.use { input ->
                FileOutputStream(part, done > 0 && code == HttpURLConnection.HTTP_PARTIAL).use { out ->
                    val buf = ByteArray(1 shl 16)
                    var reported = done
                    while (true) {
                        ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        // a state write recomposes the screen: report in 256 KB steps, not per chunk
                        if (done - reported >= (1 shl 18)) {
                            reported = done
                            state = state?.copy(done = done)
                        }
                    }
                    state = state?.copy(done = done)
                }
            }
        } finally {
            conn.disconnect()
        }
        // a font file starts with a real sfnt magic number
        val head = part.inputStream().use { it.readNBytes(4) }
        val magic = head.toList().joinToString(" ") { "%02x".format(it) }
        if (part.length() < 1000 || (magic != "00 01 00 00" && magic != "4f 54 54 4f" && magic != "74 72 75 65")) {
            part.delete()
            throw IOException("${entry.family} didn't download correctly. Try again.")
        }
        if (!part.renameTo(target)) throw IOException("${entry.family} couldn't be saved.")
        target
    }

    /** Give the engine the font so it can be used right away (the engine re-reads on start). */
    private suspend fun register(ctx: Context, file: File) = withContext(Dispatchers.IO) {
        val app = ctx.applicationContext as? studio.vantacut.app.VantaCutApplication ?: return@withContext
        val engine = runCatching { app.engine.await().getOrNull() }.getOrNull() ?: return@withContext
        runCatching { engine.command { addFontBytes(file.readBytes()) } }
    }
}

package com.anigoose.app

import android.content.Context
import android.system.Os
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Unpacks the bundled minimal Linux userland (Termux-style bootstrap: bash,
 * coreutils, curl, grep, sed, fzf, mpv — all cross-compiled against Android's
 * bionic libc) into the app's private storage on first launch.
 *
 * The zip itself is NOT built by this project — see /bootstrap/README.md for
 * how to produce bootstrap-<abi>.zip from Termux's packaging scripts. This
 * class only knows how to install one that's already sitting in assets/.
 */
object BootstrapInstaller {

    fun prefixDir(context: Context): File = File(context.filesDir, "usr")

    fun isInstalled(context: Context): Boolean =
        File(prefixDir(context), ".installed").exists()

    /** Runs on a background thread. Throws on failure. */
    fun install(context: Context) {
        val prefix = prefixDir(context)
        if (isInstalled(context)) return

        prefix.mkdirs()

        val abi = preferredAbi()
        val assetName = "bootstrap-$abi.zip"

        context.assets.open(assetName).use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                val buffer = ByteArray(8192)
                while (entry != null) {
                    val outFile = File(prefix, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { out ->
                            var read: Int
                            while (zip.read(buffer).also { read = it } != -1) {
                                out.write(buffer, 0, read)
                            }
                        }
                        // Executables in bin/ and lib*/ need +x; bionic won't
                        // exec them otherwise once extracted from the zip.
                        if (outFile.path.contains("/bin/") || outFile.path.contains("/lib")) {
                            Os.chmod(outFile.absolutePath, 0b111_101_101) // 0755
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        // ani-cli itself + our launcher wrapper are dropped in as plain assets
        // (small text files), not part of the compiled bootstrap zip, so they
        // stay easy to update independently of the rootfs.
        copyAsset(context, "ani-cli", File(prefix, "bin/ani-cli"))
        Os.chmod(File(prefix, "bin/ani-cli").absolutePath, 0b111_101_101)

        File(prefix, ".installed").writeText("ok")
    }

    private fun copyAsset(context: Context, assetName: String, dest: File) {
        dest.parentFile?.mkdirs()
        context.assets.open(assetName).use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun preferredAbi(): String {
        for (abi in android.os.Build.SUPPORTED_ABIS) {
            when (abi) {
                "arm64-v8a", "armeabi-v7a", "x86_64" -> return abi
            }
        }
        throw IllegalStateException("No supported ABI bootstrap for ${android.os.Build.SUPPORTED_ABIS.joinToString()}")
    }
}

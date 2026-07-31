package com.anigoose.app

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import kotlin.concurrent.thread

/**
 * The entire app UI. There is no menu, no other screen, no way to open a
 * plain shell: on launch we install the bootstrap if needed, then start a
 * single TerminalSession whose command IS ani-cli. When ani-cli exits, we
 * either relaunch it or finish() — never drop the user into a bare prompt.
 */
class TerminalActivity : AppCompatActivity(), TerminalSessionClient {

    private lateinit var terminalView: TerminalView
    private lateinit var statusText: TextView
    private var session: TerminalSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_terminal)

        terminalView = findViewById(R.id.terminal_view)
        statusText = findViewById(R.id.status_text)

        terminalView.setTerminalViewClient(object : TerminalViewClient {
            override fun onScale(scale: Float) = 1.0f
            override fun onSingleTapUp(e: android.view.MotionEvent?) {
                terminalView.requestFocus()
                showSoftKeyboard()
            }
            override fun shouldBackButtonBeMappedToEscape() = false
            override fun shouldEnforceCharBasedInput() = true
            override fun shouldUseCtrlSpaceWorkaround() = false
            override fun isTerminalViewSelected() = true
            override fun copyModeChanged(copyMode: Boolean) {}
            override fun onKeyDown(keyCode: Int, e: android.view.KeyEvent?, session: TerminalSession?) = false
            override fun onKeyUp(keyCode: Int, e: android.view.KeyEvent?) = false
            override fun readControlKey() = false
            override fun readAltKey() = false
            override fun readShiftKey() = false
            override fun readFnKey() = false
            override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?) = false
            override fun onEmulatorSet() {}
            override fun logError(tag: String?, message: String?) {}
            override fun logWarn(tag: String?, message: String?) {}
            override fun logInfo(tag: String?, message: String?) {}
            override fun logDebug(tag: String?, message: String?) {}
            override fun logVerbose(tag: String?, message: String?) {}
            override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
            override fun logStackTrace(tag: String?, e: Exception?) {}
        })

        ensureBootstrapThenStart()
    }

    private fun ensureBootstrapThenStart() {
        if (BootstrapInstaller.isInstalled(this)) {
            startAniCliSession()
            return
        }
        statusText.text = getString(R.string.installing_message)
        thread {
            try {
                BootstrapInstaller.install(this)
                runOnUiThread { startAniCliSession() }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = getString(R.string.install_failed, e.message ?: "unknown error")
                }
            }
        }
    }

    private fun startAniCliSession() {
        statusText.visibility = android.view.View.GONE
        terminalView.visibility = android.view.View.VISIBLE

        val prefix = BootstrapInstaller.prefixDir(this).absolutePath
        val home = File(prefix, "home").apply { mkdirs() }.absolutePath

        val env = arrayOf(
            "HOME=$home",
            "PREFIX=$prefix",
            "PATH=$prefix/bin",
            "LD_LIBRARY_PATH=$prefix/lib",
            "TERM=xterm-256color",
            "LANG=en_US.UTF-8"
        )

        // bash -lc 'exec ani-cli' — exec replaces the shell so there's no
        // intermediate prompt the user could type other commands into, and
        // the session's exit == ani-cli's exit.
        session = TerminalSession(
            "$prefix/bin/bash",
            home,
            arrayOf("bash", "-lc", "exec ani-cli"),
            env,
            /* transcriptRows */ 2000,
            this
        )
        terminalView.attachSession(session)
    }

    private fun showSoftKeyboard() {
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(terminalView, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    // --- TerminalSessionClient ---

    override fun onTextChanged(changedSession: TerminalSession) {
        terminalView.onScreenUpdated()
    }

    override fun onTitleChanged(changedSession: TerminalSession) {}

    override fun onSessionFinished(finishedSession: TerminalSession) {
        // ani-cli exited (user quit, episode ended and they backed all the way
        // out, or an error). Restart it fresh rather than exposing a shell.
        runOnUiThread {
            statusText.visibility = android.view.View.VISIBLE
            statusText.text = getString(R.string.anicli_exited_message)
            terminalView.postDelayed({ startAniCliSession() }, 1500)
        }
    }

    override fun onCopyTextToClipboard(session: TerminalSession, text: String?) {}
    override fun onPasteTextFromClipboard(session: TerminalSession?) {}
    override fun onBell(session: TerminalSession) {}
    override fun onColorsChanged(session: TerminalSession) {}
    override fun onTerminalCursorStateChange(state: Boolean) {}
    override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}
    override fun getTerminalCursorStyle(): Int? = null
    override fun logError(tag: String?, message: String?) {}
    override fun logWarn(tag: String?, message: String?) {}
    override fun logInfo(tag: String?, message: String?) {}
    override fun logDebug(tag: String?, message: String?) {}
    override fun logVerbose(tag: String?, message: String?) {}
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
    override fun logStackTrace(tag: String?, e: Exception?) {}

    override fun onBackPressed() {
        // Don't let back-press pop the activity and reveal launcher/recents
        // mid-episode; ani-cli has its own "back" (q / Ctrl-C) semantics.
        moveTaskToBack(true)
    }

    override fun finish() { super.finish() }
}

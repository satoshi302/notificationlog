package com.example.notificationlog.selfcheck

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.notificationlog.App
import com.example.notificationlog.data.SelfCheckRepository
import com.example.notificationlog.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * LINE等の入力欄のテキスト（送ろうとしているドラフト）を監視し、悪化しやすい内容なら
 * 送信前にオーバーレイで警告する。テキストは端末外に一切送信しない。
 *
 * オーバーレイ経路は文脈（相手の直近発言）を厳密に紐づけないため、ドラフト単体で解析する。
 * 相手の感情も含めた精緻な分析はアプリ内の手動チェックで行える。
 */
class LineWatchAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var settings: SettingsRepository
    private lateinit var repo: SelfCheckRepository
    private lateinit var overlay: OverlayController

    @Volatile private var enabled = false
    @Volatile private var threshold = 2
    @Volatile private var targets: Set<String> = emptySet()

    private var lastDraft: String = ""
    private var lastResult: SelfCheckResult? = null
    private var lastPackage: String = ""
    private var pending: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app = App.from(this)
        settings = app.settingsRepository
        repo = app.selfCheckRepository
        overlay = OverlayController(this)

        scope.launch { settings.selfCheckEnabled.collect { enabled = it; if (!it) clearOverlay() } }
        scope.launch { settings.selfCheckThreshold.collect { threshold = it } }
        scope.launch { settings.targetPackages.collect { targets = it } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!enabled || event == null) return
        val pkg = event.packageName?.toString() ?: return
        // 対象アプリ未選択なら全対象、選択されていればその集合のみ
        if (targets.isNotEmpty() && pkg !in targets) {
            clearOverlay(); return
        }
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> scheduleCheck(pkg)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> clearOverlay()
            else -> {}
        }
    }

    private fun scheduleCheck(pkg: String) {
        pending?.let { mainHandler.removeCallbacks(it) }
        val r = Runnable { runCheck(pkg) }
        pending = r
        mainHandler.postDelayed(r, 450)
    }

    private fun runCheck(pkg: String) {
        val draft = readFocusedEditableText()?.trim().orEmpty()
        if (draft.length < 4) { clearOverlay(); return }
        if (draft == lastDraft) return
        lastDraft = draft
        lastPackage = pkg

        scope.launch {
            val result = repo.analyze(draft, emptyList())
            lastResult = result
            mainHandler.post {
                if (result.riskLevel.score >= threshold) {
                    overlay.showChip(result) { onExpand() }
                } else {
                    overlay.remove()
                }
            }
        }
    }

    private fun onExpand() {
        val result = lastResult ?: return
        // 明示的に確認した = 記録する（傾向レポート用）
        scope.launch { repo.record("", lastPackage, result) }
        mainHandler.post {
            overlay.showCard(result) {
                overlay.showChip(result) { onExpand() }
            }
        }
    }

    private fun readFocusedEditableText(): String? {
        val root = rootInActiveWindow ?: return null
        val focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return null
        return if (focus.isEditable) focus.text?.toString() else null
    }

    private fun clearOverlay() {
        lastDraft = ""
        mainHandler.post { overlay.remove() }
    }

    override fun onInterrupt() {
        clearOverlay()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        clearOverlay()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        pending?.let { mainHandler.removeCallbacks(it) }
        overlay.remove()
        scope.cancel()
        super.onDestroy()
    }
}

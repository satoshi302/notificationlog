package com.example.notificationlog.selfcheck

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * 送信前の警告を画面上にオーバーレイ表示する。Compose を使わず素の View で構成し、
 * サービスからでも安全に描画できるようにする。すべて端末内で完結。
 *
 * チカチカ防止のため、チップは一度出したら消さずに**その場で内容だけ更新**する。
 * 明示的に [hide] を呼ぶまで表示は維持される。
 */
class OverlayController(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var chip: TextView? = null
    private var card: View? = null

    fun canDraw(): Boolean = Settings.canDrawOverlays(context)

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun baseParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            // フォーカスは奪わない（LINEでの入力を止めない）が、タップは受ける
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )
    }

    private fun dp(v: Int): Int = (v * context.resources.displayMetrics.density).toInt()

    /**
     * 小さなチップ。すでに出ていれば内容を更新するだけ（再描画=チカチカしない）。
     * タップで詳細カードへ。
     */
    fun showChip(result: SelfCheckResult, onExpand: () -> Unit) {
        if (!canDraw()) return
        removeCard()

        val existing = chip
        if (existing != null) {
            existing.setOnClickListener { onExpand() }
            applyChipStyle(existing, result)
            return
        }

        val c = TextView(context).apply {
            applyChipStyle(this, result)
            setOnClickListener { onExpand() }
        }
        val params = baseParams().apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(96)
        }
        if (runCatching { wm.addView(c, params) }.isSuccess) {
            chip = c
        }
    }

    private fun applyChipStyle(tv: TextView, result: SelfCheckResult) {
        val newText = "⚠ 一歩引いて考える（危険度: ${result.riskLevel.label}）"
        if (tv.text?.toString() != newText) tv.text = newText
        tv.setTextColor(Color.WHITE)
        tv.textSize = 14f
        tv.setPadding(dp(14), dp(10), dp(14), dp(10))
        tv.background = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            setColor(riskColor(result.riskLevel))
        }
    }

    /** 詳細カード。検出・受け取られ方・クールダウン・言い換えを表示。 */
    fun showCard(result: SelfCheckResult, onClose: () -> Unit) {
        if (!canDraw()) return
        removeChip()
        removeCard()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#1E1F24"))
                setStroke(dp(2), riskColor(result.riskLevel))
            }
        }

        root.addView(title("危険度: ${result.riskLevel.label}", riskColor(result.riskLevel)))

        if (result.flags.isNotEmpty()) {
            root.addView(
                line("検出: " + result.flags.joinToString("・") { it.category.label }, bold = true)
            )
            result.flags.maxByOrNull { it.category.weight }?.let { f ->
                root.addView(line("相手にはこう伝わりやすい: ${f.category.reception}"))
            }
        }

        if (result.otherParty.intensity >= 1) {
            root.addView(line(result.otherParty.note))
        }

        if (result.cooldownSuggested && result.cooldownReason != null) {
            root.addView(line("⏸ ${result.cooldownReason}", color = Color.parseColor("#FFB74D")))
        }

        val rephrase = result.rephrases.firstOrNull()
        if (rephrase != null) {
            root.addView(line("言い換え案（たたき台）:", bold = true))
            root.addView(line(rephrase.text, color = Color.parseColor("#A5D6A7")))
        }

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        if (rephrase != null) {
            buttons.addView(Button(context).apply {
                text = "言い換えをコピー"
                setOnClickListener {
                    copyToClipboard(rephrase.text)
                    Toast.makeText(context, "コピーしました", Toast.LENGTH_SHORT).show()
                }
            })
        }
        buttons.addView(Button(context).apply {
            text = "閉じる"
            setOnClickListener { onClose() }
        })
        root.addView(buttons)

        val params = baseParams().apply {
            width = minOf(dp(340), context.resources.displayMetrics.widthPixels - dp(24))
            gravity = Gravity.CENTER
        }
        if (runCatching { wm.addView(root, params) }.isSuccess) {
            card = root
        }
    }

    /** チップ・カードの両方を消す。 */
    fun hide() {
        removeChip()
        removeCard()
    }

    private fun removeChip() {
        chip?.let { runCatching { wm.removeView(it) } }
        chip = null
    }

    private fun removeCard() {
        card?.let { runCatching { wm.removeView(it) } }
        card = null
    }

    private fun title(text: String, color: Int) = TextView(context).apply {
        this.text = text
        setTextColor(color)
        textSize = 16f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, 0, 0, dp(6))
    }

    private fun line(text: String, bold: Boolean = false, color: Int = Color.WHITE) =
        TextView(context).apply {
            this.text = text
            setTextColor(color)
            textSize = 14f
            setPadding(0, dp(3), 0, dp(3))
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    private fun riskColor(level: RiskLevel): Int = when (level) {
        RiskLevel.LOW -> Color.parseColor("#43A047")
        RiskLevel.MEDIUM -> Color.parseColor("#FBC02D")
        RiskLevel.HIGH -> Color.parseColor("#FB8C00")
        RiskLevel.CRITICAL -> Color.parseColor("#E53935")
    }

    private fun copyToClipboard(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("rephrase", text))
    }
}

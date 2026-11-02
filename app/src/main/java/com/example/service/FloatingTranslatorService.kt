package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.TtsManager
import com.example.data.AppDatabase
import com.example.data.SettingsRepository
import com.example.engine.TranslationEngine
import com.example.model.Language
import com.example.model.TranslationRecord
import com.example.model.VoiceProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FloatingTranslatorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var windowManager: WindowManager? = null
    private var floatingBubbleView: View? = null
    private var translatorPanelView: View? = null

    private lateinit var bubbleParams: WindowManager.LayoutParams
    private lateinit var panelParams: WindowManager.LayoutParams

    private lateinit var translationEngine: TranslationEngine
    private lateinit var ttsManager: TtsManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var database: AppDatabase

    private var currentSourceLang: Language = Language.AUTO_DETECT
    private var currentTargetLang: Language = Language.findByCode("hi")

    private var screenWidth = 1080
    private var screenHeight = 1920

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        translationEngine = TranslationEngine(this)
        ttsManager = TtsManager(this)
        settingsRepository = SettingsRepository(this)
        database = AppDatabase.getDatabase(this)

        updateScreenDimensions()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        serviceScope.launch {
            val srcCode = settingsRepository.sourceLang.first()
            val tgtCode = settingsRepository.targetLang.first()
            currentSourceLang = Language.findByCode(srcCode)
            currentTargetLang = Language.findByCode(tgtCode)
        }

        setupBubbleView()
        setupPanelView()
        showBubble()
    }

    private fun updateScreenDimensions() {
        val dm = resources.displayMetrics
        screenWidth = dm.widthPixels
        screenHeight = dm.heightPixels
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("स्मार्ट फ्लोटिंग ट्रांसलेटर सक्रिय है")
            .setContentText("स्क्रीन अनुवाद के लिए फ्लोटिंग बबल पर टच करें")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBubbleView() {
        val bubbleSize = dpToPx(56)
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            bubbleSize,
            bubbleSize,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - bubbleSize - dpToPx(16)
            y = screenHeight / 3
        }

        // Custom circular bubble
        val frameLayout = FrameLayout(this)
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            colors = intArrayOf(Color.parseColor("#00D2FF"), Color.parseColor("#0A1128"))
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = bubbleSize.toFloat()
            setStroke(dpToPx(2), Color.parseColor("#FFFFFF"))
        }
        frameLayout.background = bgDrawable
        frameLayout.elevation = dpToPx(8).toFloat()

        val icon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            layoutParams = FrameLayout.LayoutParams(dpToPx(28), dpToPx(28), Gravity.CENTER)
        }
        frameLayout.addView(icon)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        frameLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isClick = false
                    }
                    bubbleParams.x = initialX + deltaX
                    bubbleParams.y = initialY + deltaY
                    windowManager?.updateViewLayout(floatingBubbleView, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        showPanel()
                    } else {
                        // Snap to closest edge (Left or Right)
                        val snapToRight = (bubbleParams.x + bubbleSize / 2) > (screenWidth / 2)
                        bubbleParams.x = if (snapToRight) screenWidth - bubbleSize - dpToPx(8) else dpToPx(8)
                        windowManager?.updateViewLayout(floatingBubbleView, bubbleParams)
                    }
                    true
                }
                else -> false
            }
        }

        floatingBubbleView = frameLayout
    }

    private fun setupPanelView() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val panelWidth = (screenWidth * 0.90f).toInt().coerceAtMost(dpToPx(380))
        val panelHeight = WindowManager.LayoutParams.WRAP_CONTENT

        panelParams = WindowManager.LayoutParams(
            panelWidth,
            panelHeight,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val panelCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16).toFloat()
                setColor(Color.parseColor("#0F1A33"))
                setStroke(dpToPx(1), Color.parseColor("#1E293B"))
            }
            background = bg
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(16))
            elevation = dpToPx(12).toFloat()
        }

        // Header
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "⚡ फ्लोटिंग ट्रांसलेटर"
            setTextColor(Color.parseColor("#38D9F5"))
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val minBtn = TextView(this).apply {
            text = " 🗕 "
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setOnClickListener { showBubble() }
        }

        val closeBtn = TextView(this).apply {
            text = " ✕ "
            textSize = 18f
            setTextColor(Color.parseColor("#FF5252"))
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setOnClickListener { stopSelf() }
        }

        headerRow.addView(title)
        headerRow.addView(minBtn)
        headerRow.addView(closeBtn)
        panelCard.addView(headerRow)

        // Lang Pair Header
        val langRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }

        val langText = TextView(this).apply {
            text = "${currentSourceLang.displayName} ➔ ${currentTargetLang.displayName}"
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        langRow.addView(langText)
        panelCard.addView(langRow)

        // Quick Screen Read Button
        val screenReadBtn = Button(this).apply {
            text = "📱 स्क्रीन से टेक्स्ट पढ़ें (Read Screen)"
            textSize = 13f
            setTextColor(Color.parseColor("#0A1128"))
            val btnBg = GradientDrawable().apply {
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.parseColor("#00E5FF"))
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(40)
            ).apply { setMargins(0, dpToPx(4), 0, dpToPx(8)) }
        }
        panelCard.addView(screenReadBtn)

        // Original Text Input
        val originalInput = EditText(this).apply {
            hint = "मूल टेक्स्ट लिखें या स्क्रीन से पढ़ें..."
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.WHITE)
            textSize = 14f
            maxLines = 4
            val inputBg = GradientDrawable().apply {
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.parseColor("#1E293B"))
                setStroke(dpToPx(1), Color.parseColor("#334155"))
            }
            background = inputBg
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        panelCard.addView(originalInput)

        // Translate Button
        val translateBtn = Button(this).apply {
            text = "अनुवाद करें (Translate)"
            textSize = 13f
            setTextColor(Color.WHITE)
            val btnBg = GradientDrawable().apply {
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.parseColor("#2563EB"))
            }
            background = btnBg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(40)
            ).apply { setMargins(0, dpToPx(8), 0, dpToPx(8)) }
        }
        panelCard.addView(translateBtn)

        // Translated Text Box with ScrollView
        val scrollBox = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(100)
            )
        }

        val translatedText = TextView(this).apply {
            text = "अनुवादित परिणाम यहाँ दिखेगा..."
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 14f
            val boxBg = GradientDrawable().apply {
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.parseColor("#16203A"))
                setStroke(dpToPx(1), Color.parseColor("#29385C"))
            }
            background = boxBg
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
        }
        scrollBox.addView(translatedText)
        panelCard.addView(scrollBox)

        // Action Buttons Row (Copy & Speak)
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dpToPx(8), 0, 0)
        }

        val copyBtn = Button(this).apply {
            text = "📋 कॉपी"
            textSize = 12f
            setTextColor(Color.WHITE)
            val bBg = GradientDrawable().apply {
                cornerRadius = dpToPx(6).toFloat()
                setColor(Color.parseColor("#334155"))
            }
            background = bBg
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f).apply {
                setMargins(0, 0, dpToPx(4), 0)
            }
        }

        val speakBtn = Button(this).apply {
            text = "🔊 सुनें"
            textSize = 12f
            setTextColor(Color.WHITE)
            val bBg = GradientDrawable().apply {
                cornerRadius = dpToPx(6).toFloat()
                setColor(Color.parseColor("#334155"))
            }
            background = bBg
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f).apply {
                setMargins(dpToPx(4), 0, 0, 0)
            }
        }

        actionRow.addView(copyBtn)
        actionRow.addView(speakBtn)
        panelCard.addView(actionRow)

        // Click actions
        screenReadBtn.setOnClickListener {
            val capturedText = ScreenTranslatorAccessibilityService.captureActiveScreenText()
            if (capturedText.isNotBlank()) {
                originalInput.setText(capturedText)
                translateBtn.performClick()
                Toast.makeText(this, "स्क्रीन से टेक्स्ट पढ़ा गया", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "स्क्रीन टेक्स्ट नहीं मिला। कृपया सेटिंग्स से एक्सेसिबिलिटी सेवा सक्षम करें।",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        translateBtn.setOnClickListener {
            val text = originalInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            translateBtn.text = "अनुवाद हो रहा है..."
            translateBtn.isEnabled = false

            serviceScope.launch {
                val result = translationEngine.translate(text, currentSourceLang, currentTargetLang)
                translatedText.text = result.translatedText
                translatedText.setTextColor(Color.WHITE)

                val displaySrc = result.detectedLanguage?.displayName ?: currentSourceLang.displayName
                langText.text = "$displaySrc ➔ ${currentTargetLang.displayName}"

                // Save to history
                database.translationDao().insertRecord(
                    TranslationRecord(
                        sourceLangCode = result.detectedLanguage?.code ?: currentSourceLang.code,
                        targetLangCode = currentTargetLang.code,
                        originalText = text,
                        translatedText = result.translatedText,
                        category = "FLOATING"
                    )
                )

                translateBtn.text = "अनुवाद करें (Translate)"
                translateBtn.isEnabled = true
            }
        }

        copyBtn.setOnClickListener {
            val res = translatedText.text.toString()
            if (res.isNotEmpty() && res != "अनुवादित परिणाम यहाँ दिखेगा...") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Translated Text", res)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "अनुवाद कॉपी किया गया", Toast.LENGTH_SHORT).show()
            }
        }

        speakBtn.setOnClickListener {
            val res = translatedText.text.toString()
            if (res.isNotEmpty() && res != "अनुवादित परिणाम यहाँ दिखेगा...") {
                val voice = VoiceProfile.TEN_VOICES[0]
                ttsManager.speak(res, currentTargetLang, voice)
            }
        }

        translatorPanelView = panelCard
    }

    private fun showBubble() {
        try {
            if (translatorPanelView != null && translatorPanelView?.isAttachedToWindow == true) {
                windowManager?.removeView(translatorPanelView)
            }
            if (floatingBubbleView != null && floatingBubbleView?.isAttachedToWindow != true) {
                windowManager?.addView(floatingBubbleView, bubbleParams)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showPanel() {
        try {
            if (floatingBubbleView != null && floatingBubbleView?.isAttachedToWindow == true) {
                windowManager?.removeView(floatingBubbleView)
            }
            if (translatorPanelView != null && translatorPanelView?.isAttachedToWindow != true) {
                windowManager?.addView(translatorPanelView, panelParams)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        ttsManager.shutdown()
        try {
            if (floatingBubbleView != null && floatingBubbleView?.isAttachedToWindow == true) {
                windowManager?.removeView(floatingBubbleView)
            }
            if (translatorPanelView != null && translatorPanelView?.isAttachedToWindow == true) {
                windowManager?.removeView(translatorPanelView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    companion object {
        const val CHANNEL_ID = "floating_translator_channel"
        const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
            val intent = Intent(context, FloatingTranslatorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingTranslatorService::class.java)
            context.stopService(intent)
        }
    }
}

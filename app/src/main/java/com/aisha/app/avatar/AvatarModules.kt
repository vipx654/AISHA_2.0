package com.aisha.app.avatar

import com.aisha.app.model.EngineSnapshot
import com.aisha.app.model.Expression

/**
 * LOCKED §11 — Avatar Controller. Receives Core Engine state and coordinates
 * presentation. STATE FLOW IS ONE-WAY: Core → Controller → expression/animation/
 * lip-sync. The avatar never invents AISHA's emotional state.
 */
interface AvatarController {
    fun render(snapshot: EngineSnapshot)
    fun onSpeechStarted()
    fun onSpeechEnded()
}

/** LOCKED §11 — neutral, happy, thoughtful, concerned; expandable set. */
interface ExpressionEngine {
    fun expressionFor(snapshot: EngineSnapshot): Expression
}

/** LOCKED §11 — breathing, blinking, posture, looking around, idle variation. Battery-aware. */
interface AnimationEngine {
    fun idleLoopPolicy(batterySaver: Boolean): IdlePolicy
}

data class IdlePolicy(val breathing: Boolean, val blinking: Boolean, val lookAround: Boolean, val fpsCap: Int)

/** LOCKED §11/§13 — speech-synchronised mouth movement during TTS playback. */
interface LipSyncEngine {
    fun bind(ttsStreamId: String)
}

/**
 * LOCKED §12 — Live Wallpaper. Optional Android live wallpaper; lightweight,
 * ambient, battery-aware (pause when screen-off/low battery), NO microphone or
 * camera access merely because wallpaper is active, no conversation on unlock.
 */
interface LiveWallpaperServiceContract {
    fun ambientFramePolicy(): IdlePolicy
}

package com.aisha.app.di

import android.content.Context
import androidx.room.Room
import com.aisha.app.ai.GeminiLanguageModel
import com.aisha.app.data.AishaDatabase
import com.aisha.app.data.RoomDayLogStore
import com.aisha.app.security.KeystoreCrypto
import com.aisha.core.AishaCoreEngine
import com.aisha.core.Clock
import com.aisha.core.LanguageModel
import com.aisha.core.MockLanguageModel
import java.time.LocalDateTime

/**
 * Manual dependency graph (spec §1: clear module responsibilities and interfaces).
 * Production app composition root — swaps Gemini for Mock when no key is set,
 * which is also the offline/degraded fallback (spec §21).
 */
class AppContainer(context: Context, geminiApiKey: String?) {

    val clock: Clock = Clock { LocalDateTime.now() }

    val crypto = KeystoreCrypto()

    private val db = Room.databaseBuilder(context, AishaDatabase::class.java, AishaDatabase.NAME)
        .fallbackToDestructiveMigrationOnDowngrade() // real migrations arrive with schema v2+ (spec §21)
        .build()

    val dayLogStore = RoomDayLogStore(db.dayLogDao(), crypto)

    val languageModel: LanguageModel =
        geminiApiKey?.takeIf { it.isNotBlank() }?.let { GeminiLanguageModel(it) } ?: MockLanguageModel()

    val core = AishaCoreEngine(
        store = dayLogStore,
        languageModel = languageModel,
        clock = clock,
    )
}

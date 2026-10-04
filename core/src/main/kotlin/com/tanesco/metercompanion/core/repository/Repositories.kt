package com.tanesco.metercompanion.core.repository

import com.tanesco.metercompanion.core.model.AppEvent
import com.tanesco.metercompanion.core.model.AppSettings
import com.tanesco.metercompanion.core.model.DomainResult
import com.tanesco.metercompanion.core.model.EventQuery
import com.tanesco.metercompanion.core.model.MeterProfile
import com.tanesco.metercompanion.core.model.MeterSession
import com.tanesco.metercompanion.core.model.SessionOutcome
import com.tanesco.metercompanion.core.model.TokenHistory
import com.tanesco.metercompanion.core.model.TokenQuery
import com.tanesco.metercompanion.core.model.TokenRecord
import com.tanesco.metercompanion.core.model.TokenTransferState
import kotlinx.coroutines.flow.Flow

/**
 * Repository ports owned by the domain layer.
 *
 * The domain declares what it needs; `:data` supplies SQLite-backed
 * implementations and tests supply in-memory ones. No interface in this file
 * mentions SQLite, Android or Compose, which is what allows the whole domain to
 * be exercised by fast JVM tests.
 */

interface MeterRepository {

    /** Emits the current profile list and every subsequent change. */
    fun observeAll(): Flow<List<MeterProfile>>

    /** Emits one profile, or `null` when it does not exist. */
    fun observeById(id: String): Flow<MeterProfile?>

    suspend fun getAll(): List<MeterProfile>

    suspend fun getById(id: String): MeterProfile?

    /** Inserts or replaces [profile], keyed by its id. */
    suspend fun upsert(profile: MeterProfile): DomainResult<Unit>

    /**
     * Soft-deletes a profile.
     *
     * Archiving rather than deleting keeps historical events and tokens
     * meaningful.
     */
    suspend fun archive(id: String): DomainResult<Unit>

    /** Whether [normalizedAccountNumber] already belongs to another profile. */
    suspend fun isAccountNumberInUse(normalizedAccountNumber: String, excludeProfileId: String?): Boolean
}

interface TokenRepository {

    /** Emits the token history matching [query]. */
    fun observeHistory(query: TokenQuery): Flow<TokenHistory>

    suspend fun getHistory(query: TokenQuery): TokenHistory

    suspend fun getById(id: String): TokenRecord?

    /**
     * Locates an earlier token with the same canonical value.
     *
     * [excludeTokenId] lets a re-validated record exclude itself.
     */
    suspend fun findPreviousWithSameValue(
        normalizedValue: String,
        beforeMillis: Long,
        excludeTokenId: String? = null,
    ): TokenRecord?

    suspend fun insert(record: TokenRecord): DomainResult<Unit>

    /** Updates the hand-off progress of an existing token. */
    suspend fun setTransferState(id: String, state: TokenTransferState): DomainResult<Unit>
}

interface EventRepository {

    /** Appends a single event. The log is append-only. */
    suspend fun append(event: AppEvent): DomainResult<Unit>

    suspend fun query(query: EventQuery): List<AppEvent>

    fun observe(query: EventQuery): Flow<List<AppEvent>>

    /** Number of events recorded under [name], used by duplicate diagnostics. */
    suspend fun countByName(name: String): Int

    /** Deletes events older than [olderThanMillis] and returns how many. */
    suspend fun prune(olderThanMillis: Long): Int
}

interface SettingsRepository {

    fun observe(): Flow<AppSettings>

    suspend fun get(): AppSettings

    suspend fun set(settings: AppSettings): DomainResult<Unit>
}

interface MeterSessionRepository {

    /** Records the start of an attempt. */
    suspend fun start(session: MeterSession): DomainResult<Unit>

    /** Closes an attempt with its terminal [outcome]. */
    suspend fun finish(
        id: String,
        endedAtMillis: Long,
        outcome: SessionOutcome,
        failureCode: String? = null,
    ): DomainResult<Unit>

    fun observeRecent(limit: Int): Flow<List<MeterSession>>

    suspend fun getActive(meterId: String?): MeterSession?

    suspend fun getRecent(limit: Int): List<MeterSession>
}

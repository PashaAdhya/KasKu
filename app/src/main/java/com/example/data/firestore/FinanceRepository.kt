package com.example.data.firestore

import com.example.data.model.Category
import com.example.data.model.CategoryBudget
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringTemplate
import com.example.data.model.TransactionRecord
import com.example.data.model.TransactionType
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FinanceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    private fun requireAuthorizedUser(expectedUid: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google terlebih dahulu.")
        check(uid == expectedUid) { "Sesi akun berubah; operasi dibatalkan." }
        return uid
    }

    private fun handleStreamError(error: Throwable, uid: String, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
        throw error
    }

    fun observeTransactions(userId: String): Flow<List<TransactionRecord>> = flow {
        require(userId.isNotBlank()) { "User ID kosong" }
        val path = "users/$userId/transactions"
        emitAll(
            db.collection("users").document(userId).collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TransactionRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error -> handleStreamError(error, userId, path) }
        )
    }

    fun observeBudgets(userId: String): Flow<List<CategoryBudget>> = flow {
        require(userId.isNotBlank()) { "User ID kosong" }
        val path = "users/$userId/budgets"
        emitAll(
            db.collection("users").document(userId).collection("budgets")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CategoryBudget::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error -> handleStreamError(error, userId, path) }
        )
    }

    fun observeRecurring(userId: String): Flow<List<RecurringTemplate>> = flow {
        require(userId.isNotBlank()) { "User ID kosong" }
        val path = "users/$userId/recurring"
        emitAll(
            db.collection("users").document(userId).collection("recurring")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RecurringTemplate::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
                }
                .catch { error -> handleStreamError(error, userId, path) }
        )
    }

    suspend fun addTransaction(
        userId: String,
        title: String,
        amount: Long,
        type: TransactionType,
        category: Category,
        paymentMethod: PaymentMethod,
        note: String,
        dateEpochDay: Long,
        monthKey: String,
    ): Result<Unit> {
        var docPath = "users/$userId/transactions"
        return try {
            val uid = requireAuthorizedUser(userId)
            val cleanTitle = title.trim()
            val cleanNote = note.trim()
            require(cleanTitle.length in 1..80) { "Judul transaksi harus 1-80 karakter." }
            require(amount in 1L..1_000_000_000_000L) { "Nominal harus lebih dari Rp 0." }
            require(dateEpochDay in 0L..100_000L) { "Tanggal tidak valid." }
            require(cleanNote.length <= 250) { "Catatan maksimal 250 karakter." }
            require(MONTH_KEY_REGEX.matches(monthKey)) { "Format bulan tidak valid." }

            val docRef = db.collection("users").document(uid).collection("transactions").document()
            docPath = docRef.path
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "title" to cleanTitle,
                "amount" to amount,
                "type" to type.name,
                "category" to category.name,
                "paymentMethod" to paymentMethod.name,
                "dateEpochDay" to dateEpochDay,
                "monthKey" to monthKey,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (cleanNote.isNotEmpty()) {
                payload["note"] = cleanNote
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(
        userId: String,
        txId: String,
        title: String,
        amount: Long,
        type: TransactionType,
        category: Category,
        paymentMethod: PaymentMethod,
        note: String,
        dateEpochDay: Long,
        monthKey: String,
    ): Result<Unit> {
        var docPath = "users/$userId/transactions/$txId"
        return try {
            val uid = requireAuthorizedUser(userId)
            require(txId.isNotBlank()) { "ID transaksi kosong." }
            val cleanTitle = title.trim()
            val cleanNote = note.trim()
            require(cleanTitle.length in 1..80) { "Judul transaksi harus 1-80 karakter." }
            require(amount in 1L..1_000_000_000_000L) { "Nominal harus lebih dari Rp 0." }
            require(dateEpochDay in 0L..100_000L) { "Tanggal tidak valid." }
            require(cleanNote.length <= 250) { "Catatan maksimal 250 karakter." }
            require(MONTH_KEY_REGEX.matches(monthKey)) { "Format bulan tidak valid." }

            val docRef = db.collection("users").document(uid).collection("transactions").document(txId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "title" to cleanTitle,
                    "amount" to amount,
                    "type" to type.name,
                    "category" to category.name,
                    "paymentMethod" to paymentMethod.name,
                    "note" to cleanNote,
                    "dateEpochDay" to dateEpochDay,
                    "monthKey" to monthKey,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(userId: String, txId: String): Result<Unit> {
        var docPath = "users/$userId/transactions/$txId"
        return try {
            val uid = requireAuthorizedUser(userId)
            require(txId.isNotBlank()) { "ID transaksi kosong." }
            val docRef = db.collection("users").document(uid).collection("transactions").document(txId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertBudget(
        userId: String,
        monthKey: String,
        categoryKey: String,
        limitAmount: Long,
    ): Result<Unit> {
        val budgetId = "${monthKey}_${categoryKey}"
        var docPath = "users/$userId/budgets/$budgetId"
        return try {
            val uid = requireAuthorizedUser(userId)
            require(MONTH_KEY_REGEX.matches(monthKey)) { "Format bulan tidak valid." }
            require(categoryKey.length in 1..40) { "Kategori tidak valid." }
            require(limitAmount in 1L..1_000_000_000_000L) { "Batas anggaran harus lebih dari Rp 0." }

            val docRef = db.collection("users").document(uid).collection("budgets").document(budgetId)
            docPath = docRef.path
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "limitAmount" to limitAmount,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "monthKey" to monthKey,
                        "category" to categoryKey,
                        "limitAmount" to limitAmount,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit> {
        var docPath = "users/$userId/budgets/$budgetId"
        return try {
            val uid = requireAuthorizedUser(userId)
            require(budgetId.isNotBlank()) { "ID anggaran kosong." }
            val docRef = db.collection("users").document(uid).collection("budgets").document(budgetId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addRecurringTemplate(
        userId: String,
        title: String,
        amount: Long,
        type: TransactionType,
        category: Category,
        paymentMethod: PaymentMethod,
        dueDay: Int,
    ): Result<Unit> {
        var docPath = "users/$userId/recurring"
        return try {
            val uid = requireAuthorizedUser(userId)
            val cleanTitle = title.trim()
            require(cleanTitle.length in 1..80) { "Judul harus 1-80 karakter." }
            require(amount in 1L..1_000_000_000_000L) { "Nominal harus lebih dari Rp 0." }
            require(dueDay in 1..31) { "Tanggal jatuh tempo harus 1-31." }

            val docRef = db.collection("users").document(uid).collection("recurring").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "amount" to amount,
                    "type" to type.name,
                    "category" to category.name,
                    "paymentMethod" to paymentMethod.name,
                    "dueDay" to dueDay.toLong(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markRecurringLogged(
        userId: String,
        template: RecurringTemplate,
        monthKey: String,
        dateEpochDay: Long,
    ): Result<Unit> {
        var docPath = "users/$userId/recurring/${template.id}"
        return try {
            val uid = requireAuthorizedUser(userId)
            require(template.id.isNotBlank()) { "ID template kosong." }
            require(MONTH_KEY_REGEX.matches(monthKey)) { "Format bulan tidak valid." }

            val txCol = db.collection("users").document(uid).collection("transactions")
            val recRef = db.collection("users").document(uid).collection("recurring").document(template.id)
            docPath = recRef.path
            val batch = db.batch()
            val newTxRef = txCol.document()
            batch.set(
                newTxRef,
                mapOf(
                    "userId" to uid,
                    "title" to template.title,
                    "amount" to template.amount,
                    "type" to template.type,
                    "category" to template.category,
                    "paymentMethod" to template.paymentMethod,
                    "note" to "Dicatat otomatis dari template rutin",
                    "dateEpochDay" to dateEpochDay,
                    "monthKey" to monthKey,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            batch.update(
                recRef,
                mapOf(
                    "lastLoggedMonth" to monthKey,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteRecurringTemplate(userId: String, itemId: String): Result<Unit> {
        var docPath = "users/$userId/recurring/$itemId"
        return try {
            val uid = requireAuthorizedUser(userId)
            require(itemId.isNotBlank()) { "ID template kosong." }
            val docRef = db.collection("users").document(uid).collection("recurring").document(itemId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private companion object {
        val MONTH_KEY_REGEX = Regex("^[0-9]{4}-[0-9]{2}$")
    }
}

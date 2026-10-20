package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.AccountItem
import com.example.model.Transaction
import com.example.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class KharchaRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val usersCollection = db.collection("users")

    fun observeProfile(userId: String): Flow<UserProfile?> {
        return usersCollection.document(userId).snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java)
                } else null
            }
            .catch { error ->
                Log.e("KharchaRepository", "Error observing profile: ${error.message}", error)
                emit(null)
            }
    }

    suspend fun saveProfile(userId: String, profile: UserProfile) {
        val payload = hashMapOf(
            "name" to profile.name,
            "email" to profile.email,
            "currencyUnit" to profile.currencyUnit,
            "themeMode" to profile.themeMode,
            "totalLiquidity" to profile.totalLiquidity,
            "protectedVault" to profile.protectedVault,
            "netWorth" to profile.netWorth,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        usersCollection.document(userId).set(payload).await()
    }

    fun observeAccounts(userId: String): Flow<List<AccountItem>> {
        return usersCollection.document(userId).collection("accounts").snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(AccountItem::class.java) }
            }
            .catch { error ->
                Log.e("KharchaRepository", "Error observing accounts: ${error.message}", error)
                emit(emptyList())
            }
    }

    suspend fun saveAccount(userId: String, account: AccountItem) {
        val docRef = usersCollection.document(userId).collection("accounts").document(account.id)
        val payload = hashMapOf(
            "id" to account.id,
            "name" to account.name,
            "mask" to account.mask,
            "subtext" to account.subtext,
            "balance" to account.balance,
            "isCredit" to account.isCredit,
            "apy" to (account.apy ?: ""),
            "icon" to account.icon,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
    }

    suspend fun deleteAccount(userId: String, accountId: String) {
        usersCollection.document(userId).collection("accounts").document(accountId).delete().await()
    }

    fun observeTransactions(userId: String): Flow<List<Transaction>> {
        return usersCollection.document(userId).collection("transactions").snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(Transaction::class.java) }
            }
            .catch { error ->
                Log.e("KharchaRepository", "Error observing transactions: ${error.message}", error)
                emit(emptyList())
            }
    }

    suspend fun saveTransaction(userId: String, transaction: Transaction) {
        val docRef = usersCollection.document(userId).collection("transactions").document(transaction.id)
        val payload = hashMapOf(
            "id" to transaction.id,
            "title" to transaction.title,
            "category" to transaction.category,
            "timestamp" to transaction.timestamp,
            "dateGroup" to transaction.dateGroup,
            "accountName" to transaction.accountName,
            "amount" to transaction.amount,
            "status" to transaction.status,
            "type" to transaction.type.name,
            "note" to transaction.note,
            "iconName" to transaction.iconName,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
    }

    suspend fun deleteTransaction(userId: String, transactionId: String) {
        usersCollection.document(userId).collection("transactions").document(transactionId).delete().await()
    }

    suspend fun deleteAllUserData(userId: String) {
        try {
            val accountsSnapshot = usersCollection.document(userId).collection("accounts").get().await()
            for (doc in accountsSnapshot.documents) {
                doc.reference.delete().await()
            }

            val transactionsSnapshot = usersCollection.document(userId).collection("transactions").get().await()
            for (doc in transactionsSnapshot.documents) {
                doc.reference.delete().await()
            }

            usersCollection.document(userId).delete().await()
        } catch (e: Exception) {
            Log.e("KharchaRepository", "Error wiping user cloud data: ${e.message}", e)
            throw e
        }
    }
}

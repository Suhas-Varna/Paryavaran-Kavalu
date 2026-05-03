package com.example.paryavaran_kavalu.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE userId = 1 LIMIT 1")
    suspend fun getUser(): UserEntity?

    @Query("SELECT * FROM user_profile WHERE userId = 1 LIMIT 1")
    fun observeUser(): Flow<UserEntity?>

    @Query("UPDATE user_profile SET ecoPoints = ecoPoints + :delta WHERE userId = 1")
    suspend fun addEcoPoints(delta: Int)

    /**
     * Deducts [cost] only if the current balance is enough. Returns the number of rows updated (0 or 1).
     */
    @Query(
        "UPDATE user_profile SET ecoPoints = ecoPoints - :cost WHERE userId = 1 AND ecoPoints >= :cost",
    )
    suspend fun tryDeductEcoPoints(cost: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Query(
        """
        UPDATE user_profile
        SET nickname = :nickname, userType = :userType, bio = :bio
        WHERE userId = 1
        """,
    )
    suspend fun updateProfile(nickname: String, userType: String, bio: String)
}

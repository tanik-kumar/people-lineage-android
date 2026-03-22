package com.tanik.peoplelineage.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM people WHERE deletedAt IS NULL ORDER BY isFavorite DESC, lastViewedAt DESC, fullName COLLATE NOCASE")
    suspend fun getAllPeople(): List<PersonEntity>

    @Query(
        """
        SELECT * FROM people
        WHERE deletedAt IS NULL
        ORDER BY isFavorite DESC, lastViewedAt DESC, fullName COLLATE NOCASE
        """,
    )
    fun observeAllPeople(): Flow<List<PersonEntity>>

    @Query("SELECT COUNT(*) FROM people")
    suspend fun getPeopleCount(): Int

    @Query(
        """
        SELECT * FROM people
        WHERE deletedAt IS NULL
            AND (
                :query = ''
                OR normalizedName LIKE '%' || :query || '%'
                OR normalizedPhone LIKE '%' || :query || '%'
                OR normalizedVillage LIKE '%' || :query || '%'
                OR normalizedDistrict LIKE '%' || :query || '%'
                OR normalizedState LIKE '%' || :query || '%'
            )
            AND (:gender = '' OR LOWER(gender) = LOWER(:gender))
            AND (:village = '' OR normalizedVillage LIKE '%' || :village || '%')
            AND (:district = '' OR normalizedDistrict LIKE '%' || :district || '%')
            AND (:state = '' OR normalizedState LIKE '%' || :state || '%')
            AND (:favoritesOnly = 0 OR isFavorite = 1)
        ORDER BY isFavorite DESC, lastViewedAt DESC, fullName COLLATE NOCASE
        """,
    )
    suspend fun searchPeople(
        query: String,
        gender: String,
        village: String,
        district: String,
        state: String,
        favoritesOnly: Boolean,
    ): List<PersonEntity>

    @Query(
        """
        SELECT * FROM people
        WHERE deletedAt IS NULL
            AND (
                :query = ''
                OR normalizedName LIKE '%' || :query || '%'
                OR normalizedPhone LIKE '%' || :query || '%'
                OR normalizedVillage LIKE '%' || :query || '%'
                OR normalizedDistrict LIKE '%' || :query || '%'
                OR normalizedState LIKE '%' || :query || '%'
            )
            AND (:gender = '' OR LOWER(gender) = LOWER(:gender))
            AND (:village = '' OR normalizedVillage LIKE '%' || :village || '%')
            AND (:district = '' OR normalizedDistrict LIKE '%' || :district || '%')
            AND (:state = '' OR normalizedState LIKE '%' || :state || '%')
            AND (:favoritesOnly = 0 OR isFavorite = 1)
        ORDER BY isFavorite DESC, lastViewedAt DESC, fullName COLLATE NOCASE
        """,
    )
    fun observeSearchPeople(
        query: String,
        gender: String,
        village: String,
        district: String,
        state: String,
        favoritesOnly: Boolean,
    ): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE id = :personId LIMIT 1")
    suspend fun getPersonById(personId: Long): PersonEntity?

    @Insert
    suspend fun insertPerson(person: PersonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeople(people: List<PersonEntity>)

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Query("UPDATE people SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :personId")
    suspend fun updateFavorite(personId: Long, isFavorite: Boolean, updatedAt: Long)

    @Query("UPDATE people SET lastViewedAt = :timestamp WHERE id = :personId")
    suspend fun updateLastViewedAt(personId: Long, timestamp: Long)

    @Delete
    suspend fun deletePerson(person: PersonEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRelationship(relationship: PersonRelationshipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationships(relationships: List<PersonRelationshipEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSpouseRelationship(relationship: SpouseRelationshipEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpouseRelationships(relationships: List<SpouseRelationshipEntity>)

    @Query("DELETE FROM person_relationships WHERE parentId = :parentId AND childId = :childId")
    suspend fun deleteRelationship(parentId: Long, childId: Long)

    @Query("SELECT * FROM person_relationships WHERE childId = :childId")
    suspend fun getParentRelationshipsForChild(childId: Long): List<PersonRelationshipEntity>

    @Query("SELECT * FROM person_relationships WHERE childId = :childId AND relationType = :relationType LIMIT 1")
    suspend fun getParentRelationshipByType(childId: Long, relationType: String): PersonRelationshipEntity?

    @Query("SELECT * FROM person_relationships WHERE parentId = :parentId AND childId = :childId LIMIT 1")
    suspend fun getParentRelationshipByPair(parentId: Long, childId: Long): PersonRelationshipEntity?

    @Query("DELETE FROM person_relationships WHERE childId = :childId AND relationType = :relationType")
    suspend fun deleteParentRelationshipByType(childId: Long, relationType: String)

    @Query("UPDATE person_relationships SET relationType = :relationType WHERE id = :relationshipId")
    suspend fun updateParentRelationshipType(relationshipId: Long, relationType: String)

    @Query(
        """
        SELECT p.* FROM people p
        INNER JOIN spouse_relationships s
            ON ((s.personAId = :personId AND p.id = s.personBId)
                OR (s.personBId = :personId AND p.id = s.personAId))
        ORDER BY p.fullName COLLATE NOCASE
        """,
    )
    suspend fun getSpousesForPerson(personId: Long): List<PersonEntity>

    @Query(
        """
        SELECT CASE
            WHEN personAId = :personId THEN personBId
            ELSE personAId
        END
        FROM spouse_relationships
        WHERE personAId = :personId OR personBId = :personId
        """,
    )
    suspend fun getSpouseIdsForPerson(personId: Long): List<Long>

    @Query(
        """
        SELECT p.* FROM people p
        INNER JOIN person_relationships r ON r.parentId = p.id
        WHERE r.childId = :childId
        ORDER BY p.fullName COLLATE NOCASE
        """,
    )
    suspend fun getParentsForChild(childId: Long): List<PersonEntity>

    @Query(
        """
        SELECT p.* FROM people p
        INNER JOIN person_relationships r ON r.childId = p.id
        WHERE r.parentId = :parentId
        ORDER BY p.fullName COLLATE NOCASE
        """,
    )
    suspend fun getChildrenForParent(parentId: Long): List<PersonEntity>

    @Query("SELECT childId FROM person_relationships WHERE parentId = :parentId")
    suspend fun getChildIdsForParent(parentId: Long): List<Long>

    @Query("SELECT * FROM person_relationships")
    suspend fun getAllParentRelationships(): List<PersonRelationshipEntity>

    @Query("SELECT * FROM spouse_relationships")
    suspend fun getAllSpouseRelationships(): List<SpouseRelationshipEntity>

    @Query("DELETE FROM spouse_relationships")
    suspend fun clearSpouseRelationships()

    @Query("DELETE FROM person_relationships")
    suspend fun clearParentRelationships()

    @Query("DELETE FROM people")
    suspend fun clearPeople()

    @Query(
        """
        SELECT * FROM people
        WHERE id != :excludeId
            AND LOWER(village) = LOWER(:village)
        ORDER BY fullName COLLATE NOCASE
        """,
    )
    suspend fun getPeopleByVillage(village: String, excludeId: Long): List<PersonEntity>

    @Query(
        """
        SELECT * FROM people
        WHERE id != :excludeId
            AND LOWER(policeStation) = LOWER(:policeStation)
            AND LOWER(postOffice) = LOWER(:postOffice)
            AND LOWER(district) = LOWER(:district)
            AND LOWER(state) = LOWER(:state)
            AND LOWER(country) = LOWER(:country)
        ORDER BY fullName COLLATE NOCASE
        """,
    )
    suspend fun getPeopleByAddress(
        policeStation: String,
        postOffice: String,
        district: String,
        state: String,
        country: String,
        excludeId: Long,
    ): List<PersonEntity>
}

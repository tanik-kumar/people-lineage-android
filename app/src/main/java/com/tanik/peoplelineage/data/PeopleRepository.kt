package com.tanik.peoplelineage.data

import android.content.Context
import androidx.room.withTransaction
import com.tanik.peoplelineage.core.text.SearchNormalizer
import com.tanik.peoplelineage.core.validation.LineageValidationEngine
import com.tanik.peoplelineage.model.AddressSeed
import com.tanik.peoplelineage.model.LineageGraph
import com.tanik.peoplelineage.model.LineageGraphLink
import com.tanik.peoplelineage.model.LineageGraphLinkType
import com.tanik.peoplelineage.model.LineageGraphNode
import com.tanik.peoplelineage.model.ParentRelationType
import com.tanik.peoplelineage.model.PersonDetailSnapshot
import com.tanik.peoplelineage.model.PersonDraft
import com.tanik.peoplelineage.model.PersonSearchFilters
import com.tanik.peoplelineage.model.RelationAddResult
import com.tanik.peoplelineage.model.SavePersonResult
import com.tanik.peoplelineage.model.shortLocation
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class PeopleRepository private constructor(
    private val database: AppDatabase,
) {

    private val personDao = database.personDao()
    private val storageEventDao = database.storageEventDao()

    suspend fun searchPeople(query: String): List<PersonEntity> {
        return searchPeople(
            PersonSearchFilters(query = query),
        )
    }

    suspend fun searchPeople(filters: PersonSearchFilters): List<PersonEntity> {
        return personDao.searchPeople(
            query = SearchNormalizer.normalizeText(filters.query),
            gender = filters.gender.trim(),
            village = SearchNormalizer.normalizeText(filters.village),
            district = SearchNormalizer.normalizeText(filters.district),
            state = SearchNormalizer.normalizeText(filters.state),
            favoritesOnly = filters.favoritesOnly,
        )
    }

    fun observePeople(filters: PersonSearchFilters = PersonSearchFilters()): Flow<List<PersonEntity>> {
        return personDao.observeSearchPeople(
            query = SearchNormalizer.normalizeText(filters.query),
            gender = filters.gender.trim(),
            village = SearchNormalizer.normalizeText(filters.village),
            district = SearchNormalizer.normalizeText(filters.district),
            state = SearchNormalizer.normalizeText(filters.state),
            favoritesOnly = filters.favoritesOnly,
        )
    }

    suspend fun hasPeople(): Boolean = personDao.getPeopleCount() > 0

    suspend fun getPerson(personId: Long): PersonEntity? = personDao.getPersonById(personId)

    suspend fun markPersonViewed(personId: Long) {
        personDao.updateLastViewedAt(personId, System.currentTimeMillis())
    }

    suspend fun setFavorite(personId: Long, isFavorite: Boolean) {
        personDao.updateFavorite(personId, isFavorite, System.currentTimeMillis())
    }

    fun observeRecentStorageEvents(limit: Int = 10): Flow<List<StorageEventEntity>> {
        return storageEventDao.observeRecentEvents(limit)
    }

    suspend fun getAddressMatchedPeople(
        addressSeed: AddressSeed,
        excludeId: Long,
        excludedIds: Set<Long> = emptySet(),
    ): List<PersonEntity> {
        if (!addressSeed.hasAnyEnteredField()) {
            return emptyList()
        }

        return personDao.getAllPeople()
            .asSequence()
            .filter { it.id != excludeId }
            .filterNot { excludedIds.contains(it.id) }
            .filter { matchesEnteredAddress(it, addressSeed) }
            .sortedBy { it.fullName.lowercase() }
            .toList()
    }

    suspend fun getParentCandidates(
        addressSeed: AddressSeed,
        excludeId: Long,
        requiredGender: String,
        excludedIds: Set<Long> = emptySet(),
    ): List<PersonEntity> {
        if (!addressSeed.hasParentSearchFields()) {
            return emptyList()
        }

        return personDao.getAllPeople()
            .asSequence()
            .filter { it.id != excludeId }
            .filterNot { excludedIds.contains(it.id) }
            .filter { matchesParentAddress(it, addressSeed) }
            .filter { matchesGender(it.gender, requiredGender) }
            .sortedBy { it.fullName.lowercase() }
            .toList()
    }

    suspend fun getSpouseCandidates(
        addressSeed: AddressSeed,
        excludePersonId: Long,
        existingSpouseIds: Set<Long> = emptySet(),
        excludedIds: Set<Long> = emptySet(),
    ): List<PersonEntity> {
        return getAddressMatchedPeople(
            addressSeed = addressSeed,
            excludeId = excludePersonId,
            excludedIds = existingSpouseIds + excludedIds,
        )
    }

    suspend fun getPersonDetail(personId: Long): PersonDetailSnapshot? {
        val person = personDao.getPersonById(personId) ?: return null
        val allPeople = personDao.getAllPeople().associateBy { it.id }
        val relationships = personDao.getParentRelationshipsForChild(personId)
        val parentSlots = resolveParentSlots(relationships)

        return PersonDetailSnapshot(
            person = person,
            father = parentSlots.fatherRelation?.let { allPeople[it.parentId] },
            mother = parentSlots.motherRelation?.let { allPeople[it.parentId] },
            spouses = personDao.getSpousesForPerson(personId),
            children = personDao.getChildrenForParent(personId),
        )
    }

    suspend fun savePerson(draft: PersonDraft): SavePersonResult {
        return database.withTransaction {
            val now = System.currentTimeMillis()
            val existing = draft.id?.let { personDao.getPersonById(it) }
            val person = PersonEntity(
                id = existing?.id ?: 0,
                fullName = draft.fullName.trim(),
                normalizedName = SearchNormalizer.normalizeText(draft.fullName),
                gender = draft.gender.trim(),
                age = draft.age.trim(),
                phoneNumber = draft.phoneNumber.trim(),
                normalizedPhone = SearchNormalizer.normalizePhone(draft.phoneNumber),
                village = draft.village.trim(),
                normalizedVillage = SearchNormalizer.normalizeText(draft.village),
                policeStation = draft.policeStation.trim(),
                postOffice = draft.postOffice.trim(),
                district = draft.district.trim(),
                normalizedDistrict = SearchNormalizer.normalizeText(draft.district),
                state = draft.state.trim(),
                normalizedState = SearchNormalizer.normalizeText(draft.state),
                country = draft.country.trim(),
                notes = draft.notes.trim(),
                isFavorite = existing?.isFavorite ?: false,
                lastViewedAt = existing?.lastViewedAt ?: 0L,
                remoteId = existing?.remoteId,
                syncState = if (existing?.syncState == PersonSyncState.SYNCED.name) {
                    PersonSyncState.PENDING_UPLOAD.name
                } else {
                    existing?.syncState ?: PersonSyncState.LOCAL_ONLY.name
                },
                deletedAt = existing?.deletedAt,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )

            val personId = if (existing == null) {
                personDao.insertPerson(person)
            } else {
                personDao.updatePerson(person)
                existing.id
            }

            val fatherResult = syncParentRelation(
                childId = personId,
                desiredParentId = draft.fatherId,
                relationType = ParentRelationType.FATHER,
            )
            val motherResult = syncParentRelation(
                childId = personId,
                desiredParentId = draft.motherId,
                relationType = ParentRelationType.MOTHER,
            )
            val spouseResult = syncSpouseRelation(
                personId = personId,
                desiredSpouseId = draft.spouseId,
            )
            val warnings = buildList {
                draft.fatherId?.let { fatherId ->
                    val father = personDao.getPersonById(fatherId)
                    LineageValidationEngine.ageGapWarning(
                        parentAge = father?.age.orEmpty(),
                        childAge = draft.age,
                    )?.let(::add)
                }
                draft.motherId?.let { motherId ->
                    val mother = personDao.getPersonById(motherId)
                    LineageValidationEngine.ageGapWarning(
                        parentAge = mother?.age.orEmpty(),
                        childAge = draft.age,
                    )?.let(::add)
                }
            }

            SavePersonResult(
                personId = personId,
                fatherResult = fatherResult,
                motherResult = motherResult,
                spouseResult = spouseResult,
                warnings = warnings.distinct(),
            )
        }
    }

    suspend fun deletePerson(personId: Long) {
        val person = personDao.getPersonById(personId) ?: return
        personDao.deletePerson(person)
    }

    suspend fun setFatherRelation(parentId: Long, childId: Long): RelationAddResult {
        return database.withTransaction {
            setTypedParentRelation(
                childId = childId,
                desiredParentId = parentId,
                relationType = ParentRelationType.FATHER,
            ) ?: RelationAddResult.ALREADY_EXISTS
        }
    }

    suspend fun setMotherRelation(parentId: Long, childId: Long): RelationAddResult {
        return database.withTransaction {
            setTypedParentRelation(
                childId = childId,
                desiredParentId = parentId,
                relationType = ParentRelationType.MOTHER,
            ) ?: RelationAddResult.ALREADY_EXISTS
        }
    }

    suspend fun addSpouseRelation(firstPersonId: Long, secondPersonId: Long): RelationAddResult {
        return database.withTransaction {
            syncSpouseRelation(
                personId = firstPersonId,
                desiredSpouseId = secondPersonId,
            ) ?: RelationAddResult.ALREADY_EXISTS
        }
    }

    suspend fun buildLineageGraph(personId: Long, maxNodes: Int = 120): LineageGraph? {
        val allPeople = personDao.getAllPeople().associateBy { it.id }
        val focusPerson = allPeople[personId] ?: return null
        val parentRelationships = personDao.getAllParentRelationships()
        val spouseRelationships = personDao.getAllSpouseRelationships()

        val parentsByChild = parentRelationships.groupBy { it.childId }
        val childrenByParent = parentRelationships.groupBy { it.parentId }
        val spousesByPerson = buildSpouseAdjacency(spouseRelationships)

        val includedIds = LinkedHashSet<Long>()
        val queue = ArrayDeque<Long>()
        includedIds += focusPerson.id
        queue.add(focusPerson.id)

        while (queue.isNotEmpty() && includedIds.size < maxNodes) {
            val currentId = queue.removeFirst()
            val relatedIds = buildList {
                parentsByChild[currentId]?.forEach { add(it.parentId) }
                childrenByParent[currentId]?.forEach { add(it.childId) }
                spousesByPerson[currentId]?.forEach { add(it) }
            }
            relatedIds.forEach { relatedId ->
                if (allPeople.containsKey(relatedId) && includedIds.add(relatedId) && includedIds.size <= maxNodes) {
                    queue.add(relatedId)
                }
            }
        }

        val generations = assignGenerations(
            focusPersonId = focusPerson.id,
            includedIds = includedIds,
            parentsByChild = parentsByChild,
            childrenByParent = childrenByParent,
            spousesByPerson = spousesByPerson,
        )

        val nodes = includedIds.mapNotNull { includedId ->
            allPeople[includedId]?.toGraphNode(
                generation = generations[includedId] ?: 0,
                isFocus = includedId == focusPerson.id,
            )
        }

        val links = LinkedHashSet<LineageGraphLink>()
        parentRelationships.forEach { relationship ->
            if (includedIds.contains(relationship.parentId) && includedIds.contains(relationship.childId)) {
                links += LineageGraphLink(
                    fromPersonId = relationship.parentId,
                    toPersonId = relationship.childId,
                    type = LineageGraphLinkType.PARENT_CHILD,
                )
            }
        }
        spouseRelationships.forEach { relationship ->
            if (includedIds.contains(relationship.personAId) && includedIds.contains(relationship.personBId)) {
                links += LineageGraphLink(
                    fromPersonId = relationship.personAId,
                    toPersonId = relationship.personBId,
                    type = LineageGraphLinkType.SPOUSE,
                )
            }
        }

        return LineageGraph(
            focusPersonId = focusPerson.id,
            nodes = nodes,
            links = links.toList(),
        )
    }

    suspend fun exportSnapshotJson(): String {
        val root = JSONObject()
        root.put("schemaVersion", SNAPSHOT_SCHEMA_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put(
            "people",
            JSONArray().apply {
                personDao.getAllPeople().forEach { person ->
                    put(
                        JSONObject().apply {
                            put("id", person.id)
                            put("fullName", person.fullName)
                            put("normalizedName", person.normalizedName)
                            put("gender", person.gender)
                            put("age", person.age)
                            put("phoneNumber", person.phoneNumber)
                            put("normalizedPhone", person.normalizedPhone)
                            put("village", person.village)
                            put("normalizedVillage", person.normalizedVillage)
                            put("policeStation", person.policeStation)
                            put("postOffice", person.postOffice)
                            put("district", person.district)
                            put("normalizedDistrict", person.normalizedDistrict)
                            put("state", person.state)
                            put("normalizedState", person.normalizedState)
                            put("country", person.country)
                            put("notes", person.notes)
                            put("isFavorite", person.isFavorite)
                            put("lastViewedAt", person.lastViewedAt)
                            put("remoteId", person.remoteId)
                            put("syncState", person.syncState)
                            put("deletedAt", person.deletedAt)
                            put("createdAt", person.createdAt)
                            put("updatedAt", person.updatedAt)
                        },
                    )
                }
            },
        )
        root.put(
            "parentRelationships",
            JSONArray().apply {
                personDao.getAllParentRelationships().forEach { relationship ->
                    put(
                        JSONObject().apply {
                            put("id", relationship.id)
                            put("parentId", relationship.parentId)
                            put("childId", relationship.childId)
                            put("relationType", relationship.relationType)
                            put("createdAt", relationship.createdAt)
                        },
                    )
                }
            },
        )
        root.put(
            "spouseRelationships",
            JSONArray().apply {
                personDao.getAllSpouseRelationships().forEach { relationship ->
                    put(
                        JSONObject().apply {
                            put("id", relationship.id)
                            put("personAId", relationship.personAId)
                            put("personBId", relationship.personBId)
                            put("status", relationship.status)
                            put("notes", relationship.notes)
                            put("createdAt", relationship.createdAt)
                            put("updatedAt", relationship.updatedAt)
                        },
                    )
                }
            },
        )
        return root.toString()
    }

    suspend fun importSnapshotJson(snapshotJson: String) {
        val root = JSONObject(snapshotJson)
        val people = mutableListOf<PersonEntity>()
        val peopleArray = root.optJSONArray("people") ?: JSONArray()
        for (index in 0 until peopleArray.length()) {
            val item = peopleArray.getJSONObject(index)
            people += PersonEntity(
                id = item.getLong("id"),
                fullName = item.optString("fullName"),
                normalizedName = item.optString("normalizedName", SearchNormalizer.normalizeText(item.optString("fullName"))),
                gender = item.optString("gender"),
                age = item.optString("age"),
                phoneNumber = item.optString("phoneNumber"),
                normalizedPhone = item.optString("normalizedPhone", SearchNormalizer.normalizePhone(item.optString("phoneNumber"))),
                village = item.optString("village"),
                normalizedVillage = item.optString("normalizedVillage", SearchNormalizer.normalizeText(item.optString("village"))),
                policeStation = item.optString("policeStation"),
                postOffice = item.optString("postOffice"),
                district = item.optString("district"),
                normalizedDistrict = item.optString("normalizedDistrict", SearchNormalizer.normalizeText(item.optString("district"))),
                state = item.optString("state"),
                normalizedState = item.optString("normalizedState", SearchNormalizer.normalizeText(item.optString("state"))),
                country = item.optString("country"),
                notes = item.optString("notes"),
                isFavorite = item.optBoolean("isFavorite", false),
                lastViewedAt = item.optLong("lastViewedAt", 0L),
                remoteId = item.optString("remoteId").ifBlank { null },
                syncState = item.optString("syncState", PersonSyncState.LOCAL_ONLY.name),
                deletedAt = item.takeIf { !it.isNull("deletedAt") }?.optLong("deletedAt"),
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = item.optLong("updatedAt", item.optLong("createdAt", System.currentTimeMillis())),
            )
        }

        val parentRelationships = mutableListOf<PersonRelationshipEntity>()
        val parentArray = root.optJSONArray("parentRelationships") ?: JSONArray()
        for (index in 0 until parentArray.length()) {
            val item = parentArray.getJSONObject(index)
            parentRelationships += PersonRelationshipEntity(
                id = item.getLong("id"),
                parentId = item.getLong("parentId"),
                childId = item.getLong("childId"),
                relationType = item.optString("relationType", ParentRelationType.UNKNOWN.name),
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
            )
        }

        val spouseRelationships = mutableListOf<SpouseRelationshipEntity>()
        val spouseArray = root.optJSONArray("spouseRelationships") ?: JSONArray()
        for (index in 0 until spouseArray.length()) {
            val item = spouseArray.getJSONObject(index)
            spouseRelationships += SpouseRelationshipEntity(
                id = item.getLong("id"),
                personAId = item.getLong("personAId"),
                personBId = item.getLong("personBId"),
                status = item.optString("status", SpouseRelationshipStatus.ACTIVE.name),
                notes = item.optString("notes"),
                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = item.optLong("updatedAt", item.optLong("createdAt", System.currentTimeMillis())),
            )
        }

        database.withTransaction {
            personDao.clearSpouseRelationships()
            personDao.clearParentRelationships()
            personDao.clearPeople()
            if (people.isNotEmpty()) {
                personDao.insertPeople(people)
            }
            if (parentRelationships.isNotEmpty()) {
                personDao.insertRelationships(parentRelationships)
            }
            if (spouseRelationships.isNotEmpty()) {
                personDao.insertSpouseRelationships(spouseRelationships)
            }
        }
    }

    private suspend fun syncParentRelation(
        childId: Long,
        desiredParentId: Long?,
        relationType: ParentRelationType,
    ): RelationAddResult? {
        val currentRelation = resolveCurrentParentRelation(
            relationships = personDao.getParentRelationshipsForChild(childId),
            relationType = relationType,
        )

        if (desiredParentId == null) {
            currentRelation?.let { personDao.deleteRelationship(it.parentId, childId) }
            return null
        }

        return setTypedParentRelation(
            childId = childId,
            desiredParentId = desiredParentId,
            relationType = relationType,
            currentRelation = currentRelation,
        )
    }

    private suspend fun setTypedParentRelation(
        childId: Long,
        desiredParentId: Long,
        relationType: ParentRelationType,
        currentRelation: PersonRelationshipEntity? = null,
    ): RelationAddResult? {
        if (desiredParentId == childId) {
            return RelationAddResult.SAME_PERSON
        }
        if (wouldCreateCycle(parentId = desiredParentId, childId = childId)) {
            return RelationAddResult.CYCLE_DETECTED
        }

        val resolvedCurrent = currentRelation ?: resolveCurrentParentRelation(
            relationships = personDao.getParentRelationshipsForChild(childId),
            relationType = relationType,
        )

        if (resolvedCurrent?.parentId == desiredParentId) {
            if (resolvedCurrent.relationType != relationType.name) {
                personDao.updateParentRelationshipType(resolvedCurrent.id, relationType.name)
            }
            return null
        }

        val existingPair = personDao.getParentRelationshipByPair(desiredParentId, childId)
        if (existingPair != null) {
            if (existingPair.relationType != relationType.name) {
                personDao.updateParentRelationshipType(existingPair.id, relationType.name)
            }
            if (resolvedCurrent != null && resolvedCurrent.id != existingPair.id) {
                personDao.deleteRelationship(resolvedCurrent.parentId, childId)
            }
            return null
        }

        if (resolvedCurrent != null) {
            personDao.deleteRelationship(resolvedCurrent.parentId, childId)
        }

        personDao.insertRelationship(
            PersonRelationshipEntity(
                parentId = desiredParentId,
                childId = childId,
                relationType = relationType.name,
            ),
        )
        return RelationAddResult.ADDED
    }

    private suspend fun syncSpouseRelation(
        personId: Long,
        desiredSpouseId: Long?,
    ): RelationAddResult? {
        if (desiredSpouseId == null) {
            return null
        }
        if (personId == desiredSpouseId) {
            return RelationAddResult.SAME_PERSON
        }
        if (personDao.getSpouseIdsForPerson(personId).contains(desiredSpouseId)) {
            return null
        }

        val personAId = minOf(personId, desiredSpouseId)
        val personBId = maxOf(personId, desiredSpouseId)
        val inserted = personDao.insertSpouseRelationship(
            SpouseRelationshipEntity(
                personAId = personAId,
                personBId = personBId,
            ),
        )
        return if (inserted == -1L) {
            RelationAddResult.ALREADY_EXISTS
        } else {
            RelationAddResult.ADDED
        }
    }

    private fun buildSpouseAdjacency(relationships: List<SpouseRelationshipEntity>): Map<Long, List<Long>> {
        val adjacency = mutableMapOf<Long, MutableList<Long>>()
        relationships.forEach { relationship ->
            adjacency.getOrPut(relationship.personAId) { mutableListOf() }.add(relationship.personBId)
            adjacency.getOrPut(relationship.personBId) { mutableListOf() }.add(relationship.personAId)
        }
        return adjacency
    }

    private fun assignGenerations(
        focusPersonId: Long,
        includedIds: Set<Long>,
        parentsByChild: Map<Long, List<PersonRelationshipEntity>>,
        childrenByParent: Map<Long, List<PersonRelationshipEntity>>,
        spousesByPerson: Map<Long, List<Long>>,
    ): Map<Long, Int> {
        val generations = mutableMapOf<Long, Int>()
        val queue = ArrayDeque<Long>()
        generations[focusPersonId] = 0
        queue.add(focusPersonId)

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val currentGeneration = generations[currentId] ?: 0

            spousesByPerson[currentId].orEmpty().forEach { spouseId ->
                if (includedIds.contains(spouseId) && generations.putIfAbsent(spouseId, currentGeneration) == null) {
                    queue.add(spouseId)
                }
            }

            parentsByChild[currentId].orEmpty().forEach { relationship ->
                val parentId = relationship.parentId
                if (includedIds.contains(parentId) && generations.putIfAbsent(parentId, currentGeneration - 1) == null) {
                    queue.add(parentId)
                }
            }

            childrenByParent[currentId].orEmpty().forEach { relationship ->
                val childId = relationship.childId
                if (includedIds.contains(childId) && generations.putIfAbsent(childId, currentGeneration + 1) == null) {
                    queue.add(childId)
                }
            }
        }

        includedIds.forEach { includedId ->
            generations.putIfAbsent(includedId, 0)
        }
        return generations
    }

    private fun matchesEnteredAddress(person: PersonEntity, addressSeed: AddressSeed): Boolean {
        val checks = buildList {
            addressSeed.village.trim().takeIf { it.isNotBlank() }?.let {
                add(matchesIgnoreCase(person.village, it))
            }
            addressSeed.policeStation.trim().takeIf { it.isNotBlank() }?.let {
                add(matchesIgnoreCase(person.policeStation, it))
            }
            addressSeed.postOffice.trim().takeIf { it.isNotBlank() }?.let {
                add(matchesIgnoreCase(person.postOffice, it))
            }
            addressSeed.district.trim().takeIf { it.isNotBlank() }?.let {
                add(matchesIgnoreCase(person.district, it))
            }
            addressSeed.state.trim().takeIf { it.isNotBlank() }?.let {
                add(matchesIgnoreCase(person.state, it))
            }
            addressSeed.country.trim().takeIf { it.isNotBlank() }?.let {
                add(matchesIgnoreCase(person.country, it))
            }
        }
        return checks.isNotEmpty() && checks.all { it }
    }

    private fun matchesParentAddress(person: PersonEntity, addressSeed: AddressSeed): Boolean {
        val checks = buildList {
            addressSeed.village.trim().takeIf { it.isNotBlank() }?.let {
                add(matchesIgnoreCase(person.village, it))
            }
            addressSeed.state.trim().takeIf { it.isNotBlank() }?.let {
                add(matchesIgnoreCase(person.state, it))
            }
            addressSeed.country.trim().takeIf { it.isNotBlank() }?.let {
                add(matchesIgnoreCase(person.country, it))
            }
        }
        return checks.isNotEmpty() && checks.all { it }
    }

    private fun matchesIgnoreCase(left: String, right: String): Boolean {
        return left.trim().equals(right.trim(), ignoreCase = true)
    }

    private fun matchesGender(personGender: String, requiredGender: String): Boolean {
        return personGender.trim().equals(requiredGender.trim(), ignoreCase = true)
    }

    private suspend fun wouldCreateCycle(parentId: Long, childId: Long): Boolean {
        val childrenByParent = personDao.getAllParentRelationships()
            .groupBy { it.parentId }
            .mapValues { entry -> entry.value.map { it.childId } }
        return LineageValidationEngine.wouldCreateParentCycle(
            parentId = parentId,
            childId = childId,
            childrenByParent = childrenByParent,
        )
    }

    private fun resolveCurrentParentRelation(
        relationships: List<PersonRelationshipEntity>,
        relationType: ParentRelationType,
    ): PersonRelationshipEntity? {
        val slots = resolveParentSlots(relationships)
        return when (relationType) {
            ParentRelationType.FATHER -> slots.fatherRelation
            ParentRelationType.MOTHER -> slots.motherRelation
            ParentRelationType.UNKNOWN -> null
        }
    }

    private fun resolveParentSlots(relationships: List<PersonRelationshipEntity>): ParentSlots {
        val fatherRelation = relationships.firstOrNull { it.relationType == ParentRelationType.FATHER.name }
        val motherRelation = relationships.firstOrNull { it.relationType == ParentRelationType.MOTHER.name }
        val unknownRelations = relationships.filter { it.relationType == ParentRelationType.UNKNOWN.name }

        val resolvedFather = fatherRelation ?: unknownRelations.getOrNull(0)
        val resolvedMother = motherRelation ?: unknownRelations.firstOrNull { it.id != resolvedFather?.id }

        return ParentSlots(
            fatherRelation = resolvedFather,
            motherRelation = resolvedMother,
        )
    }

    private fun AddressSeed.hasAnyEnteredField(): Boolean {
        return listOf(village, policeStation, postOffice, district, state).any { it.trim().isNotBlank() }
    }

    private fun AddressSeed.hasParentSearchFields(): Boolean {
        return village.trim().isNotBlank() && state.trim().isNotBlank()
    }

    private fun PersonEntity.toGraphNode(generation: Int, isFocus: Boolean): LineageGraphNode {
        return LineageGraphNode(
            personId = id,
            name = fullName,
            location = shortLocation(fallback = ""),
            generation = generation,
            isFocus = isFocus,
        )
    }

    private data class ParentSlots(
        val fatherRelation: PersonRelationshipEntity?,
        val motherRelation: PersonRelationshipEntity?,
    )

    companion object {
        private const val SNAPSHOT_SCHEMA_VERSION = 2

        @Volatile
        private var INSTANCE: PeopleRepository? = null

        fun getInstance(context: Context): PeopleRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PeopleRepository(AppDatabaseProvider.get(context)).also { INSTANCE = it }
            }
        }
    }
}

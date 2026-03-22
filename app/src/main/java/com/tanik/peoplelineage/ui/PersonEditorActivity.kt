package com.tanik.peoplelineage.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.tanik.peoplelineage.R
import com.tanik.peoplelineage.data.PeopleRepository
import com.tanik.peoplelineage.data.PersonEntity
import com.tanik.peoplelineage.data.StorageSyncManager
import com.tanik.peoplelineage.databinding.ActivityPersonEditorBinding
import com.tanik.peoplelineage.model.AddressSeed
import com.tanik.peoplelineage.model.PersonDraft
import com.tanik.peoplelineage.model.RelationAddResult
import com.tanik.peoplelineage.model.SavePersonResult
import com.tanik.peoplelineage.model.shortLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PersonEditorActivity : ComponentActivity() {

    private lateinit var binding: ActivityPersonEditorBinding
    private lateinit var repository: PeopleRepository

    private var editingPersonId: Long = 0L
    private var preselectedFatherId: Long = 0L
    private var preselectedMotherId: Long = 0L
    private var prefillAddressFromId: Long = 0L
    private var selectedFatherId: Long? = null
    private var selectedFatherPerson: PersonEntity? = null
    private var selectedMotherId: Long? = null
    private var selectedMotherPerson: PersonEntity? = null
    private var selectedSpouseId: Long? = null
    private var selectedSpousePerson: PersonEntity? = null
    private var fatherCandidates: List<PersonEntity> = emptyList()
    private var motherCandidates: List<PersonEntity> = emptyList()
    private var spouseCandidates: List<PersonEntity> = emptyList()
    private var existingSpouseIds: Set<Long> = emptySet()
    private var refreshJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = PeopleRepository.getInstance(this)
        editingPersonId = intent.getLongExtra(EXTRA_PERSON_ID, 0L)
        preselectedFatherId = intent.getLongExtra(EXTRA_PRESELECT_FATHER_ID, 0L)
        preselectedMotherId = intent.getLongExtra(EXTRA_PRESELECT_MOTHER_ID, 0L)
        prefillAddressFromId = intent.getLongExtra(EXTRA_PREFILL_ADDRESS_FROM_ID, 0L)

        binding.toolbar.navigationIcon =
            AppCompatResources.getDrawable(this, androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = getString(if (editingPersonId > 0) R.string.edit_person else R.string.add_person)
        binding.formTitleText.text = binding.toolbar.title

        val genderItems = listOf("", "Male", "Female", getString(R.string.gender_unspecified))
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, genderItems)
        binding.genderDropdown.setAdapter(genderAdapter)

        val stateAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.india_states).toList(),
        )
        binding.stateDropdown.setAdapter(stateAdapter)

        configureDropdowns()
        observeAddressChanges()
        binding.saveButton.setOnClickListener { savePerson() }

        if (editingPersonId > 0) {
            loadExistingPerson()
        } else if (prefillAddressFromId > 0 || preselectedFatherId > 0 || preselectedMotherId > 0) {
            loadPrefillContext()
        } else {
            refreshRelationCandidates()
        }
    }

    private fun configureDropdowns() {
        binding.fatherDropdown.setOnItemClickListener { _, _, position, _ ->
            fatherCandidates.getOrNull(position)?.let {
                applySelectedFather(it)
                refreshRelationCandidates()
            }
        }
        binding.fatherDropdown.setOnClickListener {
            if (fatherCandidates.isNotEmpty()) {
                binding.fatherDropdown.showDropDown()
            }
        }
        binding.fatherDropdown.doAfterTextChanged {
            if (it.isNullOrBlank()) {
                selectedFatherId = null
                selectedFatherPerson = null
                refreshRelationCandidates()
            }
        }

        binding.motherDropdown.setOnItemClickListener { _, _, position, _ ->
            motherCandidates.getOrNull(position)?.let {
                applySelectedMother(it)
                refreshRelationCandidates()
            }
        }
        binding.motherDropdown.setOnClickListener {
            if (motherCandidates.isNotEmpty()) {
                binding.motherDropdown.showDropDown()
            }
        }
        binding.motherDropdown.doAfterTextChanged {
            if (it.isNullOrBlank()) {
                selectedMotherId = null
                selectedMotherPerson = null
                refreshRelationCandidates()
            }
        }

        binding.spouseDropdown.setOnItemClickListener { _, _, position, _ ->
            spouseCandidates.getOrNull(position)?.let {
                applySelectedSpouse(it)
                refreshRelationCandidates()
            }
        }
        binding.spouseDropdown.setOnClickListener {
            if (spouseCandidates.isNotEmpty()) {
                binding.spouseDropdown.showDropDown()
            }
        }
        binding.spouseDropdown.doAfterTextChanged {
            if (it.isNullOrBlank()) {
                selectedSpouseId = null
                selectedSpousePerson = null
                refreshRelationCandidates()
            }
        }
    }

    private fun observeAddressChanges() {
        val watcher: (android.text.Editable?) -> Unit = {
            refreshJob?.cancel()
            refreshJob = lifecycleScope.launch {
                delay(150)
                refreshRelationCandidates()
            }
        }
        binding.villageEditText.doAfterTextChanged(watcher)
        binding.policeStationEditText.doAfterTextChanged(watcher)
        binding.postOfficeEditText.doAfterTextChanged(watcher)
        binding.districtEditText.doAfterTextChanged(watcher)
        binding.stateDropdown.doAfterTextChanged(watcher)
    }

    private fun loadExistingPerson() {
        lifecycleScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                repository.getPersonDetail(editingPersonId)
            } ?: return@launch

            val person = snapshot.person
            binding.nameEditText.setText(person.fullName)
            binding.genderDropdown.setText(person.gender, false)
            binding.ageEditText.setText(person.age)
            binding.phoneEditText.setText(person.phoneNumber)
            binding.villageEditText.setText(person.village)
            binding.policeStationEditText.setText(person.policeStation)
            binding.postOfficeEditText.setText(person.postOffice)
            binding.districtEditText.setText(person.district)
            binding.stateDropdown.setText(person.state, false)
            binding.notesEditText.setText(person.notes)
            existingSpouseIds = snapshot.spouses.map { it.id }.toSet()
            snapshot.father?.let(::applySelectedFather)
            snapshot.mother?.let(::applySelectedMother)
            binding.formSubtitleText.text = getString(R.string.relation_search_helper_initial)
            refreshRelationCandidates(existingSpouseCount = snapshot.spouses.size)
        }
    }

    private fun loadPrefillContext() {
        lifecycleScope.launch {
            val contextPerson = withContext(Dispatchers.IO) {
                when {
                    prefillAddressFromId > 0 -> repository.getPerson(prefillAddressFromId)
                    preselectedFatherId > 0 -> repository.getPerson(preselectedFatherId)
                    preselectedMotherId > 0 -> repository.getPerson(preselectedMotherId)
                    else -> null
                }
            }

            contextPerson?.let { person ->
                binding.villageEditText.setText(person.village)
                binding.policeStationEditText.setText(person.policeStation)
                binding.postOfficeEditText.setText(person.postOffice)
                binding.districtEditText.setText(person.district)
                binding.stateDropdown.setText(person.state, false)
                binding.formSubtitleText.text = getString(
                    R.string.relation_helper_prefilled,
                    person.fullName,
                )
            }

            withContext(Dispatchers.IO) {
                preselectedFatherId.takeIf { it > 0 }?.let { repository.getPerson(it) }
            }?.let(::applySelectedFather)

            withContext(Dispatchers.IO) {
                preselectedMotherId.takeIf { it > 0 }?.let { repository.getPerson(it) }
            }?.let(::applySelectedMother)

            refreshRelationCandidates()
        }
    }

    private fun refreshRelationCandidates(existingSpouseCount: Int = existingSpouseIds.size) {
        lifecycleScope.launch {
            val addressSeed = currentAddressSeed()
            val parentAddressSeed = currentParentAddressSeed()
            val matchedSpouses = withContext(Dispatchers.IO) {
                repository.getSpouseCandidates(
                    addressSeed = addressSeed,
                    excludePersonId = editingPersonId,
                    existingSpouseIds = existingSpouseIds,
                )
            }
            val matchedFathers = withContext(Dispatchers.IO) {
                repository.getParentCandidates(
                    addressSeed = parentAddressSeed,
                    excludeId = editingPersonId,
                    requiredGender = GENDER_MALE,
                )
            }
            val matchedMothers = withContext(Dispatchers.IO) {
                repository.getParentCandidates(
                    addressSeed = parentAddressSeed,
                    excludeId = editingPersonId,
                    requiredGender = GENDER_FEMALE,
                )
            }

            fatherCandidates = buildCandidateList(
                matchedPeople = matchedFathers,
                selectedPerson = selectedFatherPerson,
                excludedIds = setOfNotNull(selectedMotherId, selectedSpouseId),
            )
            motherCandidates = buildCandidateList(
                matchedPeople = matchedMothers,
                selectedPerson = selectedMotherPerson,
                excludedIds = setOfNotNull(selectedFatherId, selectedSpouseId),
            )
            spouseCandidates = buildCandidateList(
                matchedPeople = matchedSpouses,
                selectedPerson = selectedSpousePerson,
                excludedIds = existingSpouseIds + setOfNotNull(selectedFatherId, selectedMotherId),
            )

            bindDropdown(
                people = fatherCandidates,
                selectedPerson = selectedFatherPerson,
                dropdown = binding.fatherDropdown,
            )
            bindDropdown(
                people = motherCandidates,
                selectedPerson = selectedMotherPerson,
                dropdown = binding.motherDropdown,
            )
            bindDropdown(
                people = spouseCandidates,
                selectedPerson = selectedSpousePerson,
                dropdown = binding.spouseDropdown,
            )

            binding.fatherHelperText.text = helperTextFor(
                roleLabel = getString(R.string.father_hint),
                count = fatherCandidates.size,
                hasAddress = parentAddressSeed.hasParentSearchAddress(),
                initialMessageRes = R.string.relation_helper_initial_father,
            )
            binding.motherHelperText.text = helperTextFor(
                roleLabel = getString(R.string.mother_hint),
                count = motherCandidates.size,
                hasAddress = parentAddressSeed.hasParentSearchAddress(),
                initialMessageRes = R.string.relation_helper_initial_mother,
            )
            binding.spouseHelperText.text = when {
                existingSpouseCount > 0 -> getString(R.string.spouse_helper_existing, existingSpouseCount)
                !addressSeed.hasUserEnteredAddress() -> getString(R.string.spouse_helper_initial)
                else -> getString(R.string.spouse_helper_loaded, spouseCandidates.size)
            }
        }
    }

    private fun buildCandidateList(
        matchedPeople: List<PersonEntity>,
        selectedPerson: PersonEntity?,
        excludedIds: Set<Long>,
    ): List<PersonEntity> {
        val candidates = matchedPeople
            .asSequence()
            .filterNot { excludedIds.contains(it.id) }
            .toMutableList()

        if (selectedPerson != null && candidates.none { it.id == selectedPerson.id }) {
            candidates.add(0, selectedPerson)
        }

        return candidates
            .distinctBy { it.id }
            .sortedBy { person ->
                if (selectedPerson?.id == person.id) "" else person.fullName.lowercase()
            }
    }

    private fun bindDropdown(
        people: List<PersonEntity>,
        selectedPerson: PersonEntity?,
        dropdown: com.google.android.material.textfield.MaterialAutoCompleteTextView,
    ) {
        val labels = people.map { person ->
            "${person.fullName} • ${person.shortLocation(getString(R.string.unknown_location))}"
        }
        dropdown.setAdapter(
            ArrayAdapter(
                this@PersonEditorActivity,
                android.R.layout.simple_list_item_1,
                labels,
            ),
        )

        if (selectedPerson != null) {
            dropdown.setText(
                labels.getOrNull(people.indexOfFirst { it.id == selectedPerson.id }).orEmpty(),
                false,
            )
        }
    }

    private fun applySelectedFather(person: PersonEntity) {
        selectedFatherId = person.id
        selectedFatherPerson = person
        binding.fatherDropdown.setText(
            "${person.fullName} • ${person.shortLocation(getString(R.string.unknown_location))}",
            false,
        )
    }

    private fun applySelectedMother(person: PersonEntity) {
        selectedMotherId = person.id
        selectedMotherPerson = person
        binding.motherDropdown.setText(
            "${person.fullName} • ${person.shortLocation(getString(R.string.unknown_location))}",
            false,
        )
    }

    private fun applySelectedSpouse(person: PersonEntity) {
        selectedSpouseId = person.id
        selectedSpousePerson = person
        binding.spouseDropdown.setText(
            "${person.fullName} • ${person.shortLocation(getString(R.string.unknown_location))}",
            false,
        )
    }

    private fun helperTextFor(
        roleLabel: String,
        count: Int,
        hasAddress: Boolean,
        initialMessageRes: Int,
    ): String {
        return if (!hasAddress) {
            getString(initialMessageRes)
        } else {
            getString(R.string.relation_helper_loaded, count, roleLabel.lowercase())
        }
    }

    private fun savePerson() {
        val draft = buildDraft() ?: return
        binding.saveButton.isEnabled = false
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.savePerson(draft)
            }
            withContext(Dispatchers.IO) {
                StorageSyncManager(applicationContext).pushLocalToCloudIfConfigured()
            }
            binding.saveButton.isEnabled = true
            handleSaveResult(result)
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun handleSaveResult(result: SavePersonResult) {
        val messageRes = when {
            result.fatherResult == RelationAddResult.CYCLE_DETECTED ||
                result.motherResult == RelationAddResult.CYCLE_DETECTED -> R.string.relation_cycle
            result.fatherResult == RelationAddResult.SAME_PERSON ||
                result.motherResult == RelationAddResult.SAME_PERSON ||
                result.spouseResult == RelationAddResult.SAME_PERSON -> R.string.relation_invalid
            result.fatherResult == RelationAddResult.ALREADY_EXISTS ||
                result.motherResult == RelationAddResult.ALREADY_EXISTS ||
                result.spouseResult == RelationAddResult.ALREADY_EXISTS -> R.string.relation_exists
            else -> R.string.save_successful
        }
        Toast.makeText(this, getString(messageRes), Toast.LENGTH_SHORT).show()
    }

    private fun buildDraft(): PersonDraft? {
        val name = binding.nameEditText.text?.toString().orEmpty().trim()
        val age = binding.ageEditText.text?.toString().orEmpty().trim()
        val village = binding.villageEditText.text?.toString().orEmpty().trim()
        val policeStation = binding.policeStationEditText.text?.toString().orEmpty().trim()
        val postOffice = binding.postOfficeEditText.text?.toString().orEmpty().trim()
        val district = binding.districtEditText.text?.toString().orEmpty().trim()
        val state = binding.stateDropdown.text?.toString().orEmpty().trim()

        if (name.isBlank()) {
            binding.nameEditText.error = getString(R.string.validation_required, getString(R.string.person_name_hint))
            binding.nameEditText.requestFocus()
            return null
        }
        if (village.isBlank()) {
            binding.villageEditText.error = getString(R.string.validation_required, getString(R.string.village_hint))
            binding.villageEditText.requestFocus()
            return null
        }
        if (state.isBlank()) {
            binding.stateDropdown.error = getString(R.string.validation_required, getString(R.string.state_hint))
            binding.stateDropdown.requestFocus()
            return null
        }
        if (selectedFatherId != null && selectedFatherId == selectedMotherId) {
            binding.motherDropdown.error = getString(R.string.relation_conflict_parents)
            binding.motherDropdown.requestFocus()
            return null
        }
        if (selectedSpouseId != null && (selectedSpouseId == selectedFatherId || selectedSpouseId == selectedMotherId)) {
            binding.spouseDropdown.error = getString(R.string.relation_conflict_spouse_parent)
            binding.spouseDropdown.requestFocus()
            return null
        }

        return PersonDraft(
            id = editingPersonId.takeIf { it > 0 },
            fullName = name,
            gender = binding.genderDropdown.text?.toString().orEmpty().trim(),
            age = age,
            phoneNumber = binding.phoneEditText.text?.toString().orEmpty().trim(),
            village = village,
            policeStation = policeStation,
            postOffice = postOffice,
            district = district,
            state = state,
            country = DEFAULT_COUNTRY,
            notes = binding.notesEditText.text?.toString().orEmpty().trim(),
            fatherId = selectedFatherId,
            motherId = selectedMotherId,
            spouseId = selectedSpouseId,
        )
    }

    private fun currentAddressSeed(): AddressSeed {
        return AddressSeed(
            village = binding.villageEditText.text?.toString().orEmpty(),
            policeStation = binding.policeStationEditText.text?.toString().orEmpty(),
            postOffice = binding.postOfficeEditText.text?.toString().orEmpty(),
            district = binding.districtEditText.text?.toString().orEmpty(),
            state = binding.stateDropdown.text?.toString().orEmpty(),
            country = DEFAULT_COUNTRY,
        )
    }

    private fun currentParentAddressSeed(): AddressSeed {
        return AddressSeed(
            village = binding.villageEditText.text?.toString().orEmpty(),
            state = binding.stateDropdown.text?.toString().orEmpty(),
            country = DEFAULT_COUNTRY,
        )
    }

    private fun AddressSeed.hasUserEnteredAddress(): Boolean {
        return listOf(village, policeStation, postOffice, district, state).any { it.trim().isNotBlank() }
    }

    private fun AddressSeed.hasParentSearchAddress(): Boolean {
        return village.trim().isNotBlank() && state.trim().isNotBlank()
    }

    companion object {
        private const val DEFAULT_COUNTRY = "India"
        private const val GENDER_MALE = "Male"
        private const val GENDER_FEMALE = "Female"
        private const val EXTRA_PERSON_ID = "extra_person_id"
        private const val EXTRA_PRESELECT_FATHER_ID = "extra_preselect_father_id"
        private const val EXTRA_PRESELECT_MOTHER_ID = "extra_preselect_mother_id"
        private const val EXTRA_PREFILL_ADDRESS_FROM_ID = "extra_prefill_address_from_id"

        fun createIntent(
            context: Context,
            personId: Long? = null,
            preselectFatherId: Long? = null,
            preselectMotherId: Long? = null,
            prefillAddressFromId: Long? = null,
        ): Intent {
            return Intent(context, PersonEditorActivity::class.java).apply {
                personId?.let { putExtra(EXTRA_PERSON_ID, it) }
                preselectFatherId?.let { putExtra(EXTRA_PRESELECT_FATHER_ID, it) }
                preselectMotherId?.let { putExtra(EXTRA_PRESELECT_MOTHER_ID, it) }
                prefillAddressFromId?.let { putExtra(EXTRA_PREFILL_ADDRESS_FROM_ID, it) }
            }
        }
    }
}

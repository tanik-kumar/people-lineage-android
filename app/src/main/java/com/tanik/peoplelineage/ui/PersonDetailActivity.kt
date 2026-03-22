package com.tanik.peoplelineage.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.tanik.peoplelineage.R
import com.tanik.peoplelineage.data.PeopleRepository
import com.tanik.peoplelineage.data.PersonEntity
import com.tanik.peoplelineage.data.StorageSyncManager
import com.tanik.peoplelineage.databinding.ActivityPersonDetailBinding
import com.tanik.peoplelineage.model.PersonDetailSnapshot
import com.tanik.peoplelineage.model.RelationAddResult
import com.tanik.peoplelineage.model.fullAddress
import com.tanik.peoplelineage.model.shortLocation
import com.tanik.peoplelineage.model.toAddressSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class PersonDetailActivity : ComponentActivity() {

    private lateinit var binding: ActivityPersonDetailBinding
    private lateinit var repository: PeopleRepository
    private lateinit var fatherAdapter: RelationPersonAdapter
    private lateinit var motherAdapter: RelationPersonAdapter
    private lateinit var spousesAdapter: RelationPersonAdapter
    private lateinit var childrenAdapter: RelationPersonAdapter

    private var personId: Long = 0L
    private var currentSnapshot: PersonDetailSnapshot? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = PeopleRepository.getInstance(this)
        personId = intent.getLongExtra(EXTRA_PERSON_ID, 0L)

        fatherAdapter = RelationPersonAdapter { person ->
            startActivity(createIntent(this, person.id))
        }
        motherAdapter = RelationPersonAdapter { person ->
            startActivity(createIntent(this, person.id))
        }
        spousesAdapter = RelationPersonAdapter { person ->
            startActivity(createIntent(this, person.id))
        }
        childrenAdapter = RelationPersonAdapter { person ->
            startActivity(createIntent(this, person.id))
        }

        binding.fatherRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PersonDetailActivity)
            adapter = fatherAdapter
        }
        binding.motherRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PersonDetailActivity)
            adapter = motherAdapter
        }
        binding.childrenRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PersonDetailActivity)
            adapter = childrenAdapter
        }
        binding.spousesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PersonDetailActivity)
            adapter = spousesAdapter
        }

        binding.toolbar.navigationIcon =
            AppCompatResources.getDrawable(this, androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.editButton.setOnClickListener {
            startActivity(PersonEditorActivity.createIntent(this, personId = personId))
        }
        binding.graphButton.setOnClickListener {
            startActivity(HierarchyActivity.createIntent(this, personId))
        }
        binding.addSuccessorButton.setOnClickListener {
            startActivity(buildSuccessorIntent())
        }
        binding.linkFatherButton.setOnClickListener { showFatherPicker() }
        binding.linkMotherButton.setOnClickListener { showMotherPicker() }
        binding.linkSpouseButton.setOnClickListener { showSpousePicker() }
        binding.shareDetailsButton.setOnClickListener { sharePersonDetails() }
        binding.callButton.setOnClickListener {
            openDialer(currentSnapshot?.person?.phoneNumber.orEmpty())
        }
        binding.whatsappButton.setOnClickListener {
            currentSnapshot?.person?.let(::openWhatsApp)
        }
        binding.deleteButton.setOnClickListener { confirmDelete() }
    }

    override fun onResume() {
        super.onResume()
        loadDetail()
    }

    private fun loadDetail() {
        lifecycleScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                repository.markPersonViewed(personId)
                repository.getPersonDetail(personId)
            }
            if (snapshot == null) {
                finish()
                return@launch
            }
            currentSnapshot = snapshot
            renderSnapshot(snapshot)
        }
    }

    private fun renderSnapshot(snapshot: PersonDetailSnapshot) {
        val person = snapshot.person
        binding.toolbar.title = person.fullName
        binding.personNameText.text = person.fullName
        binding.relationCountText.text = getString(
            R.string.detail_counts,
            listOfNotNull(snapshot.father, snapshot.mother).size,
            snapshot.spouses.size,
            snapshot.children.size,
        )
        binding.addressText.text = person.fullAddress(getString(R.string.unknown_location))
        binding.ageText.text = if (person.age.isBlank()) {
            getString(R.string.detail_age_label) + ": " + getString(R.string.age_not_available)
        } else {
            getString(R.string.detail_age_label) + ": " + person.age
        }
        binding.phoneText.text = if (person.phoneNumber.isBlank()) {
            getString(R.string.detail_phone_label) + ": " + getString(R.string.phone_not_available)
        } else {
            getString(R.string.detail_phone_label) + ": " + person.phoneNumber
        }
        binding.contactActionsRow.isVisible = toDialablePhoneNumber(person.phoneNumber) != null
        binding.notesText.text = if (person.notes.isBlank()) {
            getString(R.string.unknown_notes)
        } else {
            person.notes
        }

        fatherAdapter.submitList(snapshot.father?.let(::listOf) ?: emptyList())
        motherAdapter.submitList(snapshot.mother?.let(::listOf) ?: emptyList())
        spousesAdapter.submitList(snapshot.spouses)
        childrenAdapter.submitList(snapshot.children)

        binding.fatherEmptyText.isVisible = snapshot.father == null
        binding.motherEmptyText.isVisible = snapshot.mother == null
        binding.spousesEmptyText.isVisible = snapshot.spouses.isEmpty()
        binding.childrenEmptyText.isVisible = snapshot.children.isEmpty()
        binding.fatherRecyclerView.isVisible = snapshot.father != null
        binding.motherRecyclerView.isVisible = snapshot.mother != null
        binding.spousesRecyclerView.isVisible = snapshot.spouses.isNotEmpty()
        binding.childrenRecyclerView.isVisible = snapshot.children.isNotEmpty()
    }

    private fun buildSuccessorIntent(): Intent {
        val snapshot = currentSnapshot
        val gender = snapshot?.person?.gender.orEmpty().trim().lowercase()
        return when {
            gender == "male" -> PersonEditorActivity.createIntent(
                context = this,
                preselectFatherId = personId,
                prefillAddressFromId = personId,
            )
            gender == "female" -> PersonEditorActivity.createIntent(
                context = this,
                preselectMotherId = personId,
                prefillAddressFromId = personId,
            )
            else -> PersonEditorActivity.createIntent(
                context = this,
                prefillAddressFromId = personId,
            )
        }
    }

    private fun showFatherPicker() {
        val snapshot = currentSnapshot ?: return
        lifecycleScope.launch {
            val candidates = withContext(Dispatchers.IO) {
                repository.getParentCandidates(
                    addressSeed = snapshot.person.toParentSearchSeed(),
                    excludeId = snapshot.person.id,
                    requiredGender = GENDER_MALE,
                    excludedIds = buildSet {
                        snapshot.mother?.id?.let(::add)
                        addAll(snapshot.spouses.map { it.id })
                    },
                )
            }
            showRelationPicker(
                candidates = candidates,
                titleRes = R.string.select_father_title,
                onSelected = ::addFatherRelation,
            )
        }
    }

    private fun showMotherPicker() {
        val snapshot = currentSnapshot ?: return
        lifecycleScope.launch {
            val candidates = withContext(Dispatchers.IO) {
                repository.getParentCandidates(
                    addressSeed = snapshot.person.toParentSearchSeed(),
                    excludeId = snapshot.person.id,
                    requiredGender = GENDER_FEMALE,
                    excludedIds = buildSet {
                        snapshot.father?.id?.let(::add)
                        addAll(snapshot.spouses.map { it.id })
                    },
                )
            }
            showRelationPicker(
                candidates = candidates,
                titleRes = R.string.select_mother_title,
                onSelected = ::addMotherRelation,
            )
        }
    }

    private fun showRelationPicker(
        candidates: List<PersonEntity>,
        titleRes: Int,
        onSelected: (PersonEntity) -> Unit,
    ) {
        if (candidates.isEmpty()) {
            Snackbar.make(binding.root, getString(R.string.no_matching_people), Snackbar.LENGTH_SHORT).show()
            return
        }

        val labels = candidates.map { candidate ->
            "${candidate.fullName} • ${candidate.shortLocation(getString(R.string.unknown_location))}"
        }.toTypedArray()

        MaterialAlertDialogBuilder(this@PersonDetailActivity)
            .setTitle(titleRes)
            .setItems(labels) { _, which ->
                onSelected(candidates[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun addFatherRelation(parent: PersonEntity) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.setFatherRelation(parentId = parent.id, childId = personId)
            }
            showRelationResult(result)
        }
    }

    private fun addMotherRelation(parent: PersonEntity) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.setMotherRelation(parentId = parent.id, childId = personId)
            }
            showRelationResult(result)
        }
    }

    private fun showRelationResult(result: RelationAddResult) {
        val messageRes = when (result) {
            RelationAddResult.ADDED -> R.string.relation_added
            RelationAddResult.ALREADY_EXISTS -> R.string.relation_exists
            RelationAddResult.CYCLE_DETECTED -> R.string.relation_cycle
            RelationAddResult.SAME_PERSON -> R.string.relation_invalid
        }
        Snackbar.make(binding.root, getString(messageRes), Snackbar.LENGTH_SHORT).show()
        if (result == RelationAddResult.ADDED) {
            lifecycleScope.launch(Dispatchers.IO) {
                StorageSyncManager(applicationContext).pushLocalToCloudIfConfigured()
            }
            loadDetail()
        }
    }

    private fun showSpousePicker() {
        val snapshot = currentSnapshot ?: return
        lifecycleScope.launch {
            val candidates = withContext(Dispatchers.IO) {
                repository.getSpouseCandidates(
                    addressSeed = snapshot.person.toAddressSeed(),
                    excludePersonId = snapshot.person.id,
                    existingSpouseIds = snapshot.spouses.map { it.id }.toSet(),
                    excludedIds = setOfNotNull(snapshot.father?.id, snapshot.mother?.id),
                )
            }

            if (candidates.isEmpty()) {
                Snackbar.make(binding.root, getString(R.string.no_spouse_candidates), Snackbar.LENGTH_SHORT).show()
                return@launch
            }

            val labels = candidates.map { candidate ->
                "${candidate.fullName} • ${candidate.shortLocation(getString(R.string.unknown_location))}"
            }.toTypedArray()

            MaterialAlertDialogBuilder(this@PersonDetailActivity)
                .setTitle(R.string.select_spouse_title)
                .setItems(labels) { _, which ->
                    val selected = candidates[which]
                    addSpouseRelation(selected)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun addSpouseRelation(spouse: PersonEntity) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.addSpouseRelation(firstPersonId = personId, secondPersonId = spouse.id)
            }
            showRelationResult(result)
        }
    }

    private fun sharePersonDetails() {
        val snapshot = currentSnapshot ?: return
        val shareText = buildString {
            appendLine(snapshot.person.fullName)
            appendLine(snapshot.person.fullAddress(getString(R.string.unknown_location)))
            appendLine("${getString(R.string.detail_age_label)}: ${snapshot.person.age.ifBlank { getString(R.string.age_not_available) }}")
            appendLine("${getString(R.string.detail_phone_label)}: ${snapshot.person.phoneNumber.ifBlank { getString(R.string.phone_not_available) }}")
            appendLine()
            appendLine("${getString(R.string.detail_father_title)}: ${snapshot.father?.fullName ?: getString(R.string.not_linked)}")
            appendLine("${getString(R.string.detail_mother_title)}: ${snapshot.mother?.fullName ?: getString(R.string.not_linked)}")
            appendLine("${getString(R.string.detail_spouses_title)}: ${formatPeopleList(snapshot.spouses)}")
            appendLine("${getString(R.string.detail_children_title)}: ${formatPeopleList(snapshot.children)}")
            appendLine()
            appendLine("${getString(R.string.detail_notes_label)}: ${snapshot.person.notes.ifBlank { getString(R.string.unknown_notes) }}")
        }.trim()

        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_details_subject, snapshot.person.fullName))
                    putExtra(Intent.EXTRA_TEXT, shareText)
                },
                getString(R.string.share_details),
            ),
        )
    }

    private fun openDialer(rawPhoneNumber: String) {
        val dialablePhoneNumber = toDialablePhoneNumber(rawPhoneNumber)
        if (dialablePhoneNumber == null) {
            val messageRes = if (rawPhoneNumber.isBlank()) {
                R.string.phone_action_unavailable
            } else {
                R.string.phone_number_invalid
            }
            Snackbar.make(binding.root, getString(messageRes), Snackbar.LENGTH_SHORT).show()
            return
        }

        startActivity(
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(dialablePhoneNumber)}")),
        )
    }

    private fun openWhatsApp(person: PersonEntity) {
        val whatsappPhoneNumber = toWhatsAppPhoneNumber(person.phoneNumber)
        if (whatsappPhoneNumber == null) {
            val messageRes = if (person.phoneNumber.isBlank()) {
                R.string.phone_action_unavailable
            } else {
                R.string.phone_number_invalid
            }
            Snackbar.make(binding.root, getString(messageRes), Snackbar.LENGTH_SHORT).show()
            return
        }

        val encodedMessage = URLEncoder.encode(
            getString(R.string.whatsapp_message_template, person.fullName),
            StandardCharsets.UTF_8.toString(),
        )

        val directIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("whatsapp://send?phone=$whatsappPhoneNumber&text=$encodedMessage"),
        ).apply {
            `package` = WHATSAPP_PACKAGE
        }
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/$whatsappPhoneNumber?text=$encodedMessage"),
        )

        when {
            directIntent.resolveActivity(packageManager) != null -> startActivity(directIntent)
            webIntent.resolveActivity(packageManager) != null -> startActivity(webIntent)
            else -> Snackbar.make(binding.root, getString(R.string.whatsapp_unavailable), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun toDialablePhoneNumber(rawPhoneNumber: String): String? {
        val trimmedNumber = rawPhoneNumber.trim()
        val digits = trimmedNumber.filter(Char::isDigit)
        if (digits.length < MIN_PHONE_DIGITS) {
            return null
        }
        return if (trimmedNumber.startsWith("+")) {
            "+$digits"
        } else {
            digits
        }
    }

    private fun toWhatsAppPhoneNumber(rawPhoneNumber: String): String? {
        val trimmedNumber = rawPhoneNumber.trim()
        val digits = trimmedNumber.filter(Char::isDigit)
        if (digits.length < MIN_PHONE_DIGITS) {
            return null
        }
        return when {
            trimmedNumber.startsWith("+") -> digits
            digits.length == INDIA_LOCAL_PHONE_LENGTH -> INDIA_COUNTRY_CODE + digits
            else -> digits
        }
    }

    private fun formatPeopleList(people: List<PersonEntity>): String {
        return if (people.isEmpty()) {
            getString(R.string.not_linked)
        } else {
            people.joinToString(", ") { it.fullName }
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(R.string.confirm_delete_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                deletePerson()
            }
            .show()
    }

    private fun deletePerson() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                repository.deletePerson(personId)
                StorageSyncManager(applicationContext).pushLocalToCloudIfConfigured()
            }
            android.widget.Toast.makeText(
                this@PersonDetailActivity,
                getString(R.string.delete_successful),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
            finish()
        }
    }

    companion object {
        private const val EXTRA_PERSON_ID = "extra_person_id"
        private const val GENDER_MALE = "Male"
        private const val GENDER_FEMALE = "Female"
        private const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val MIN_PHONE_DIGITS = 6
        private const val INDIA_LOCAL_PHONE_LENGTH = 10
        private const val INDIA_COUNTRY_CODE = "91"

        fun createIntent(context: Context, personId: Long): Intent {
            return Intent(context, PersonDetailActivity::class.java).apply {
                putExtra(EXTRA_PERSON_ID, personId)
            }
        }
    }

    private fun PersonEntity.toParentSearchSeed() = toAddressSeed().copy(
        policeStation = "",
        postOffice = "",
        district = "",
    )
}

package com.tanik.peoplelineage.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tanik.peoplelineage.data.PersonEntity
import com.tanik.peoplelineage.databinding.ItemPersonBinding
import com.tanik.peoplelineage.model.shortLocation

class PeopleAdapter(
    private val onPersonClicked: (PersonEntity) -> Unit,
) : ListAdapter<PersonEntity, PeopleAdapter.PersonViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val binding = ItemPersonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return PersonViewHolder(binding, onPersonClicked)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PersonViewHolder(
        private val binding: ItemPersonBinding,
        private val onPersonClicked: (PersonEntity) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(person: PersonEntity) {
            binding.nameText.text = person.fullName
            binding.addressText.text = person.shortLocation(
                fallback = binding.root.context.getString(com.tanik.peoplelineage.R.string.unknown_location),
            )
            val meta = when {
                person.phoneNumber.isNotBlank() -> "${binding.root.context.getString(com.tanik.peoplelineage.R.string.detail_phone_label)}: ${person.phoneNumber}"
                person.gender.isNotBlank() -> person.gender
                else -> ""
            }
            binding.metaText.isVisible = meta.isNotBlank()
            binding.metaText.text = meta
            binding.root.setOnClickListener { onPersonClicked(person) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PersonEntity>() {
        override fun areItemsTheSame(oldItem: PersonEntity, newItem: PersonEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PersonEntity, newItem: PersonEntity): Boolean {
            return oldItem == newItem
        }
    }
}

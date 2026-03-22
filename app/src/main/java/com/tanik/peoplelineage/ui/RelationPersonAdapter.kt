package com.tanik.peoplelineage.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tanik.peoplelineage.R
import com.tanik.peoplelineage.data.PersonEntity
import com.tanik.peoplelineage.databinding.ItemRelationPersonBinding
import com.tanik.peoplelineage.model.shortLocation

class RelationPersonAdapter(
    private val onPersonClicked: (PersonEntity) -> Unit,
) : ListAdapter<PersonEntity, RelationPersonAdapter.RelationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelationViewHolder {
        val binding = ItemRelationPersonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return RelationViewHolder(binding, onPersonClicked)
    }

    override fun onBindViewHolder(holder: RelationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RelationViewHolder(
        private val binding: ItemRelationPersonBinding,
        private val onPersonClicked: (PersonEntity) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(person: PersonEntity) {
            binding.nameText.text = person.fullName
            binding.addressText.text = person.shortLocation(
                fallback = binding.root.context.getString(R.string.unknown_location),
            )
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

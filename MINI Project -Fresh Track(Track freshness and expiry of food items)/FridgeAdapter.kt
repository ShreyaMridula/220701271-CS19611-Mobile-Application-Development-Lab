package com.example.fridgemanager1

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater


class FridgeAdapter(private val itemList: MutableList<FridgeItem>,
                    private val onItemClick: (Int) -> Unit):  // For updating) :
    RecyclerView.Adapter<FridgeAdapter.FridgeViewHolder>() {

    class FridgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.itemNameText)
        val itemQuantity:TextView=itemView.findViewById(R.id.itemQuantityText)
        val itemExpiry: TextView = itemView.findViewById(R.id.itemDateText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FridgeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fridge, parent, false)
        return FridgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: FridgeViewHolder, position: Int) {
        val item = itemList[position]
        holder.itemName.text = item.name
        holder.itemQuantity.text=item.quantity
        holder.itemExpiry.text = item.expiryDate
        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
        holder.itemView.setOnClickListener {
            onItemClick(holder.adapterPosition)
        }

    }

    override fun getItemCount(): Int = itemList.size
}

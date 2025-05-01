package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.Expense
import java.text.SimpleDateFormat
import java.util.Locale

// DiffUtil callback
class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
    override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
        return oldItem == newItem
    }
}

// Adapter class
class ExpenseAdapter : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryTextView: TextView = itemView.findViewById(R.id.textViewCategory)
        private val amountTextView: TextView = itemView.findViewById(R.id.textViewAmount)
        private val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)

        fun bind(expense: Expense) {
            categoryTextView.text = expense.category
            amountTextView.text = "R ${expense.amount}"

            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val parsedDate = inputFormat.parse(expense.date)
                val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                dateTextView.text = parsedDate?.let { outputFormat.format(it) } ?: expense.date
            } catch (e: Exception) {
                dateTextView.text = expense.date
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = getItem(position)
        holder.bind(expense)
    }

    // To update data, call submitList(newList)
    fun updateData(newExpenses: List<Expense>) {
        submitList(newExpenses)
    }
}
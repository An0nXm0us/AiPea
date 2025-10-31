package vcmsa.projects.wilproject.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vcmsa.projects.wilproject.databinding.ItemScoreBinding
import vcmsa.projects.wilproject.models.ScoreEntity
import java.text.DecimalFormat

class ScoresAdapter : ListAdapter<ScoreEntity, ScoresAdapter.ScoreViewHolder>(ScoreDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val binding = ItemScoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ScoreViewHolder(private val binding: ItemScoreBinding) : RecyclerView.ViewHolder(binding.root) {
        private val decimalFormat = DecimalFormat("0.0")

        fun bind(score: ScoreEntity) {
            binding.tvTopic.text = "Topic: ${score.topic}"
            binding.tvScoreDetails.text = "Difficulty: ${score.difficulty} | Correct: ${score.correctAnswers}/${score.totalQuestions}"

            val percentageText = if (score.percentage != null) {
                "${decimalFormat.format(score.percentage)}%"
            } else {
                "N/A"
            }
            binding.tvScorePercentage.text = percentageText
        }
    }

    private class ScoreDiffCallback : DiffUtil.ItemCallback<ScoreEntity>() {
        override fun areItemsTheSame(oldItem: ScoreEntity, newItem: ScoreEntity): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: ScoreEntity, newItem: ScoreEntity): Boolean {
            return oldItem == newItem
        }
    }
}

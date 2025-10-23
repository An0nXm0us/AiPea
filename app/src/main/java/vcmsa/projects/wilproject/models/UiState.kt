package vcmsa.projects.wilproject.models



sealed class UiState {
    data object Welcome : UiState()
    data object Empty : UiState()
    data object Loading : UiState()
    data class Success(val books: List<Book>) : UiState()
    data class Error(val message: String) : UiState()
}

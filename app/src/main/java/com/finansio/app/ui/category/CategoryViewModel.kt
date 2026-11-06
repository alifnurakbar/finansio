package com.finansio.app.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finansio.app.data.local.entity.CategoryEntity
import com.finansio.app.data.repository.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CategoryViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _showAddCategoryEvent = MutableSharedFlow<Unit>()
    val showAddCategoryEvent = _showAddCategoryEvent.asSharedFlow()

    fun triggerAddCategory() {
        viewModelScope.launch {
            _showAddCategoryEvent.emit(Unit)
        }
    }
    
    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            if (category.id == 0) {
                categoryRepository.insert(category)
            } else {
                categoryRepository.update(category)
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.delete(category)
        }
    }
}

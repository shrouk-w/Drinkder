package com.example.drinkder.ui.searchpage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SearchPageViewModel: ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Search Thingy Fragment"
    }
    val text: LiveData<String> = _text
}
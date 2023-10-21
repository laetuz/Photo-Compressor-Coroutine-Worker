package com.neotica.workmanagerdemo.di

import com.neotica.workmanagerdemo.PhotoViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModules = module {
    viewModel {
        PhotoViewModel(get())
    }
}
package com.papersama.filmvault

import android.app.Application
import com.papersama.filmvault.data.FilmVaultDatabase
import com.papersama.filmvault.reminder.FilmReminder

class FilmVaultApplication : Application() {
    val database: FilmVaultDatabase by lazy { FilmVaultDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        FilmReminder.createChannel(this)
    }
}

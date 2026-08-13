package com.papersama.filmvault

import android.app.Application
import com.papersama.filmvault.data.FilmVaultDatabase

class FilmVaultApplication : Application() {
    val database: FilmVaultDatabase by lazy { FilmVaultDatabase(this) }
}

package com.example.tiktokdownloaderci.ui.downloads

import androidx.lifecycle.LiveData
import com.example.tiktokdownloaderci.database.AppDatabase
import com.example.tiktokdownloaderci.database.Download
import android.app.Application
import androidx.lifecycle.AndroidViewModel

//Clase donde se guarda toda la información de la vista que queremos que sea persistente, incluso cuando se destruya la vista.
//Esta información solo se mantendrá durante la ejecución de la aplicación
class DownloadsViewModel(application: Application) : AndroidViewModel(application) {
    //Variable donde se guarda la lista de Descargas obtenida de la base de datos
    val allDownloads: LiveData<List<Download>>

    init {
        //Se obtiene el Dao para la instancia de la base de datos
        val downloadDao = AppDatabase.getDatabase(application).DownloadDao()
        //Se guarda la lista de descargas obtenida de la base de datos en la variable allDownloads
        allDownloads = downloadDao.getAll()
    }


}
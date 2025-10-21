package com.example.tiktokdownloaderci.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.yausername.youtubedl_android.mapper.VideoInfo

//Clase donde se guarda toda la información de la vista que queremos que sea persistente, incluso cuando se destruya la vista.
//Esta información solo se mantendrá durante la ejecución de la aplicación
class HomeViewModel : ViewModel() {
    //Variable donde se guarda el URL del video/audio
    var URL: String = ""

    //Variable donde se guarda la información del video/audio
    var mediaInformation: VideoInfo? = null

    //Variable donde se guarda el estado de visibilidad de los botones de descarga
    var buttonLayoutVisible: Boolean = false

    //Variable donde se guarda el estado del circulo de carga
    val loadState: MutableLiveData<LoadState> = MutableLiveData(LoadState.INITIAL)
}

enum class LoadState{
    INITIAL, LOADING, LOADED
}
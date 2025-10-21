package com.example.tiktokdownloaderci.app

import android.app.Application
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.example.tiktokdownloaderci.R

private const val Application_TAG = "Application"
//Clase 'Application', la cual se crea cuando inicia la aplicación y se utiliza para inicializar las herramientas necesarias para el funcionamiento de la aplicación.
class Application: Application() {
    //Función que se manda a llamar cuando la aplicación esta iniciando, antes de cualquier actividad o servicio.
    override fun onCreate() {
        super.onCreate()

        //Se obtiene una instancia de SharedPreferences que apunta al archivo por defecto donde se guardan las preferencias globales de la aplicaciones.
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this@Application)
        //Se obtiene el valor de la preferencia donde se guarda si el usuario quiere usar el tema oscuro o no.
        //En caso de que no exista aun la preferencia, se obtiene el valor por defecto false
        val darkTheme = sharedPrefs.getBoolean(getString(R.string.dark_theme_key), false)

        //If que verifica si el valor de la preferencia 'dark_theme_key' es verdadero o falso
        if(darkTheme){
            //En caso de ser verdadero, se cambia el tema de la aplicación por un modo oscuro
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES
            )
        }else {
            //En caso de ser falso, se verifica si el usuario tiene una versión inferior a Android 10 o superior o igual a Android 10
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                //En caso de tener una versión inferior a Android 10, se pone la aplicación en un modo oscuro pero solo cuando el modo de 'ahorro de bateria' esta activado
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                )
            } else {
                //En caso de tener una versión superior o igual a Android 10, se pone la aplicación en un modo oscuro pero solo cuando esta indicado el 'modo oscuro' por el sistema del usuario
                AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                )
            }
        }

        try{
            //Se inicializa yt-dlp al abrir la aplicación
            YoutubeDL.getInstance().init(this@Application)
            //Se inicializa ffmpeg de yt-dlp al abrir la aplicación
            FFmpeg.getInstance().init(this@Application)
            //Se inicializa Aria2c de yt-dlp al abrir la aplicación
            Aria2c.getInstance().init(this@Application)
        }catch(e: Exception){
            Log.e(Application_TAG, "The exception caught while executing the process. (e)")
            e.printStackTrace()
            //Se muestra un mensaje si ocurrio un error al tratar de inicializar yt-dlp
            Toast.makeText(
                applicationContext,
                R.string.initialization_fail,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
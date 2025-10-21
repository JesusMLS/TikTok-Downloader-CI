package com.example.tiktokdownloaderci.work

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.work.WorkManager
import com.yausername.youtubedl_android.YoutubeDL
import com.example.tiktokdownloaderci.R

private const val TAG = "CancelReceiver"

//Clase 'CancelReceiver', la cual se utilizará para especificar las accciones que se van a realizar cuando se presione el boton 'Cancel'
class CancelReceiver : BroadcastReceiver() {
    //Esta función se va a ejecutar cuando el usuario presione el boton de 'Cancel' en la notificación.
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.e(TAG, "A task cancellation process has started.")

        //En el caso de que el parámetro context sea nulo, se va a regresar sin ejecutar el código.
        if(context == null){
            return
        }

        //En el caso de que el parámetro intent sea nulo, se va a regresar sin ejecutar el código.
        if(intent == null){
            return
        }

        //Se obtienen datos del Intent que se inicializo en la clase 'DownloadWorker'
        val taskId = intent.getStringExtra("taskId")
        val notificationId = intent.getIntExtra("notificationId", 0)

        //En el caso de que el dato 'taskId' sea nulo o vacio se regresa sin ejecutar el código.
        if(taskId.isNullOrEmpty()){
            return
        }

        //Variable en la que se guarda el resultado de la tarea para cancelar el proceso generado por la herramienta 'YoutubeDL'.
        val cancelResult = YoutubeDL.getInstance().destroyProcessById(taskId)

        //If para condicionar el resultado de la tarea anterior
        //Donde en caso de que se completará satisfactoriamente la tarea, se ejecuta el código dentro del if
        if(cancelResult){
            Log.e(TAG, "Task with id: $taskId was killed.")
            //Se cancela el trabajo en segundo plano con el taskId especificado.
            WorkManager.getInstance(context).cancelAllWorkByTag(taskId)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as
                        NotificationManager?
           //Se le manda un Toast al usuario especificando que la descarga ha sido cancelada.
            Toast.makeText(
                context,
                R.string.download_task_killed,
                Toast.LENGTH_LONG
            ).show()

            //Se cancela la notificación especificada por el notificationId.
            notificationManager?.cancel(notificationId)
        }
    }
}
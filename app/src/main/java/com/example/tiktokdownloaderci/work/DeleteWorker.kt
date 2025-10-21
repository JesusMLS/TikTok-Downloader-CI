package com.example.tiktokdownloaderci.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.tiktokdownloaderci.R
import com.example.tiktokdownloaderci.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "DeleteWorker"

//Clase 'DeleteWorker', la cual se utilizará para inicializar una nueva petición de trabajo para la cola de trabajo.
//El proposito de esta clase 'ListenableWorker' es el eliminar un contenido de manera asincrona.
class DeleteWorker(appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    //Se obtiene el servicio de notificaciones del dispositivo del Usuario.
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager?

    override suspend fun doWork(): Result {
        try{
            //Se obtiene el id del contenido a eliminar
            val contentId = inputData.getLong("contentIdKey", 0)

            //Se crea el canal de notificaciones, necesario para mostrar notificaciones a partir de Android Oreo (8)
            createNotificationChannel()
            //Se guarda en una variable el id que tendran las notificaciones de este trabajo en específico
            val notificationId = id.hashCode()
            //Se inicializa la notificación que le indicará al usuario que la eliminación del contenido esta iniciando
            val notification = NotificationCompat.Builder(applicationContext, "tiktok_delete")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_stat_tdci)
                .setContentTitle(applicationContext.getString(R.string.content_deletion_notification_title))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(applicationContext.getString(R.string.content_deletion_notification_text_1))
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                notification.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            }
            //Se construye la notificación inicializada, pero en otra variable.
            val notificationB = notification.build()

            //Se crea una instancia de 'ForegroundInfo' la cual contendrá la notificación que creamos anteriormente 'notification' y el id que se le dará a la notificación
            //En el caso de Android 14, es necesario especificar el tipo de Foreground Service dentro de la instancia de 'ForegroundInfo'
            val foregroundInfo : ForegroundInfo = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ForegroundInfo(
                    notificationId,
                    notificationB,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                ForegroundInfo(notificationId, notificationB)
            }

            //Se convierte la notificación en un 'ForegroundService' y se lanza
            //Esto con el propósito de mostrarle al usuario el progreso de la eliminación en segundo plano
            setForegroundAsync(foregroundInfo)

            //Se inicia el proceso de eliminación del contenido especificado por el Id.
            val downloadDao = AppDatabase.getDatabase(applicationContext).DownloadDao()
            val download = downloadDao.getById(contentId)
            //Se elimina la entrada del contenido de la bd
            downloadDao.delete(download)
            //Se guarda la ubicación del archivo en una variable filePath
            val filePath = download.downloadedPath
            val file = File(filePath.trim())
            //Se verifica si el archivo existe en el directorio
            if(!file.exists()){
                //En caso de no existir en el directorio, se le indica al usuario que la entrada en la bd ya se elimino
                withContext(Dispatchers.Main){
                    Toast.makeText(
                        applicationContext,
                        R.string.file_not_found_2,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                val notificationDelete = NotificationCompat.Builder(applicationContext, "tiktok_delete")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_stat_tdci)
                    .setContentTitle(applicationContext.getString(R.string.content_deletion_notification_title))
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(applicationContext.getString(R.string.content_deletion_notification_text_2))
                    )
                    .build()
                notificationManager?.notify(notificationId, notificationDelete)
            } else {
                //En caso de si existir en el directorio, se verifica si se puede escribir en el archivo
                if (file.canWrite()) {
                    //En caso de poder escribir en el archivo, se procede a eliminar y se le indica al usuario que se elimino del dispositivo el archivo.
                        file.delete()
                        withContext(Dispatchers.Main){
                            Toast.makeText(
                                applicationContext,
                                R.string.file_deletion_success,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        val notificationDelete = NotificationCompat.Builder(applicationContext, "tiktok_delete")
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setSmallIcon(R.drawable.ic_stat_tdci)
                            .setContentTitle(applicationContext.getString(R.string.content_deletion_notification_title))
                            .setStyle(
                                NotificationCompat.BigTextStyle()
                                    .bigText("File ${file.name} deletion has finished")
                            )
                            .build()
                        notificationManager?.notify(notificationId, notificationDelete)

                } else{
                    //En caso de no poder escribir en el archivo, se le indica al usuario que lo tiene que eliminar manualmente
                    withContext(Dispatchers.Main){
                        Toast.makeText(
                            applicationContext,
                            R.string.file_delete_manually,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    val notificationDelete = NotificationCompat.Builder(applicationContext, "tiktok_delete")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setSmallIcon(R.drawable.ic_stat_tdci)
                        .setContentTitle(applicationContext.getString(R.string.content_deletion_notification_title))
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .bigText("Item ${file.name} has been deleted from list but you need to delete file manually")
                        )
                        .build()
                    notificationManager?.notify(notificationId, notificationDelete)
                }
            }

        }catch(e: Exception){
            Log.e(TAG, "The exception caught while executing the DeleteWorker")
            e.printStackTrace()
            //Envia un mensaje al usuario si ocurrio un error durante el proceso de eliminación del contenido
            withContext(Dispatchers.Main){
                Toast.makeText(
                    applicationContext,
                    R.string.delete_worker_error,
                    Toast.LENGTH_SHORT
                ).show()
            }

            //Se le regresa una instancia al WokrManager que indica que el trabajo fallo en algún momento
            return Result.failure()
        }
        //Se le regresa una instancia al WorkManager que indica que el trabajo fue exitoso.
        delay(5000)
        return Result.success()
    }

    private fun createNotificationChannel(){
        //Condicional para especificar que solo se ejecute el siguiente código si la versión de Android del usuario es 8 en adelante
        //Esto debido a que los canales de notificaciones fueron implementados a partir de esa versión de Android
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel = notificationManager?.getNotificationChannel("tiktok_delete")
            if(notificationChannel == null){
                val name = applicationContext.getString(R.string.delete_notification_channel_name)
                val descriptionText = applicationContext.getString(R.string.delete_notification_description)
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel("tiktok_delete", name, importance).apply {
                    description = descriptionText
                }
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }
}
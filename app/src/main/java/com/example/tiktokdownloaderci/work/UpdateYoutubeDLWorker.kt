package com.example.tiktokdownloaderci.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.tiktokdownloaderci.R
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "UpdateYoutubeDLWorker"

//Clase 'UpdateYoutubeDLWorker', la cual se utilizará para inicializar una nueva petición de trabajo para la cola de trabajo.
//El proposito de esta clase 'ListenableWorker' es el de actualizar la herramienta de manera asincrona.
class UpdateYoutubeDLWorker(appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    //Se obtiene el servicio de notificaciones del dispositivo del Usuario.
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager?

    override suspend fun doWork(): Result {
        try{
            //Se obtiene el canal de actualización en el que se actualizará la herramienta yt-dlp
            val updateChannel = inputData.getString("updateChannelKey")!!

            //Se convierte el String del canal de actualización en un objeto compatible con el método 'updateYoutubeDL'
            val updateChannelR: YoutubeDL.UpdateChannel = when(updateChannel){
                "NIGHTY" -> {
                    YoutubeDL.UpdateChannel.NIGHTLY
                }
                "STABLE" -> {
                    YoutubeDL.UpdateChannel.STABLE
                }
                "MASTER" -> {
                    YoutubeDL.UpdateChannel.MASTER
                }
                else -> {
                    YoutubeDL.UpdateChannel.NIGHTLY
                }
            }

            //Se crea el canal de notificaciones, necesario para mostrar notificaciones a partir de Android Oreo (8)
            createNotificationChannel()
            //Se guarda en una variable el id que tendran las notificaciones de este trabajo en específico
            val notificationId = id.hashCode()
            //Se inicializa la notificación que le indicará al usuario que la actualización de la herramienta esta iniciando
            val notification = NotificationCompat.Builder(applicationContext, "tiktok_update_yt_dl")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_stat_tdci)
                .setContentTitle(applicationContext.getString(R.string.update_notification_title))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(applicationContext.getString(R.string.update_notification_text_1))
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                notification.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            }
            //Se construye la notificación inicializada, pero en otra variable.
            val notificationB = notification.build()

            //Se crea una instancia de 'ForegroundInfo' la cual contendrá la notificación que creamos anteriormente 'notification' y el id que se le dará a la notificación
            //En el caso de Android 14, es necesario especificar el tipo de Foreground Service dentro de la instancia de 'ForegroundInfo'
            val foregroundInfo : ForegroundInfo = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ForegroundInfo(notificationId, notificationB, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                ForegroundInfo(notificationId, notificationB)
            }

            //Se convierte la notificación en un 'ForegroundService' y se lanza
            //Esto con el propósito de mostrarle al usuario el progreso de la actualización en segundo plano
            setForegroundAsync(foregroundInfo)

            //Se inicia el proceso de actualización de la herramienta con el canal de actualización especificado al inicio.
            val result = YoutubeDL.getInstance().updateYoutubeDL(applicationContext, updateChannelR)
            //Muestra un mensaje si yt-dlp ya esta actualizado
            if(result == YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE){
                val notificationDownload = NotificationCompat.Builder(applicationContext, "tiktok_update_yt_dl")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_stat_tdci)
                    .setContentTitle(applicationContext.getString(R.string.update_notification_title))
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(applicationContext.getString(R.string.update_notification_text_3))
                    )
                    .build()
                notificationManager?.notify(notificationId, notificationDownload)

                withContext(Dispatchers.Main){
                    Toast.makeText(applicationContext, R.string.update_notification_text_3, Toast.LENGTH_SHORT).show()
                }
            }
            //Muestra un mensaje si yt-dlp se actualizo correctamente
            if(result == YoutubeDL.UpdateStatus.DONE){
                val notificationDownload = NotificationCompat.Builder(applicationContext, "tiktok_update_yt_dl")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_stat_tdci)
                    .setContentTitle(applicationContext.getString(R.string.update_notification_title))
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(applicationContext.getString(R.string.update_notification_text_2))
                    )
                    .build()
                notificationManager?.notify(notificationId, notificationDownload)

                withContext(Dispatchers.Main){
                    Toast.makeText(applicationContext, R.string.update_notification_text_2, Toast.LENGTH_SHORT).show()
                }
            }

        }catch(e: Exception){
            Log.e(TAG, "The exception caught while executing the UpdateYoutubeDLWorker")
            e.printStackTrace()
            //Envia un mensaje al usuario si ocurrio un error durante el proceso de actualización de la herramienta
            withContext(Dispatchers.Main){
                Toast.makeText(
                    applicationContext,
                    R.string.update_worker_error,
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
            val notificationChannel = notificationManager?.getNotificationChannel("tiktok_update_yt_dl")
            if(notificationChannel == null){
                val name = applicationContext.getString(R.string.update_notification_channel_name)
                val descriptionText = applicationContext.getString(R.string.update_notification_description)
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel("tiktok_update_yt_dl", name, importance).apply {
                    description = descriptionText
                }
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }
}
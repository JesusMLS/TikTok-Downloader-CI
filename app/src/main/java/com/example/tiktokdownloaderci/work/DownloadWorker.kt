package com.example.tiktokdownloaderci.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.tiktokdownloaderci.R
import com.example.tiktokdownloaderci.database.AppDatabase
import com.example.tiktokdownloaderci.database.Download
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import com.example.tiktokdownloaderci.utils.URIUtils

private const val TAG = "DownloadWorker"

//Clase 'DownloadWorker', la cual se utilizará para inicializar una nueva petición de trabajo para la cola de trabajo.
//El proposito de esta clase 'ListenableWorker' es el de descargar videos/audios en segundo plano de manera asincrona
class DownloadWorker(appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    //Se obtiene el servicio de notificaciones del dispositivo del Usuario.
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager?

    //Se crea un metodo 'doWork', donde se realizara el trabajo en segundo plano
    override suspend fun doWork(): Result {
        try {
            //Se obtienen los datos de entrada que se le dieron a la petición de trabajo cuando se inicializo.
            //Se obtiene la URL del video/audio
            val url = inputData.getString("urlKey")!!
            //Se obtiene el id de la tarea en concreto
            val taskId = inputData.getString("taskIdKey")!!
            //Se obtiene el nombre del video/audio en concreto (el titulo)
            val name = inputData.getString("nameKey")!!
            //Se obtiene el formato especificado en el que se descargará el video/audio
            val format = inputData.getString("formatKey")!!
            //Se obtiene el modo de descarga específico en el que se descargará el video/audio
            val downloadMode = inputData.getString("downloadModeKey")!!
            //Se obtiene el directorio de descarga especificado por el usuario
            val downloadDirectory = inputData.getString("downloadDirectoryKey")!!
            //Se obtiene el url de la vista previa del video/audio en concreto
            val thumbnail = inputData.getString("thumbnailKey")!!

            //Se obtiene la ruta del directorio especificado por el usuario y se le agrega a la ruta la carpeta 'TikTok Downloader CI'
            val treeUri = Uri.parse(downloadDirectory)
            val directory = File(URIUtils.resolveContentUri(treeUri, applicationContext), applicationContext.getString(R.string.app_name))

            //Se crea el canal de notificaciones, necesario para mostrar notificaciones a partir de Android Oreo (8)
            createNotificationChannel()

            //Se guarda en una variable el id que tendran las notificaciones de este trabajo en específico
            val notificationId = id.hashCode()

            //Se crea un Intent para nuestra clase CancelReceiver (del tipo BroadcastReceiver)
            //A la cual se le van a pasar 2 variables 'taskId' y 'notificationId'
            val intent = Intent(applicationContext, CancelReceiver::class.java).apply {
                putExtra("taskId", taskId)
                putExtra("notificationId", notificationId)
            }

            //Se construye una flag, la cual se va a especificar en nuestro PendingIntet
            val flag = PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE

            //Se construye el PendingIntent usando la función getBroadcast
            //Este se utiliza para especificar la acción a realizar cuando se presione el boton 'Cancel'.
            val pendingIntent = PendingIntent.getBroadcast(applicationContext, 0, intent,
                flag)

            //Se inicializa la notificación que le indicará al usuario que la descarga del video/audio esta iniciando
            val notification = NotificationCompat.Builder(applicationContext, "tiktok_download")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_stat_tdci)
                .setContentTitle(name)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(applicationContext.getString(R.string.download_notification_text_2))
                )
                .setProgress(100, 0, true)
                .addAction(android.R.drawable.ic_delete, applicationContext.getString(R.string.download_cancel_button_text), pendingIntent)
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
            //Esto con el propósito de mostrarle al usuario el progreso de la descarga en segundo plano.
            setForegroundAsync(foregroundInfo)

            //Se inicializa la petición de descarga del video/audio a la herramienta
            val request = YoutubeDLRequest(url)
            //Se agregan los parámetros necesarios antes de ejecutar la petición

            //Se verifica si se indico algún formato de descarga para yt-dlp
            if(format != "none"){
                request.addOption("-f", format)
            }

            //Se verifica si la descarga es de tipo 'audio'
            if(downloadMode == "audio"){
                //Se añade el formato del archivo en el que se descargará el audio
                val formatConverted = inputData.getString("formatConvertedKey")!!

                //Se verifica si el audio se convertirá de video a audio
                if(format == "none"){
                    request.addOption("--extract-audio")
                    request.addOption("--audio-format", formatConverted)
                }
                //Se añade el formato que tendrá el nombre del archivo y la ubicación donde se descargará el archivo
                request.addOption("-o", directory.absolutePath + "/tiktok@%(uploader)s_%(display_id)s_audio_${formatConverted}.%(ext)s")
            }

            //Se verifica si la descarga es de tipo 'video'
            if(downloadMode == "video"){
                //Se añade el formato que tendrá el nombre del archivo y la ubicación donde se descargará el archivo
                request.addOption("-o", directory.absolutePath + "/tiktok@%(uploader)s_%(display_id)s.%(ext)s")
            }

            //Se limita los caracteres para los nombres de archivos
            request.addOption("--restrict-filenames")
            //Se imprime la ubicación del archivo al finalizar la descarga
            request.addOption("--print" ,"after_move:filepath")

            //Se ejecuta la petición para descargar el video/audio especificado en el URL
            val downloadReq = YoutubeDL.getInstance().execute(request, taskId) {
                _, _ , _ ->
                //Muestra una notificación donde se ve el fin del progreso de la descarga
                val notificationDownload3 =
                    NotificationCompat.Builder(applicationContext, "tiktok_download")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setSmallIcon(R.drawable.ic_stat_tdci)
                        .setContentTitle(name)
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .bigText(applicationContext.getString(R.string.download_notification_text_3))
                        )
                        .setProgress(100, 0, true)
                        .build()
                notificationManager?.notify(notificationId, notificationDownload3)
            }

            //Código para guardar datos de la descarga en la bd, con el fin de mostrarlos en el historial de descargas.
            val downloadDao = AppDatabase.getDatabase(applicationContext).DownloadDao()
            val download = Download(name).apply {
                imageUrl = thumbnail
                downloadedPath = downloadReq.out
                downloadedPercent = 100.00
                downloadedType = downloadMode
                mediaUniqueIdentifier = taskId
            }
            //Se guarda la descarga en la bd
            downloadDao.insert(download)

            //Se guarda la ubicación de la descarga en una variable filePath
            val filePath = download.downloadedPath
            val file = File(filePath.trim())

            //Muestra una notificación donde se le indica al usuario que la descarga se agrego al historial de descargas
            val notificationDownloadR =
                NotificationCompat.Builder(applicationContext, "tiktok_download")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_stat_tdci)
                    .setContentTitle(name)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText("Download has been added to history as ${file.name}")
                    )
                    .setProgress(100, 100, false)
                    .build()
            notificationManager?.notify(notificationId, notificationDownloadR)

            //Muestra un mensaje indicando el fin del proceso de la descarga dependiendo del modo de descarga
            if(downloadMode == "video"){
                withContext(Dispatchers.Main){
                    Toast.makeText(
                        applicationContext,
                        R.string.video_download_finished,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }else {
                withContext(Dispatchers.Main){
                    Toast.makeText(
                        applicationContext,
                        R.string.audio_download_finished,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }catch(e: Exception){
            Log.e(TAG, "The exception caught while executing the DownloadWorker")
            e.printStackTrace()

            //Envia un mensaje al usuario si ocurrio un error durante el proceso de descarga del contenido
            withContext(Dispatchers.Main){
                Toast.makeText(
                    applicationContext,
                    R.string.download_worker_error,
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

//Función para crear el canal de notificaciones, necesario a partir de Android Oreo
    private fun createNotificationChannel(){
        //Condicional para especificar que solo se ejecute el siguiente código si la versión de Android del usuario es 8 en adelante
        //Esto debido a que los canales de notificaciones fueron implementados a partir de esa versión de Android
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel = notificationManager?.getNotificationChannel("tiktok_download")
            if(notificationChannel == null){
                val name = applicationContext.getString(R.string.download_notification_channel_name)
                val descriptionText = applicationContext.getString(R.string.download_notification_description)
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel("tiktok_download", name, importance).apply {
                    description = descriptionText
                }
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }
}
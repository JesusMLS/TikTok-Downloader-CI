package com.example.tiktokdownloaderci.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.tiktokdownloaderci.database.Download
import com.example.tiktokdownloaderci.databinding.FragmentDownloadsItemBinding
import com.example.tiktokdownloaderci.R
import com.example.tiktokdownloaderci.work.DeleteWorker
import com.squareup.picasso.Picasso
import java.io.File

//Clase 'DownloadsAdapter', la cual se utilizará para modificar el contenido del RecyclerView desde el código.
class DownloadsAdapter: RecyclerView.Adapter<DownloadsAdapter.DownloadViewHolder>() {

    //Variable donde se guardará la lista de Descargas (Download), con el propósito de mostrarla al usuario
    private var downloadList: List<Download> = emptyList()

    //Función que tiene el propósito de guardar una lista de 'Download' en la variable 'downloadList'
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<Download>){
        downloadList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DownloadViewHolder {
        val binding =
            FragmentDownloadsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DownloadViewHolder(binding)
    }

    //Función que tiene el propósito de actualizar el contenido de cada Item (Descarga) dentro del RecyclerView
    override fun onBindViewHolder(holder: DownloadViewHolder, position: Int) {
        holder.bind(downloadList[position])
    }

    //Función que tiene el propósito de obtener el número de Items (Descargas) almacenados en este adaptador
    override fun getItemCount(): Int {
        return downloadList.size
    }

    //Clase DownloadViewHolder, la cual almacenará toda la lógica que contendrá cada Item dentro del RecyclerView
    class DownloadViewHolder(private val binding: FragmentDownloadsItemBinding):
            RecyclerView.ViewHolder(binding.root){
                //Función que tiene el propósito de añadir/modificar/eliminar lógica a un Item en específico
                fun bind(item: Download){

                    //El nombre para cada descarga que se muestre en el historial de descargas será el nombre del archivo descargado.
                    //Ejemplo tiktok@32323.mp4
                    binding.mediaText.text = File((item.downloadedPath).trim()).name

                    //El porcentaje para cada descarga que se muestre en el historial de descargas será el porcentaje indicado en la base de datos
                    //Ejemplo 100%
                    binding.downloadPercent.text = "${ item.downloadedPercent }%"

                    //Se utiliza la herramienta Picasso para descargar la vista previa de cada video y mostrarla en el historial de descargas
                    //El url donde se encuentra la vista previa de cada video se encuentra guardado en la base de datos como imageUrl
                    val picasso = Picasso.get()
                    //If para verificar si la descarga es de tipo 'audio o 'video' (lo cual es especificado por la variable downloadedType en la base de datos
                    if(item.downloadedType == "video"){
                        //En caso de que sea 'video', se muestra un icono ilustrando un archivo de video como placeholder
                        picasso.load(item.imageUrl.toUri()).placeholder(R.drawable.baseline_video_file_24).resize(450,450).into(binding.mediaImage)
                    } else {
                        //En caso de que sea 'audio', se muestra un icono ilustrando un archivo de audio como placeholder
                        picasso.load(item.imageUrl.toUri()).placeholder(R.drawable.baseline_audio_file_24).resize(450,450).into(binding.mediaImage)
                    }

                    //Código que captura cuando le das click a una descarga del historial
                    binding.root.setOnClickListener{
                        //Cuando le des click a una descarga, se mandará a llamar a la función 'viewContent'
                        viewContent(item.downloadedPath, it.context)
                    }

                    //Código que captura cuando le das un click largo a una descarga del historial
                    binding.root.setOnLongClickListener{
                        //Cuando le des un click largo a una descarga, se mandará a llamar a la función 'shareContent'
                        shareContent(item.downloadedPath, it.context)
                        return@setOnLongClickListener true
                    }

                    //Código que captura cuando le das click al botón que abré el submenú de opciones
                    binding.imgMore.setOnClickListener{
                        //Cuando le des click al botón del submenú de opciones, se mandará a llamar a la función 'showOptions'
                        showOptionsMenu(binding.imgMore, binding.root.context, item)
                    }

                }

                //Función que tiene el propósito de mostrar un submenú de opciones, con las siguientes opciones:
                //Open, Share y Delete
                private fun showOptionsMenu(view: View, context: Context, item: Download){
                    val popupMenu = PopupMenu(context, view, Gravity.START)
                    popupMenu.menuInflater.inflate(R.menu.download_sub_menu, popupMenu.menu)
                    popupMenu.show()
                    popupMenu.setOnMenuItemClickListener {
                        when(it.itemId){
                            R.id.open_item -> {
                                //Al darle a la opción de Open, se mandará a llamar a la función 'viewContent'
                                viewContent(item.downloadedPath, context)
                            }
                            R.id.share_item -> {
                                //Al darle a la opción de Share, se mandará a llamar a la función 'shareContent'
                                shareContent(item.downloadedPath, context)
                            }
                            R.id.delete_item -> {
                                //Al darle a la opción de Delete, se mandará a llamar a la función 'deleteContent'
                                deleteContent(item.id, context)
                            }
                        }
                        true
                    }
                }

                //Función para abrir/mostrar el contenido especificado por su ubicación
                //Esta función tiene el propósito de mostrarle al usuario una lista de aplicaciones en las que puede visualizar el video / escuchar el audio
                private fun viewContent(filePath: String, context: Context){
                    val intent = Intent(Intent.ACTION_VIEW)
                    val downloadedFile = File(filePath.trim())
                    //Este if verifica que el archivo especificado en la ubicación como parámetro 'filePath', exista realmente.
                    if(!downloadedFile.exists()){
                        //En caso de que no exista, se muestra un Toast especificandole al usuario de que no se encontró el archivo
                        Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show()
                        return
                    }
                    //Se convierte la ubicación en un ContentUri
                    val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", downloadedFile)
                    //Se obtiene el típo de archivo, ejemplo video/mp4
                    val mimeType = context.contentResolver.getType(uri) ?: "*/*"
                    intent.setDataAndType(uri, mimeType)
                    //Se establece una bandera para obtener permisos de lectura en el archivo especificado por el uri
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    //If para verificar que el listado de aplicaciones disponibles en las que abrir ese archivo no sea nulo
                    if(intent.resolveActivity(context.packageManager) != null){
                        //En caso de que no lo sea, se inicia la actividad
                        startActivity(context, intent, null)
                    } else {
                        //En caso de que lo sea, se le indica al usuario que no hay alguna aplicación disponible en la cual poder reproducir el archivo.
                        Toast.makeText(context, R.string.available_app_not_found, Toast.LENGTH_SHORT).show()
                    }
                }

                //Función para compartir el contenido especificado por su ubicación
                //Esta función tiene el propósito de mostrarle al usuario un menú con opciones para compartir el archivo.
                //Ejemplo, compartir el video en Discord, Facebook, Whatsapp, etc.
                private fun shareContent(filePath: String, context: Context){
                    val intent = Intent(Intent.ACTION_SEND)
                    val downloadedFile = File(filePath.trim())
                    //Este if verifica que el archivo especificado en la ubicación como parámetro 'filePath', exista realmente.
                    if(!downloadedFile.exists()){
                        //En caso de que no exista, se muestra un Toast especificandole al usuario de que no se encontró el archivo
                        Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show()
                        return
                    }
                    //Se convierte la ubicación en un ContentUri
                    val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", downloadedFile)
                    //Se obtiene el típo de archivo, ejemplo video/mp4
                    val mimeType = context.contentResolver.getType(uri) ?: "*/*"
                    intent.setType(mimeType)
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    //Se establece una bandera para obtener permisos de lectura en el archivo especificado por el uri
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    //Se inicia la actividad (El menú donde se le mostrará al usuario opciones para compartir su video/audio)
                    startActivity(context, Intent.createChooser(intent, context.getString(R.string.share_chooser_title)), null)
                }

                //Función para eliminar un contenido de la bd y del directorio, especificado por la id
                //Esta función tiene el propósito de eliminar la entrada en la base de datos y el archivo en el directorio, a partir de una id especficada por el usuario
                private val deleteContent_TAG: String = "deleteContent"
                private fun deleteContent(id: Long, context: Context){
                    try{
                        //Variable donde se guarda el identificador para el trabajo de eliminación de el contenido
                        val workTag = "tag_$id"

                        //Variable donde se inicializa el WorkManager
                        //Este sera necesario para crear trabajos (Workers), que nos permitiran hacer tareas en segundo plano, sin la necesidad de tener abierta la aplicación
                        val wManager = WorkManager.getInstance(context.applicationContext)
                        val state = wManager.getWorkInfosByTag(workTag).get()?.getOrNull(0)?.state
                        val running = state === WorkInfo.State.RUNNING || state === WorkInfo.State.ENQUEUED

                        //Se verifica que la eliminación del contenido no este ya en progreso
                        //En caso de que lo este, se muestra un mensaje avisandole al usuario y se sale de la función
                        if(running){
                            Toast.makeText(
                                context,
                                R.string.delete_task_running,
                                Toast.LENGTH_SHORT
                            ).show()
                            return
                        }

                        //Se inicializa el objeto workData, donde se guardarán pares de llavés que almacenarán variables con datos.
                        //Esta lista de pares de llaves se le pasará al Worker que inicializaremos más adelante.
                        val workData = workDataOf(
                            "contentIdKey" to id
                        )

                        //Se inicializa una petición de trabajo, tomando como base la clase 'DeleteWorker' creada dentro del mismo proyecto.
                        val workRequest = OneTimeWorkRequestBuilder<DeleteWorker>()
                            .addTag(workTag)
                            .setInputData(workData)
                            .build()

                        //Se envía la petición de trabajo dentro de la cola de trabajos de la aplicación
                        wManager.enqueueUniqueWork(
                            workTag,
                            ExistingWorkPolicy.KEEP,
                            workRequest
                        )

                        //Se le envía un mensaje al usuario avisando que la eliminación del contenido entro en cola
                        Toast.makeText(
                            context,
                            R.string.delete_task_queued,
                            Toast.LENGTH_SHORT
                        ).show()

                    }catch(e: Exception){
                        Log.e(deleteContent_TAG, "The exception caught while executing the process. (e)")
                        e.printStackTrace()
                        //Muestra un mensaje si ocurrio un error durante el proceso de eliminación del contenido
                        Toast.makeText(
                            context,
                            R.string.delete_function_error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
}
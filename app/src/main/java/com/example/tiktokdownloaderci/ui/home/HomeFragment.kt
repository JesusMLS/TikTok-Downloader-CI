package com.example.tiktokdownloaderci.ui.home

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tiktokdownloaderci.databinding.FragmentHomeBinding
import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.preference.PreferenceManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.tiktokdownloaderci.R
import com.example.tiktokdownloaderci.database.AppDatabase
import com.example.tiktokdownloaderci.database.Download
import com.example.tiktokdownloaderci.work.DownloadWorker
import com.squareup.picasso.Picasso
import com.yausername.youtubedl_android.YoutubeDL.getInstance
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Matcher
import java.util.regex.Pattern
import com.example.tiktokdownloaderci.utils.URIUtils
import java.io.File

//Clase HomeFragment, donde se especifica toda la lógica para el fragmento fragment_home
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    //En esta función se define y configura la interfaz de la vista del fragmento (durante la creación de la vista)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //Requerido para actualizar el menú de opciones
        requireActivity().invalidateOptionsMenu()

        return root
    }

    //En esta función se define y configura la interfaz/logica de la vista del fragmento (después de la creación de la vista)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Se manda a llamar a la función initViews, donde se encuentra toda la lógica de la vista
        initViews()
    }

    //Función donde se especifica toda la lógica de la vista, lo que incluye observadores, botones, permisos, inicialización de viewModels, etc.
    private fun initViews(){
        //Se crea una instancia del ViewModel HomeViewModel o se obtiene el existente
        //Gracias a este ViewModel podemos restaurar el estado de los componentes antes de la destrucción de la vista
        //Por ejemplo, la vista previa del video, los botones ocultos, etc.
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        //Cambiamos la visibilidad de los botones/la vista previa de acuerdo al estado de la variable buttonLayoutVisible
        binding.layoutDownloadOptions.visibility = if(homeViewModel.buttonLayoutVisible) View.VISIBLE else View.GONE

        //En caso de que la variable mediaInformation no sea nula, se obtiene la vista previa y se muestra en la vista.
        if(homeViewModel.mediaInformation != null){
            val picasso = Picasso.get()
            picasso.load((homeViewModel.mediaInformation!!.thumbnail)).resize(450,450).into(binding.imageViewVistaVideo)
        }

        //Observador que cambia la visibilidad del circulo de carga dependiendo del estado de la variable 'loadState' dentro del viewModel de la pantalla Home
        homeViewModel.loadState.observe(viewLifecycleOwner) { state: LoadState ->
            when (state) {
                LoadState.INITIAL -> {
                    binding.loadingProgressBar.visibility = View.GONE
                }

                LoadState.LOADING -> {
                    binding.loadingProgressBar.visibility = View.VISIBLE
                }

                LoadState.LOADED -> {
                    binding.loadingProgressBar.visibility = View.GONE
                }
            }
        }

        //Verificamos que la aplicación tenga los permisos necesarios para su correcto funcionamiento.
        //Ejemplo, permisos de almacenamiento, notificaciones, etc.
        askMultiplePermissions()

        //A partir de este punto le damos funcionalidad a los diferentes elementos dentro de la vista

        //Se obtiene el servicio del portapapeles de Android
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        //Se le da lógica al boton de 'PEGAR'
        binding.botonPegar.setOnClickListener{
            //Se obtiene el objeto en la posición 0 del portapapeles de Android
            val pasteData = clipboard.primaryClip?.getItemAt(0)
            //Se verifica que el objeto no sea nulo
            if (pasteData != null) {
                //Se obtiene el texto del objeto en la posición 0 del portapapeles y se pasa al campo_URL
                binding.campoURL.setText(pasteData.text)
            }
        }

        //Se le da lógica al boton de 'BORRAR_1'
        binding.botonBorrar1.setOnClickListener{
            //Se elimina el URL del campo de texto
            binding.campoURL.setText("")

            //Se actualiza el estado de las variables en el viewModel, con el propósito de inicializarlas como nuevas
            homeViewModel.URL = ""
            homeViewModel.mediaInformation = null
            homeViewModel.buttonLayoutVisible = false

            //Se recarga el fragmento para mostrar los nuevos cambios
            parentFragmentManager.beginTransaction().detach(this@HomeFragment).commit()
            parentFragmentManager.beginTransaction().attach(this@HomeFragment).commit()
        }

        //Se verifica que el campo de URL tenga texto antes de habilitar el boton de 'DESCARGAR'
        binding.campoURL.doOnTextChanged{ text, _, _, _ ->
            if (text != null) {
                //Si tiene texto y el botón 'DESCARGAR' esta desabilitado, entonces se habilita
                if(!binding.botonMostrarDescarga.isEnabled){
                    binding.botonMostrarDescarga.setEnabled(true)
                }
                //Si tiene texto pero es una cadena vacia, entonces se desabilita el boton 'DESCARGAR'
                if(text.isEmpty()){
                    binding.botonMostrarDescarga.setEnabled(false)
                }
            }
        }

        val botonMostrarDescarga_TAG = "botonMostrarDescarga"
        //Se le da lógica al boton de 'DESCARGAR'
        binding.botonMostrarDescarga.setOnClickListener{
            //Se cambia el estado del circulo de carga a 'LOADING'
            homeViewModel.loadState.value = LoadState.LOADING

            //Se obtiene la URL del campoURL
            val URL = (binding.campoURL.text).toString().trim()


            //Se verifica si ya se trabajo con ese URL
            if(URL == homeViewModel.URL){
                //En caso de que ya se trabajo con ese URL, simplemente se cambia el estado del circulo de carga a 'LOADED' y se sale del método
                homeViewModel.loadState.value = LoadState.LOADED
                return@setOnClickListener
            }

            //Se verifica si el URL coincide con el requerido para el correcto funcionamiento de la herramienta
            //Esto a partir de una función verifyURL, que se encarga de ello
            val validURL = verifyURL(URL)

            //Este if verifica si el valor de validURL es true o false
            if(validURL){
                //En caso de ser true, se continua obteniendo la información del URL

                //Se obtiene la información del video a partir de una petición de YoutubeDL
                val request = YoutubeDLRequest(URL)
                val myScope = CoroutineScope(Dispatchers.IO)
                //El código que sigue se lanza en el hilo de IO, con el propósito de que no bloquee el funcionamiento de la aplicación
                //Esto debido a que son peticiones que se hacen a la red
                myScope.launch {
                    try{
                        //Se obtiene información del video a partir de la herramienta youtubedl-android
                        val streamInfo = getInstance().getInfo(request)

                        //Se guarda la información del video en una variable, para su uso en el código
                        homeViewModel.mediaInformation = streamInfo

                        //Se ejecuta el siguiente código en el hilo Main, ya que son cambios que se hacen a la interfáz, por lo que no se pueden hacer en el hilo IO
                        withContext(Dispatchers.Main){

                            //Se utilizá Picasso para descargar la vista previa del video y mostrarla en la pantalla 'Home'
                            val picasso = Picasso.get()
                            picasso.load(homeViewModel.mediaInformation!!.thumbnail).resize(450,450).into(binding.imageViewVistaVideo)

                            //Se guarda el url del video en el viewModel
                            homeViewModel.URL = URL

                            //Se muestran los botones para descargar en los diferentes formatos y la vista previa del video
                            //Se verifica que los botones para descargar no esten visibles ya, antes de volverlos visibles. También para la vista previa del video se aplica lo mismo
                            if(!binding.layoutDownloadOptions.isVisible){
                                binding.layoutDownloadOptions.visibility = View.VISIBLE

                                //Guardamos el estado de la visibilidad en el viewModel
                                homeViewModel.buttonLayoutVisible = true
                            }
                        }
                    }catch(e: Exception){
                        //En caso de ocurrir algún error durante la obtención de la información del video con youtubedl-android se le indica al usuario
                        Log.e(botonMostrarDescarga_TAG, "The exception caught while executing the process. (e)")
                        e.printStackTrace()
                        withContext(Dispatchers.Main){
                            //Se le muestra al usuario un mensaje indicando que hubo un error
                            Toast.makeText(context, R.string.information_fetch_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                    //Después de que se termine la tarea de obtener la información del video y mostrar la vista previa y los botones, se cambia el estado del circulo de carga a 'LOADED'
                    homeViewModel.loadState.postValue(LoadState.LOADED)
                }
            } else {
                //En caso de ser false se muestra un mensaje de error al usuario, especificando que el URL no es un URL perteneciente a la plataforma que se requiere
                Toast.makeText(context, R.string.invalid_download_url, Toast.LENGTH_SHORT).show()
                //Se cambia el esta del circulo de carga a 'LOADING'
                homeViewModel.loadState.value = LoadState.LOADED
            }
        }

        //Se le da lógica al boton de 'DESCARGAR VIDEO (MEJOR)'
        binding.botonDescargar.setOnClickListener{
            //Se obtiene el URL del video guardado en el viewModel
            val URL = homeViewModel.URL

            //Se especifica el formato con el que se va a descargar el video/audio
            val format = "best[ext=mp4]/bv[ext=mp4]+ba"

            //Se obtiene la ubicación de la carpeta especificada por el usuario donde se guardaran las descargas
            val downloadLocation = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(getString(
                R.string.download_location_key), null)

            //If para verificar que la ubicación de descarga no sea null
            //En caso de que lo sea se le indica al usuario
            if(downloadLocation == null){
                Toast.makeText(context, R.string.invalid_download_location, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //If para verificar que mediaInformation no sea null
            //Esto es poco posible, ya que mediaInformation siempre será != null antes de que se muestren los botones de descarga o después
            if(homeViewModel.mediaInformation == null){
                return@setOnClickListener
            }

            //Se manda a llamar a la función que se encarga de descargar el video con yt-dlp
            videoDownload(URL, format, downloadLocation, homeViewModel.mediaInformation!!)
        }

        //Se le da lógica al boton de 'DESCARGAR VIDEO (PEOR)'
        binding.botonDescargar2.setOnClickListener{
            //Se obtiene el URL del video guardado en el viewModel
            val URL = homeViewModel.URL
            //Se especifica el formato con el que se va a descargar el video/audio
            val format = "worst[ext=mp4]/wv[ext=mp4]+wa"

            //Se obtiene la ubicación de la carpeta especificada por el usuario donde se guardaran las descargas
            val downloadLocation = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(getString(
                R.string.download_location_key), null)

            //If para verificar que la ubicación de descarga no sea null
            //En caso de que lo sea se le indica al usuario
            if(downloadLocation == null){
                Toast.makeText(context, R.string.invalid_download_location, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //If para verificar que mediaInformation no sea null
            //Esto es poco posible, ya que mediaInformation siempre será != null antes de que se muestren los botones de descarga o después
            if(homeViewModel.mediaInformation == null){
                return@setOnClickListener
            }

            //Se manda a llamar a la función que se encarga de descargar el video con yt-dlp
            videoDownload(URL, format, downloadLocation, homeViewModel.mediaInformation!!)
        }

        //Se le da lógica al boton de 'DESCARGAR AUDIO (MP3)'
        binding.botonMp3.setOnClickListener{
            //Se obtiene el URL del video guardado en el viewModel
            val URL = homeViewModel.URL

            //Se especifica el formato con el que se va a descargar el video/audio
            val format = "none"

            //Se especifica el formato del archivo convertido, en caso de utilizar yt-dlp para convertir un .mp4 a .mp3, .flac, .wav (de video a audio)
            val formatFile = "mp3"

            //Se obtiene la ubicación de la carpeta especificada por el usuario donde se guardaran las descargas
            val downloadLocation = PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(getString(
                R.string.download_location_key), null)

            //If para verificar que la ubicación de descarga no sea null
            //En caso de que lo sea se le indica al usuario
            if(downloadLocation == null){
                Toast.makeText(context, R.string.invalid_download_location, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //If para verificar que mediaInformation no sea null
            //Esto es poco posible, ya que mediaInformation siempre será != null antes de que se muestren los botones de descarga o después
            if(homeViewModel.mediaInformation == null){
                return@setOnClickListener
            }

            //Se manda a llamar a la función que se encarga de descargar el audio con yt-dlp
            audioDownload(URL, format, formatFile, downloadLocation, homeViewModel.mediaInformation!!)
        }
    }

    //Función para pedir multiples permisos cómo de notificaciones y almacenamiento
    private fun askMultiplePermissions(){
        //Lista donde se guardaran todos los permisos que aun no se han dado a la aplicación
        val permissionsToRequest = mutableListOf<String>()

        //Se verifica cada permiso y se añade a la lista si no ha sido permitido aun
        //Se verifica si el dispositivo tiene una versión de Android Inferior a Android 13
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU){
            //En caso de tener una versión inferior a Android 13, se verifica si la aplicación tiene permisos 'WRITE_EXTERNAL_STORAGE'
            if(ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED){
                //En caso de no tenerlos, se añaden a la lista
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            //Se verifica si el dispositivo tiene una versión de Android superior o igual a Android 13
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            //En caso de ser correcta esta verificación, se verifica si la aplicación tiene los siguientes permisos
            //Permiso de 'READ_MEDIA_AUDIO'
            if(ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_AUDIO
                ) != PackageManager.PERMISSION_GRANTED){
                //En caso de no tenerlo, se añade a la lista
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            //Permiso de 'READ_MEDIA_VIDEO'
            if(ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_VIDEO
                ) != PackageManager.PERMISSION_GRANTED){
                //En caso de no tenerlo, se añade a la lista
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }

            //Permiso de 'POST_NOTIFICATIONS'
            if(ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED){
                //En caso de no tenerlo, se añade a la lista
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Después de verificar todos los permisos, si hay 1 o más permisos sin otorgar, se mandan al launcher de permisos
        if (permissionsToRequest.isNotEmpty()) {
            multiplePermissionsLauncher(permissionsToRequest.toTypedArray())
        }
    }

    //Launcher al que se le dan todos los permisos a pedir por parte de la aplicación
    //Se le piden al usuario y se le muestra si fueron otorgados o no.
    private fun multiplePermissionsLauncher(permissionsToRequest: Array<String>) {
        val requestMultiplePermissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                //Para cada permiso pedido por el launcher, se obtiene su información
                permissions.entries.forEach { entry ->
                    val permission = entry.key
                    val isGranted = entry.value
                    //If que verifica si el permiso fue permitido
                    if (isGranted) {
                        //En caso de ser permitido, se le indica al usuario
                        Toast.makeText(requireContext(), "$permission granted", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        //En caso de no ser permitido, se le indica al usuario
                        Toast.makeText(requireContext(), "$permission denied", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }

        //Se lanza el launcher de permisos con todos los permisos especificados en el Array de Strings.
        requestMultiplePermissionsLauncher.launch(permissionsToRequest)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //Funcion para verificar que el URL dado por el usuario pertenezca a la plataforma TikTok.
    //Al verificar el URL, nos evitamos errores en la aplicación y ahorramos peticiones innecesarias de descarga a youtubedl-android
    private val verifyURL_TAG: String = "verifyURL"
    private fun verifyURL(url: String): Boolean {
            try{
                //Guardamos el URL del usuario en una variable
                var URL = url

                //Verificamos que el URL no este vacio
                if(URL.isEmpty()){
                    //En caso de estarlo, nos salimos de la función
                    return false
                }

                //Verificamos que el URL no tenga un / al final, esto con el propósito de que funcione correctamente con la expresion regular a utilizar.
                if(URL.last() == '/'){
                    //En caso de tenerlo, se elimina del URL
                    URL = URL.substring(0, URL.length -1 )
                }

                //Se verifica el URL mediante el uso de una expresión regular, la cual solo admite URLS pertenecientes a la plataforma en específico.
                //En este caso la plataforma es TikTok
                //Por lo que esta expresión regular verifica si el URL es perteneciente a algún URL de TikTok. (entre todos los diferentes URLS que tiene la plataforma)
                val regex = "^https:\\/\\/(?:m|www|vm)?\\.?tiktok\\.com\\/((?:.*\\b(?:(?:usr|v|embed|user|video)\\/|\\?shareId=|\\&item_id=)(\\d+))|\\w+)"
                val URL_Pattern : Pattern = Pattern.compile(regex)
                val URL_Matches : Matcher = URL_Pattern.matcher(URL)
                val result: Boolean = URL_Matches.matches()
                //Se regresa un true o un false, dependiendo de si el URL coincide o no.
                return result
            }catch(e: Exception){
                Log.e(verifyURL_TAG, "The exception caught while executing the process. (e)")
                e.printStackTrace()
                //Muestra un mensaje si ocurrio un error durante el proceso para verificar el URL
                    Toast.makeText(
                        context,
                        R.string.verify_function_error,
                        Toast.LENGTH_SHORT
                    ).show()
                //Se regresa un false, indicando que el URL no coincide.
                return false
            }
    }

    private val videoDownload_TAG: String = "videoDownload"
    //Función para descargar el video proporcionado en la URL, donde dependiendo del parámetro que se le dé
    //se va a descargar el video en la mejor calidad y sin marca de agua o la peor calidad y con marca de agua.
    private fun videoDownload(url: String, videoFormat: String, downloadLocation: String, mediaInformation: VideoInfo){
        val downloadScope = CoroutineScope(Dispatchers.IO)
        //Se especifica que el siguiente código se va a ejecutar en el hilo IO.
        downloadScope.launch {
            try{
                //Obtiene la información del video a partir del viewModel
                val videoInformation = mediaInformation
                //Guarda la información del video necesaria en distintas variables
                //Variable donde se guarda el id del video
                val workTag = (videoInformation.id).toString()
                //Variable para especificar un modo de descarga en el DownloadWorker
                val downloadMode = "video"
                //Variable donde se guarda el título del video
                val title = (videoInformation.title).toString()
                //Variable donde se guarda el URL de la vista previa del video
                val thumbnail = (videoInformation.thumbnail).toString()
                //Variable para especificar la ubicación de la descarga
                val downloadDirectory = downloadLocation
                //Variable para especificar el formato del video
                val format = videoFormat

                //Aquí se verifica si el video no fue descargado ya por el usuario
                //Primero se hace una verificación en la base de datos
                val downloadDao = AppDatabase.getDatabase(requireContext().applicationContext).DownloadDao()
                val itemCheck = downloadDao.getByIdentifier(workTag)
                if(itemCheck != null){
                    //En caso de que ya se encuentre en el historial de descargas, se le menciona al usaurio y se sale de la función
                    withContext(Dispatchers.Main){
                        Toast.makeText(
                            context,
                            R.string.media_already_downloaded,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                //Después se hace una verificación en los archivos del directorio para ver si el video no se encuentra ya descargado
                //Se convierte la ubicación en String en un Uri
                val treeUri = Uri.parse(downloadDirectory)
                //Se le agrega al Uri la ubicación dentro de la carpeta 'TikTok Downloader CI'
                //Antes de ello se convierte el contentUri en File Uri con la función 'resolveContentUri', esto debido a que la ubicación que se guarda en la configuración global no tiene el formato adecuado
                val directory = File(URIUtils.resolveContentUri(treeUri, requireContext().applicationContext), getString(R.string.app_name))
                //Se busco si existe un archivo del video dentro de la carpeta de descargas del usuario
                val file_video = File(directory.absolutePath+"/tiktok@${videoInformation.uploader}_${videoInformation.displayId}.mp4")
                //En caso de que exista este archivo se ejecuta el siguiente código
                if(file_video.exists()){
                    //Si el archivo ya existe, simplemente se agrega al historial de Descargas, ahorrandonos el tener que volver a descargarlo
                    val download = Download(title).apply {
                        imageUrl = thumbnail
                        downloadedPath = file_video.path
                        downloadedPercent = 100.00
                        downloadedType = downloadMode
                        mediaUniqueIdentifier = workTag
                    }
                    downloadDao.insert(download)
                    //Se le indica al usuario que ya existe el archivo en el directorio y que simplemente se agregó al historial de descargas
                    withContext(Dispatchers.Main){
                        Toast.makeText(
                            context,
                            R.string.file_exits_in_directory,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    //Se sale de la función
                    return@launch
                }

                //Variable donde se inicializa el WorkManager
                //Este sera necesario para crear trabajos (Workers), que nos permitiran hacer tareas en segundo plano, sin la necesidad de tener abierta la aplicación
                val wManager = WorkManager.getInstance(requireContext().applicationContext)
                val state = wManager.getWorkInfosByTag(workTag).get()?.getOrNull(0)?.state
                val running = state === WorkInfo.State.RUNNING || state === WorkInfo.State.ENQUEUED
                //Se verifica que la descarga de un video en específico no este ya en progreso
                //En caso de que lo este, se muestra un mensaje avisandole al usuario y se sale de la función
                if(running){
                    withContext(Dispatchers.Main){
                        Toast.makeText(
                            context,
                            R.string.video_download_in_progress,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                //Se inicializa el objeto workData, donde se guardarán pares de llavés que almacenarán variables con datos.
                //Esta lista de pares de llaves se le pasará al Worker que inicializaremos más adelante.
                val workData = workDataOf(
                    "urlKey" to url,
                    "nameKey" to title,
                    "taskIdKey" to workTag,
                    "thumbnailKey" to thumbnail,
                    "formatKey" to format,
                    "downloadModeKey" to downloadMode,
                    "downloadDirectoryKey" to downloadDirectory
                )
                //Se inicializa una petición de trabajo, tomando como base la clase 'DownloadWorker' creada dentro del mismo proyecto.
                val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                    .addTag(workTag)
                    .setInputData(workData)
                    .build()

                //Se envía la petición de trabajo dentro de la cola de trabajos de la aplicación
                //Donde nuestra clase 'DownloadWorker' va a recibir los datos dentro del objeto workData.
                wManager.enqueueUniqueWork(
                    workTag,
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )

                withContext(Dispatchers.Main){
                    //Se le envía un mensaje al usuario avisando que la descarga del video entro en cola.
                    Toast.makeText(
                        context,
                        R.string.download_queued,
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }catch(e: Exception){
                Log.e(videoDownload_TAG, "The exception caught while executing the process. (e)")
                e.printStackTrace()
                //Muestra un mensaje si ocurrio un error durante el proceso de descarga del video en yt-dlp
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        R.string.video_download_function_error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private val audioDownload_TAG: String = "audioDownload"
    //Función para descargar el audio del video proporcionado en la URL, en la mejor calidad.
    private fun audioDownload(url: String, videoFormat: String, formatFileConverted: String, downloadLocation: String, mediaInformation: VideoInfo){
        val downloadScope = CoroutineScope(Dispatchers.IO)
        downloadScope.launch {
            try {
                //Obtiene la información del video a partir del viewModel
                val audioInformation = mediaInformation
                //Guarda la información del video necesaria en distintas variables

                //Variable para especificar el formato del video/audio
                val format = videoFormat

                //Variable donde se guarda el formato final del archivo del audio convertido
                val formatFile = formatFileConverted

                //Se verifica si se especificó un formato de descarga
                if(format == "none"){
                    //Se verifica si se especifico un formato de archivo de descarga
                    if(formatFile == "none"){
                        Log.e(audioDownload_TAG, "You need to specify a formatFile if format = 'none'")
                        return@launch
                    }
                }

                //Variable donde se guarda el id del video
                val workTag = ((audioInformation.id).toString()) + "_audio_"+format+"_"+formatFile

                //Variable donde se guarda el título del video
                val title = (audioInformation.title).toString()

                //Variable para especificar un modo de descarga en el DownloadWorker
                val downloadMode = "audio"

                //Variable donde se guarda el URL de la vista previa del video
                val thumbnail = (audioInformation.thumbnail).toString()

                //Variable para especificar la ubicación de la descarga
                val downloadDirectory = downloadLocation

                //Aquí se verifica si el audio no fue descargado ya por el usuario
                //Primero se hace una verificación en la base de datos
                val downloadDao = AppDatabase.getDatabase(requireContext().applicationContext).DownloadDao()
                val itemCheck = downloadDao.getByIdentifier(workTag)
                if(itemCheck != null){
                    //En caso de que ya se encuentre en el historial de descargas, se le menciona al usaurio y se sale de la función
                    withContext(Dispatchers.Main){
                        Toast.makeText(
                            context,
                            R.string.media_already_downloaded,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                //Después se hace una verificación en los archivos del directorio para ver si el audio no se encuentra ya descargado
                //Se convierte la ubicación en String en un Uri
                val treeUri = Uri.parse(downloadDirectory)
                //Se le agrega al Uri la ubicación dentro de la carpeta 'TikTok Downloader CI'
                //Antes de ello se convierte el contentUri en File Uri con la función 'resolveContentUri', esto debido a que la ubicación que se guarda en la configuración global no tiene el formato adecuado
                val directory = File(URIUtils.resolveContentUri(treeUri, requireContext().applicationContext), getString(R.string.app_name))
                //Se busco si existe un archivo del audio dentro de la carpeta de descargas del usuario
                val file_audio = File(directory.absolutePath+"/tiktok@${audioInformation.uploader}_${audioInformation.displayId}_audio_"+formatFile+"."+formatFile)
                //En caso de que exista este archivo se ejecuta el siguiente código
                if(file_audio.exists()){
                    //Si el archivo ya existe, simplemente se agrega al historial de Descargas, ahorrandonos el tener que volver a descargarlo
                    val download = Download(title).apply {
                        imageUrl = thumbnail
                        downloadedPath = file_audio.path
                        downloadedPercent = 100.00
                        downloadedType = downloadMode
                        mediaUniqueIdentifier = workTag
                    }
                    downloadDao.insert(download)
                    //Se le indica al usuario que ya existe el archivo en el directorio y que simplemente se agregó al historial de descargas
                    withContext(Dispatchers.Main){
                        Toast.makeText(
                            context,
                            R.string.file_exits_in_directory,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    //Se sale de la función
                    return@launch
                }

                //Variable donde se inicializa el WorkManager
                //Este sera necesario para crear trabajos (Workers), que nos permitiran hacer tareas en segundo plano, sin la necesidad de tener abierta la aplicación
                val wManager = WorkManager.getInstance(requireContext().applicationContext)
                val state = wManager.getWorkInfosByTag(workTag).get()?.getOrNull(0)?.state
                val running = state === WorkInfo.State.RUNNING || state === WorkInfo.State.ENQUEUED
                //Se verifica que la descarga de un audio en específico no este ya en progreso
                //En caso de que lo este, se muestra un mensaje avisandole al usuario y se sale de la función
                if (running) {
                    withContext(Dispatchers.Main){
                        Toast.makeText(
                            context,
                            R.string.audio_download_in_progress,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                //Se inicializa el objeto workData, donde se guardarán pares de llavés que almacenarán variables con datos.
                //Esta lista de pares de llaves se le pasará al Worker que inicializaremos más adelante.
                val workData = workDataOf(
                    "urlKey" to url,
                    "nameKey" to title,
                    "taskIdKey" to workTag,
                    "thumbnailKey" to thumbnail,
                    "formatKey" to format,
                    "formatConvertedKey" to formatFile,
                    "downloadModeKey" to downloadMode,
                    "downloadDirectoryKey" to downloadDirectory
                )

                //Se inicializa una petición de trabajo, tomando como base la clase 'DownloadWorker' creada dentro del mismo proyecto.
                val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                    .addTag(workTag)
                    .setInputData(workData)
                    .build()

                //Se envía la petición de trabajo dentro de la cola de trabajos de la aplicación
                //Donde nuestra clase 'DownloadWorker' va a recibir los datos dentro del objeto workData.
                wManager.enqueueUniqueWork(
                    workTag,
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )

                //Se le envía un mensaje al usuario avisando que la descarga del audio entro en cola.
                withContext(Dispatchers.Main){
                    Toast.makeText(
                        context,
                        R.string.download_queued,
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(audioDownload_TAG, "The exception caught while executing the process. (e)")
                e.printStackTrace()
                //Muestra un mensaje si ocurrio un error durante el proceso de descarga del audio en yt-dlp
                withContext(Dispatchers.Main){
                    Toast.makeText(
                        context,
                        R.string.audio_download_function_error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
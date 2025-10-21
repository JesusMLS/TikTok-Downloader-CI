package com.example.tiktokdownloaderci.ui.settings

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.findNavController
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import com.example.tiktokdownloaderci.R
import com.example.tiktokdownloaderci.NavActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.tiktokdownloaderci.work.UpdateYoutubeDLWorker
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//Clase PreferenceFragmentCompat donde se especifica toda la lógica que tendrá cada preferencia dentro de root_preferences
class SettingsFragment : PreferenceFragmentCompat() {
    //Función que se manda a llamar cuando se estan creado las preferencias
    //Aquí se puede especificar la lógica que tendrá cada una de ellas
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        //Se obtienen las preferencias del archivo root_preferences.xml
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        //Preferencia 0 'Dark Theme'
        //Se obtiene la llave de la preferencia dark_theme_key
        val darkThemePref: SwitchPreferenceCompat? =
            findPreference(getString(R.string.dark_theme_key))
        darkThemePref?.let {
            //Se agrega un Listener para ver si el valor de la preferencia cambio
            //Este puede cambiar si el usuario manipula el Switch
            it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                //En caso de que el nuevo valor sea verdadero, se cambia la aplicación a un modo oscuro
                if(newValue == true) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    //En caso de que el nuevo valor sea falso, se verifica si la versión de Android del usuario es inferior a Android 10
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        //En caso de serlo, se especifica que se quiere que la aplicación cambie a modo oscuro solo si el sistema esta en modo batería.
                        AppCompatDelegate.setDefaultNightMode(
                            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                        )
                    } else {
                        //En caso de no serlo, se especifica que se quiere que la aplicación cambie a modo oscuro solo si el sistema esta en modo oscuro
                        AppCompatDelegate.setDefaultNightMode(
                            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        )
                    }
                }
                true
            }
        }

        //Preferencia 1 'Rate App'
        //Se obtiene la llave de la preferencia rate_key
        val rateAppPref: Preference? =
            findPreference(getString(R.string.rate_key))
        rateAppPref?.let {
            //En caso de que el usuario de click a la preferencia
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                //Se manda a llamar a la función 'openPlayStoreLink'
                openPlayStoreLink()
                true
            }
        }

        //Preferencia 2 'Share App'
        //Se obtiene la llave de la preferencia share_key
        val shareAppPref: Preference? =
            findPreference(getString(R.string.share_key))
        shareAppPref?.let {
            //En caso de que el usuario de click a la preferencia
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                //Se manda a llamar a la función 'sharePlayStoreLink'
                sharePlayStoreLink()
                true
            }
        }

        //Preferencia 3 'Privacy Policy'
        //Se obtiene la llave de la preferencia privacy_policy_key
        val privacyPolicyPref: Preference? =
            findPreference(getString(R.string.privacy_policy_key))
        privacyPolicyPref?.let {
            //En caso de que el usuario de click a la preferencia
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                //Se manda a llamar a la función 'openPrivacyPolicy'
                openPrivacyPolicy()
                true
            }
        }

        //Preferencia 4 'App Version'
        //Se obtiene la llave de la preferencia app_version_key
        val appVersionPref: Preference? =
            findPreference(getString(R.string.app_version_key))
        appVersionPref?.let {
            //Se obtiene la versión de la aplicación
            val packageInfo: PackageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            //Se actualiza el valor mostrado por la preferencia por la versión de la aplicación
            it.summary = packageInfo.versionName
        }

        //Launcher getDirectoryResult, el cual abre una actividad especificada por el usuario y nos permite ver su código de resultado
        val getDirectoryResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            //Se verifica si el código de resultado es RESULT_OK
            if(it.resultCode == Activity.RESULT_OK){
                //En caso de serlo se obtienen permisos persistentes de URI, los cuales se mantendrán incluso cuando se reinicie el dispositivo.
                it.data?.data?.let {
                    activity?.contentResolver?.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                //Se guarda la ubicación obtenida por la actividad a partir de una llamada a la función 'updateDownloadLocation'
                it.data?.dataString?.let { it1 -> updateDownloadLocation(it1) }
            }
        }
        //Preferencia 5 'Download Location'
        //Se obtiene la llave de la preferencia download_location_key
        val downloadLocationPref: Preference? =
            findPreference(getString(R.string.download_location_key))
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        downloadLocationPref?.let {
            //Se obtiene la ubicación de descarga que se encuentra guardada en la llave 'download_location_key'
            //En caso de no encontrarse ninguna, se establece la variable como null
            val downloadLocation = sharedPrefs.getString(getString(R.string.download_location_key), null)
            downloadLocation?.apply {
                //En caso de que downloadLocation no sea nulo, se manda a llamar a la función 'updateDownloadPathSummary'
                updateDownloadPathSummary(it, this)
            } ?: it.setSummary(R.string.value_not_set) //En caso de que lo sea, se cambia el resumen de la preferencia por 'Not Set'
            //En caso de que el usuario de click a la preferencia
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                //Se manda a llamar a la función 'openDirectoryChooser'
                openDirectoryChooser(getDirectoryResult)
                true
            }
        }

        //Preferencia 6 'Update Channel'
        //Se obtiene la llave de la preferencia update_channel_key
        val updateChannelPref: ListPreference? =
            findPreference(getString(R.string.update_channel_key))

        //Preferencia 7 'Update yt-dlp'
        //Se obtiene la llave de la preferencia update_youtubedl_key
        val updateYTDLPref: Preference? =
            findPreference(getString(R.string.update_youtubedl_key))
        updateYTDLPref?.let {
            //Se cambia el resumen de la preferencia por la versión de yt-dlp que se encuentre en la aplicación
            //En caso de que se cuente con una versión muy vieja de la herramienta, se mostrará el mensaje 'Tap to update the tool'
            it.summary = YoutubeDL.getInstance().versionName(requireContext().applicationContext) ?: getString(R.string.update_summary)
            //En caso de que el usuario de click a la preferencia
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                //Si el valor de la llave update_channel_key no es nulo, se ejecuta el siguiente código
                if(updateChannelPref != null){
                    //Se manda a llamar a la función 'updateYoutubeDL', la cual se encarga de actualizar la herramienta yt-dlp
                    updateYoutubeDL(updateChannelPref.value)
                }
                true
            }
        }
    }

    //Función que se encarga de abrir un link que lleva al usuario a la página de Google Play Store de la aplicación
    private fun openPlayStoreLink(){
        val applicationPackageName = requireActivity().packageName
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$applicationPackageName"))
        startActivity(
            intent
        )
    }
    //Función que se encarga de abrir un menú que le muestra al usuario opciones donde puede compartir el url de la página de Google Play Store de la aplicación
    private fun sharePlayStoreLink(){
        val applicationPackageName = requireContext().packageName
        val applicationLink = "https://play.google.com/store/apps/details?id=${applicationPackageName}"
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, applicationLink)
        intent.type = "text/plain"
        val shareIntent: Intent = Intent.createChooser(intent, null)
        startActivity(
            shareIntent
        )
    }

    //Función que se encarga de abrir el fragmento de Privacy Policy en una nueva pestaña
    private fun openPrivacyPolicy(){
        try{
            findNavController().navigate(R.id.action_settings_to_privacy_Policy)
        }catch(e: Exception){
            e.printStackTrace()
        }
    }

    //Función que se encarga se actualizar el resumen de la preferencia de acuerdo a la ubicación de la carpeta de descargas
    private fun updateDownloadPathSummary(preference: Preference, path: String){
        val documentId = DocumentsContract.getTreeDocumentId(Uri.parse(path))
        documentId?.apply {
            preference.summary = documentId
        } ?: run {
            preference.summary = path
        }
    }

    //Función que se encarga de abrir la actividad para seleccionar la carpeta de descargas que utilizará la aplicación
    private fun openDirectoryChooser(getDirectoryResult: ActivityResultLauncher<Intent>) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }
        //Se agregó un aviso para los usuarios que esten utilizando la aplicación desde Android 11++
        //Esto debido a que a partir de Android 11 no es posible escribir en carpetas externas a la aplicación que no sean /Download o /Documents
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            Toast.makeText(
                context,
                R.string.scoped_storage_1,
                Toast.LENGTH_SHORT
            ).show()
            Toast.makeText(
                context,
                R.string.scoped_storage_2,
                Toast.LENGTH_SHORT
            ).show()
        }
        getDirectoryResult.launch(intent)
    }

    //Función que se encarga de actualizar la ubicación de descarga que se encuentra dentro de la preferencia global 'download_location_key'
    private fun updateDownloadLocation(path: String){
        val preferenceEditor = PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
        preferenceEditor.putString(getString(R.string.download_location_key), path).apply()
        //Después de actualizar la ubicación, se procede a actualizar el resumen de la preferencia
        findPreference<Preference>(getString(R.string.download_location_key))?.let { preference ->
            updateDownloadPathSummary(preference, path)
        }
    }

    private val updateYoutubeDL_TAG: String = "updateYoutubeDL"
    //Función para actualizar la herramienta yt-dlp
    private fun updateYoutubeDL(updateChannelPreference: String){
        val updateScope = CoroutineScope(Dispatchers.IO)
        //Se indica en el código que lo siguiente se desea ejecutar en el Hilo IO.
        updateScope.launch {
            try {
                //Variable para especificar el canal de actualización
                val YoutubeDLUpdateChannel = updateChannelPreference

                //Variable donde se guarda el identificador para el trabajo de actualización de la herramienta
                val workTag = "tiktok_update_worker"

                //Variable donde se inicializa el WorkManager
                //Este sera necesario para crear trabajos (Workers), que nos permitiran hacer tareas en segundo plano, sin la necesidad de tener abierta la aplicación
                val wManager = WorkManager.getInstance(requireContext().applicationContext)
                val state = wManager.getWorkInfosByTag(workTag).get()?.getOrNull(0)?.state
                val running = state === WorkInfo.State.RUNNING || state === WorkInfo.State.ENQUEUED

                //Se verifica que la actualización de la herramienta no este ya en progreso
                //En caso de que lo este, se muestra un mensaje avisandole al usuario y se sale de la función
                if (running) {
                    withContext(Dispatchers.Main){
                        Toast.makeText(
                            context,
                            R.string.update_in_progress,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                //Se inicializa el objeto workData, donde se guardarán pares de llavés que almacenarán variables con datos.
                //Esta lista de pares de llaves se le pasará al Worker que inicializaremos más adelante.
                val workData = workDataOf(
                    "updateChannelKey" to YoutubeDLUpdateChannel,
                )

                //Se inicializa una petición de trabajo, tomando como base la clase 'UpdateYoutubeDLWorker' creada dentro del mismo proyecto.
                val workRequest = OneTimeWorkRequestBuilder<UpdateYoutubeDLWorker>()
                    .addTag(workTag)
                    .setInputData(workData)
                    .build()

                //Se envía la petición de trabajo dentro de la cola de trabajos de la aplicación
                wManager.enqueueUniqueWork(
                    workTag,
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )

                //Se le envía un mensaje al usuario avisando que la actualización entro en cola
                withContext(Dispatchers.Main){
                    Toast.makeText(
                        context,
                        R.string.update_queued,
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(updateYoutubeDL_TAG, "The exception caught while executing the process. (e)")
                e.printStackTrace()
                //Muestra un mensaje si ocurrio un error durante el proceso de actualización de yt-dlp
                withContext(Dispatchers.Main){
                    Toast.makeText(
                        context,
                        R.string.update_function_error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    //Función que se ejecuta después de la creación de la vista
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Se oculta la barra de navegación inferior
        (activity as? NavActivity)?.hideNav()
        //Requerido para actualizar el menú de opciones
        requireActivity().invalidateOptionsMenu()
    }

    //Función que se oculta al parar el fragmento
    override fun onStop() {
        super.onStop()
        (activity as? NavActivity)?.showNav()
    }

    //Función que se ejecuta cuando el fragmento es visible al usuario
    override fun onResume() {
        super.onResume()
        (activity as? NavActivity)?.hideNav()
    }

}
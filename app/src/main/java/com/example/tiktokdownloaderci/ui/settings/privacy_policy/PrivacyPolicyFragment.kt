package com.example.tiktokdownloaderci.ui.settings.privacy_policy

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.tiktokdownloaderci.NavActivity
import com.example.tiktokdownloaderci.databinding.FragmentPrivacyPolicyBinding

//Clase PrivacyPolicyFragment, donde se especifica toda la lógica para el fragmento fragment_privacy_policy
class PrivacyPolicyFragment : Fragment() {

    private var _binding: FragmentPrivacyPolicyBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    //En esta función se define y configura la interfaz de la vista del fragmento (durante su creación)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //Se crea una instancia del ViewModel privacyPolicyViewModel o se obtiene el existente
        val privacyPolicyViewModel =
            ViewModelProvider(this).get(PrivacyPolicyViewModel::class.java)

        _binding = FragmentPrivacyPolicyBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    //En esta función se define y configura la interfaz de la vista del fragmento (después de ser creada)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Se oculta la barra inferior de navegación
        (activity as? NavActivity)?.hideNav()
        //Requerido para actualizar el menú de opciones
        requireActivity().invalidateOptionsMenu()
    }

    //Función que ejecuta el código dentro de ella antes de que se destruya la vista
    override fun onDestroyView() {
        super.onDestroyView()
        //Se muestra la barra inferior de navegación
        (activity as? NavActivity)?.showNav()
        _binding = null
    }

}
package com.example.tiktokdownloaderci.ui.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tiktokdownloaderci.databinding.FragmentHelpBinding

//Clase HelpFragment, donde se especifica toda la lógica para el fragmento fragment_help
class HelpFragment : Fragment() {

    private var _binding: FragmentHelpBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    //En esta función se define y configura la interfaz de la vista del fragmento
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //Se crea una instancia del ViewModel HelpViewModel o se obtiene el existente
        val helpViewModel =
            ViewModelProvider(this).get(HelpViewModel::class.java)

        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //Requerido para actualizar el menú de opciones
        requireActivity().invalidateOptionsMenu()
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
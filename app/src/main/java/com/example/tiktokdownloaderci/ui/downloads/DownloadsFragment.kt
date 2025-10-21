package com.example.tiktokdownloaderci.ui.downloads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.tiktokdownloaderci.adapters.DownloadsAdapter
import com.example.tiktokdownloaderci.databinding.FragmentDownloadsBinding

//Clase DownloadsFragment, donde se especifica toda la lógica para el fragmento fragment_downloads
class DownloadsFragment : Fragment() {

    //Se crea una variable downloadsAdapter del tipo DownloadsAdapter, con el propósito de inicializarla más adelante
    private lateinit var downloadsAdapter: DownloadsAdapter

    private var _binding: FragmentDownloadsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    //En esta función se define y configura la interfaz de la vista del fragmento
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //Se crea una instancia del ViewModel DownloadsViewModel o se obtiene el existente
        val downloadsViewModel =
            ViewModelProvider(this).get(DownloadsViewModel::class.java)

        _binding = FragmentDownloadsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //Requerido para actualizar el menú de opciones
        requireActivity().invalidateOptionsMenu()

        //Se inicializa la clase DownloadsAdapter, con el propósito de trabajar con su contenido
        downloadsAdapter = DownloadsAdapter()

        //Se especifica downloadsAdapter como un adaptador para el RecyclerView, con el propósito de proveer una vista dinámica para cada item.
        binding.downloadsRecyclerView.adapter = downloadsAdapter

        //Se inicializa un observador para las Descargas dentro de 'allDownloads'
        //Con el propósito de que se actualice la vista dinámicamente, reflejandose si se añadio, eliminó o cambio alguna descarga.
        downloadsViewModel.allDownloads.observe(viewLifecycleOwner) { downloads ->
            downloads?.let {
                //Se le manda la lista de descargas al adaptador, para que trabaje con cada una de ellas
                downloadsAdapter.submitList(downloads)
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
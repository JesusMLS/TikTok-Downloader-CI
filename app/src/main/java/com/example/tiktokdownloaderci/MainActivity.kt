package com.example.tiktokdownloaderci

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.iterator
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.tiktokdownloaderci.databinding.ActivityMainBinding
import com.example.tiktokdownloaderci.ui.settings.SettingsFragment
import com.example.tiktokdownloaderci.ui.settings.privacy_policy.PrivacyPolicyFragment


//Clase AppCompatActivity, donde se maneja toda la lógica de los fragmentos
//Lo que incluye la barra de navegación inferior, superior, etc.
//Es lo equivalente a la lógica para la actividad principal activity_main.xml
class MainActivity : AppCompatActivity(), NavActivity {

    private lateinit var binding: ActivityMainBinding

    //Función que se ejecuta cuando la actividad esta iniciando
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Variable donde se guarda la vista navView, con la que se va a trabajar mas adelante
        //Es la barra de navegación inferior que veremos en la aplicación
        val navView: BottomNavigationView = binding.navView

        //Variable donde se guarda el NavHostFragment, que es la vista donde se muestran todos los fragmentos de la aplicación
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment

        //Variable donde se guarda el navController, que es lo que se va a utilizar para navegar entre fragmentos
        val navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_downloads, R.id.navigation_help
            )
        )
        //Metodo que permitirá que el titulo de la barra superior se actualice de acuerdo al fragmento en el que se encuentre el usuario
        setupActionBarWithNavController(navController, appBarConfiguration)

        //Metodo donde se configura el NavigationBarView para que funcione con un set de fragmentos ya especifcados por el navController
        navView.setupWithNavController(navController)
    }

    //Se inicializan las opciones del menú superior de la aplicación
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    //Función donde se agrega lógica dependiendo de la opción que se elija de la barra de navegación
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //Requerido para el funcionamiento del boton de navegación hacia arriba
        if(item.itemId == android.R.id.home){
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        //Aquí se especifica que la aplicación navegue a la opción seleccionada por el usuario en la barra inferior (el fragmento en específico)
        val navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main)
        val navigated = NavigationUI.onNavDestinationSelected(item, navController)

        return navigated || super.onOptionsItemSelected(item)
    }

    //Función donde se prepará el menú de opciones
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        //Aquí se especifica que se quiere ocultar el menú de opciones superior solo si el fragmento actual es 'SettingsFragment' o 'PrivacyPolicyFragment'
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.fragments.lastOrNull()
        val hideBoolean : Boolean = (currentFragment !is SettingsFragment) && (currentFragment !is PrivacyPolicyFragment)
        menu?.iterator()?.forEach {
            it.isVisible = hideBoolean
        }
        return super.onPrepareOptionsMenu(menu)
    }

    //Esta función permite ocultar la barra de navegación inferior
    override fun hideNav(){
        binding.navView.visibility = View.GONE
    }

    //Esta función permite mostrar la barra de navegación inferior
    override fun showNav(){
        binding.navView.visibility = View.VISIBLE
    }
}

interface NavActivity {
    fun hideNav()
    fun showNav()
}
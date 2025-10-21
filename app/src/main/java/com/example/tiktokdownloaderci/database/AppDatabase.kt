package com.example.tiktokdownloaderci.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

//Clase abstracta donde se inicializa la base de datos y se gestionan todas las operaciones relacionadas con la base de datos
//La base de datos SQLite es gestionada por Room
@Database(entities = [Download::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase(){
    //Se define el DAO para la interacción con las tablas
    abstract fun DownloadDao() : DownloadDao

    //El patrón de diseño Singleton prevee que multiples instancias de la base de datos se abran al mismo tiempo.
    companion object {
        @Volatile
        //Variable donde se guarda la nueva instancia de la base de datos
        private var INSTANCE: AppDatabase? = null

        //Variable donde se guarda el nombre de la base de datos
        private const val database_name = "tdci_db"

        //Función que regresa la instancia de la base de datos
        fun getDatabase (context: Context): AppDatabase{
            val temporalInstance = INSTANCE
            //En caso de que la instancia ya este creada, se regresa esa instancia
            if(temporalInstance != null){
                return temporalInstance
            }
            //En caso de que la instancia no este creada, se crea una nueva instancia de la base de datos y se regresa esa nueva instancia
            synchronized(this){
                val newInstance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, database_name)
                    .build()
                INSTANCE = newInstance
                return newInstance
            }
        }
    }
}
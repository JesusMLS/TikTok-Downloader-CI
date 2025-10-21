package com.example.tiktokdownloaderci.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

//En esta interfáz se especifica cada método que nos permitirá interactuar con las tablas dentro de la base de datos
@Dao
interface DownloadDao {
    //Este método nos permitirá obtener todas las entradas dentro de la tabla 'downloads'
    @Query("SELECT * FROM downloads")
    fun getAll(): LiveData<List<Download>>

    //Este método nos permitirá obtener una entrada en especifico dentro de 'downloads' especificada por el id
    @Query("SELECT * FROM downloads WHERE id = :id")
    fun getById(id: Long): Download

    //Este método nos permitirá obtener una entrada en especifico dentro de 'downloads' especificada por el media_unique_identifier
    @Query("SELECT * FROM downloads WHERE media_unique_identifier=:identifier")
    fun getByIdentifier(identifier: String) : Download?

    //Este método nos permitirá insertar nuevas entradas dentro de la tabla 'downloads'
    @Insert
    suspend fun insert(item: Download)

    //Este método nos permitirá actualizar entradas dentro de la tabla 'downloads'
    @Update
    suspend fun update(item: Download)

    //Este método nos permitirá eliminar entradas dentro de la tabla 'downloads'
    @Delete
    suspend fun delete(item: Download)
}
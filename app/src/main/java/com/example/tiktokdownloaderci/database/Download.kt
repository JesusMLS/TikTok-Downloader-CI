package com.example.tiktokdownloaderci.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

//En esta clase se especifica la estructura de la tabla 'downloads'
// name -> Aquí se guardará el título del video/audio
// id -> Aquí se guardará el id autogenerado para la entrada en la tabla
// image_url -> Aquí se guardará el url de la ubicación en línea donde se encuentra la vista previa del video
// downloaded_path -> Aquí se guardará la ubicación real del archivo descargado por la aplicación
// downloaded_type -> Aquí se guardará si el archivo descargado es un 'video' o un 'audio'
// media_unique_identifier -> Aquí se guardará el identificador unico para cada video
@Entity(tableName = "downloads")
data class Download(
    @ColumnInfo(name = "name")
    val name: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @ColumnInfo(name = "image_url")
    lateinit var imageUrl: String

    @ColumnInfo(name = "downloaded_path")
    lateinit var downloadedPath: String

    @ColumnInfo(name = "downloaded_percent")
    var downloadedPercent: Double = 0.0

    @ColumnInfo(name = "downloaded_type")
    lateinit var downloadedType: String

    @ColumnInfo(name = "media_unique_identifier")
    lateinit var mediaUniqueIdentifier: String
}

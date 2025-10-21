package com.example.tiktokdownloaderci.utils

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File
import android.content.Context

object URIUtils {
    //Función para convertir una Content URI en una File URI
    fun resolveContentUri(uri: Uri, context: Context): String{
        val docId = DocumentsContract.getTreeDocumentId(uri)
        val destinationDirectory = DocumentsContract.buildDocumentUriUsingTree(uri, docId)
        val documentCursor = context.contentResolver.query(destinationDirectory, null, null, null, null)

        var str = ""

        //Obtenemos un string en forma de primary:Folder_name o SDCard_name:Folder_name
        while(documentCursor!!.moveToNext()){
            str = documentCursor.getString(0)
            if(str.matches(Regex(".*:.*"))) break
        }

        documentCursor.close()

        val split = str.split(":")

        val base: File =
            if(split[0] == "primary") Environment.getExternalStorageDirectory()
            else File("/storage/${split[0]}")

        //En caso de que el URI especificado no se pueda convertir en un directorio valido, se manda un error
        if(!base.isDirectory){
            throw Exception("$uri can't be resolved in a valid path")
        }

        //En caso de que si se pueda convertir, se regresa el File URI
        return File(base,split[1]).canonicalPath
    }
}
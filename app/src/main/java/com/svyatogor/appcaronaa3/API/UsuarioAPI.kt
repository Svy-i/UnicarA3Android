package com.svyatogor.appcaronaa3.API

import com.svyatogor.appcaronaa3.Model.Usuario
import retrofit2.Call
import retrofit2.http.*

interface UsuarioApi {

    @POST("api/usuarios")
    fun criarUsuario(@Body usuario: Usuario): Call<Usuario>

    @GET("api/usuarios/{id}")
    fun buscarUsuario(@Path("id") id: Long): Call<Usuario>
}

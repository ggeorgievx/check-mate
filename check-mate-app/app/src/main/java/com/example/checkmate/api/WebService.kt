package com.example.checkmate.api

import android.content.Context
import com.example.checkmate.data.model.BillDetailsResponse
import com.example.checkmate.data.model.BillSession
import com.example.checkmate.data.model.PayRequest
import com.example.checkmate.data.model.SelectItemRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

class CreateBillRequest(val base64: String)

class JoinSessionRequest(val id:Long)

interface Webservice {

    @POST("/new")
    fun createNewBill(@Body request: CreateBillRequest): Call<BillSession>

    @POST("/join")
    fun joinBillSession(@Body request: JoinSessionRequest): Call<BillSession>

    @POST("/selectItem")
    fun selectItem(@Body selectedItem: SelectItemRequest): Call<Unit>

    @GET("/getBillDetails")
    fun getBillDetails(@Query("id") billId: Long): Call<BillDetailsResponse>

    @POST("/pay")
    fun payBill(@Body payRequest: PayRequest): Call<Unit>

    companion object {
        fun create(context: Context): Webservice {
//            val clientBuilder = OkHttpClient.Builder()
//            val client = OkHttpClient.Builder()
//                .addInterceptor(ApiHeadersInterceptor(context))
//                .authenticator(ApiAuthenticator(context))
//                .build()

            val retrofit = Retrofit.Builder()
                .addCallAdapterFactory(
                    RxJava2CallAdapterFactory.create()
                )
                .addConverterFactory(
                    MoshiConverterFactory.create()
                )
                .baseUrl("https://give-a-shit-check-mate.herokuapp.com")
                .client(getUnsafeOkHttpClient(context))
                .build()

            return retrofit.create(Webservice::class.java)
        }

        private fun getUnsafeOkHttpClient(context: Context): OkHttpClient {
            try {
                // Create a trust manager that does not validate certificate chains
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }

                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(
                        chain: Array<java.security.cert.X509Certificate>,
                        authType: String
                    ) {
                    }

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(
                        chain: Array<java.security.cert.X509Certificate>,
                        authType: String
                    ) {
                    }
                })

                // Install the all-trusting trust manager
                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                // Create an ssl socket factory with our all-trusting manager
                val sslSocketFactory = sslContext.getSocketFactory()

                val loggingInterceptor = HttpLoggingInterceptor()

                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

                val builder = OkHttpClient.Builder().apply {
                    this.addInterceptor(loggingInterceptor)
                    this.addInterceptor(ApiHeadersInterceptor(context))
                    //this.authenticator(ApiAuthenticator(context))
                }

                builder.sslSocketFactory(sslSocketFactory)
                builder.hostnameVerifier(object : HostnameVerifier {
                    override fun verify(hostname: String, session: SSLSession): Boolean {
                        return true
                    }
                })

                builder.readTimeout(170, TimeUnit.SECONDS)

                return builder.build()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

        }
    }
}
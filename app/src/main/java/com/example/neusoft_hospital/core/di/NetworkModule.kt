package com.example.neusoft_hospital.core.di

import com.example.neusoft_hospital.BuildConfig
import com.example.neusoft_hospital.core.network.ApiProvider
import com.example.neusoft_hospital.core.network.AuthInterceptor
import com.example.neusoft_hospital.core.network.RefreshApi
import com.example.neusoft_hospital.core.network.TokenAuthenticator
import com.example.neusoft_hospital.feature.ai.data.AiChatApiServiceRetrofit
import com.example.neusoft_hospital.feature.ai.data.QwenApiService
import com.example.neusoft_hospital.feature.appointment.data.AppointmentApiService
import com.example.neusoft_hospital.feature.appointment.data.AppointmentApiServiceRetrofit
import com.example.neusoft_hospital.feature.appointment.data.MockAppointmentApi
import com.example.neusoft_hospital.feature.appointment.data.RetrofitAppointmentApi
import com.example.neusoft_hospital.feature.auth.data.AuthApiService
import com.example.neusoft_hospital.feature.auth.data.AuthApiServiceRetrofit
import com.example.neusoft_hospital.feature.auth.data.FamilyApiService
import com.example.neusoft_hospital.feature.auth.data.FamilyApiServiceRetrofit
import com.example.neusoft_hospital.feature.auth.data.MockAuthApi
import com.example.neusoft_hospital.feature.auth.data.MockFamilyApi
import com.example.neusoft_hospital.feature.auth.data.RetrofitAuthApi
import com.example.neusoft_hospital.feature.auth.data.RetrofitFamilyApi
import com.example.neusoft_hospital.feature.followup.data.ChronicApiServiceRetrofit
import com.example.neusoft_hospital.feature.followup.data.FollowUpApiServiceRetrofit
import com.example.neusoft_hospital.feature.preconsult.data.PreConsultApiServiceRetrofit
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Authed

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Unauthenticated

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides @Singleton @Authed
    fun provideOkHttp(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    /** Separate client without auth interceptor; used only for /api/auth/refresh. */
    @Provides @Singleton @Unauthenticated
    fun provideRefreshOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton
    fun provideRetrofit(@Authed okhttp: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(ApiProvider.backendBaseUrl)
            .client(okhttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides @Singleton
    fun provideRefreshApi(@Unauthenticated refreshOkHttp: OkHttpClient, moshi: Moshi): RefreshApi =
        Retrofit.Builder()
            .baseUrl(ApiProvider.backendBaseUrl)
            .client(refreshOkHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RefreshApi::class.java)

    @Provides @Singleton
    fun provideQwenApi(retrofit: Retrofit): QwenApiService = retrofit.create(QwenApiService::class.java)

    // ------------------- Auth -------------------

    @Provides @Singleton
    fun provideAuthRetrofit(retrofit: Retrofit): AuthApiServiceRetrofit =
        retrofit.create(AuthApiServiceRetrofit::class.java)

    /**
     * Single binding point: Repositories inject [AuthApiService], and this
     * provider returns either the mock impl (no network) or the real Retrofit
     * adapter based on [ApiProvider.useMock].
     */
    @Provides @Singleton
    fun provideAuthApi(
        mock: dagger.Lazy<MockAuthApi>,
        real: dagger.Lazy<RetrofitAuthApi>
    ): AuthApiService = if (ApiProvider.useMock) mock.get() else real.get()

    // ------------------- Family -------------------

    @Provides @Singleton
    fun provideFamilyRetrofit(retrofit: Retrofit): FamilyApiServiceRetrofit =
        retrofit.create(FamilyApiServiceRetrofit::class.java)

    @Provides @Singleton
    fun provideFamilyApi(
        mock: dagger.Lazy<MockFamilyApi>,
        real: dagger.Lazy<RetrofitFamilyApi>
    ): FamilyApiService = if (ApiProvider.useMock) mock.get() else real.get()

    // ------------------- Appointment -------------------

    @Provides @Singleton
    fun provideAppointmentRetrofit(retrofit: Retrofit): AppointmentApiServiceRetrofit =
        retrofit.create(AppointmentApiServiceRetrofit::class.java)

    @Provides @Singleton
    fun provideAppointmentApi(
        mock: dagger.Lazy<MockAppointmentApi>,
        real: dagger.Lazy<RetrofitAppointmentApi>
    ): AppointmentApiService = if (ApiProvider.useMock) mock.get() else real.get()

    // ------------------- PreConsult -------------------

    @Provides @Singleton
    fun providePreConsultApi(retrofit: Retrofit): PreConsultApiServiceRetrofit =
        retrofit.create(PreConsultApiServiceRetrofit::class.java)

    // ------------------- AiChat -------------------

    @Provides @Singleton
    fun provideAiChatApi(retrofit: Retrofit): AiChatApiServiceRetrofit =
        retrofit.create(AiChatApiServiceRetrofit::class.java)

    // ------------------- FollowUp + Chronic -------------------

    @Provides @Singleton
    fun provideFollowUpApi(retrofit: Retrofit): FollowUpApiServiceRetrofit =
        retrofit.create(FollowUpApiServiceRetrofit::class.java)

    @Provides @Singleton
    fun provideChronicApi(retrofit: Retrofit): ChronicApiServiceRetrofit =
        retrofit.create(ChronicApiServiceRetrofit::class.java)
}

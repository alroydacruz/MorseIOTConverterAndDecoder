package com.alroy.morsecodedetector.di

import android.content.Context
import com.alroy.morsecodedetector.R
import com.alroy.morsecodedetector.repository.MainRepository
import com.alroy.morsecodedetector.repository.MainRepositoryImp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped


@Module
@InstallIn(ActivityRetainedComponent::class)
object AuthModule {

    @ActivityRetainedScoped
    @Provides
    fun getGoogleSignInOptions(@ApplicationContext context: Context): GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.webclient_id))
            .requestEmail()
            .requestProfile()
            .build()

    @ActivityRetainedScoped
    @Provides
    fun getGoogleSignInClient(
        @ApplicationContext context: Context,
        options: GoogleSignInOptions
    ): GoogleSignInClient =
        GoogleSignIn.getClient(context, options)

    @ActivityRetainedScoped
    @Provides
    fun provideAuthRepository(@ApplicationContext context: Context) =
        MainRepositoryImp(context) as MainRepository

}
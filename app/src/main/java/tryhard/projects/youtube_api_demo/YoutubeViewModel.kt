package tryhard.projects.youtube_api_demo

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeScopes
import com.google.api.services.youtube.model.VideoListResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class YoutubeViewModel : ViewModel() {
    private val _state = MutableStateFlow(State.CHECKING_CONTACTS_PERMISSION)
    val state = _state.asStateFlow()

    private val _likeVideos = MutableStateFlow<VideoListResponse?>(null)
    val likeVideos = _likeVideos.asStateFlow()

    private lateinit var credential: GoogleAccountCredential
    private lateinit var accessToken: String

    fun contactsPermissionGranted() {
        credential = GoogleAccountCredential.usingOAuth2(
            Application.context, listOf(YouTubeScopes.YOUTUBE_READONLY)
        ).setBackOff(
            ExponentialBackOff()
        )
        _state.value = State.SELECTING_ACCOUNT
    }

    fun showSelectAccountPrompt(callback: (Intent) -> Unit) {
        callback.invoke(credential.newChooseAccountIntent())
    }

    fun chooseAccount(accountName: String) {
        credential.selectedAccountName = accountName
        _state.value = State.CHECK_NEED_CONSENT
    }

    fun checkNeedConsent(needConsentCallback: (Intent) -> Unit) {
        // Get token is a task need to be run in coroutine
        viewModelScope.launch(Dispatchers.IO) {
            try {
                runBlocking { accessToken = credential.token }
                // We get our token, ready for youtube call
                _state.value = State.ALL_SET
            } catch (e: UserRecoverableAuthException) {
                // We can not get token, show prompt to user for consent
                if (e.intent != null) {
                    needConsentCallback(e.intent!!)
                }
            } catch (e: GoogleAuthException) {
                // This email is not in Test User
                _state.value = State.SELECTING_ACCOUNT
            }
        }
    }

    // Same with checkNeedConsent, but we should get token for sure.
    fun revokeConsent() {
        // Get token is a task need to be run in coroutine
        viewModelScope.launch(Dispatchers.IO) {
            try {
                runBlocking { accessToken = credential.token }
                // We get our token, ready for youtube call
                _state.value = State.ALL_SET
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getMyLikeVideos() {
        viewModelScope.launch(Dispatchers.IO) {

            val youtube = YouTube.Builder(
                NetHttpTransport.Builder().build(),
                // For version 2.0.0
                GsonFactory.getDefaultInstance(),
                // For version rev183-1.22.0
//                JacksonFactory.getDefaultInstance(),
                null
            ).build()

            // https://developers.google.com/youtube/v3/docs/videos/list
            _likeVideos.value = youtube.videos()
                // For version 2.0.0
                .list(listOf("snippet", "contentDetails", "statistics"))
                // For version rev183-1.22.0
//                .list("snippet, contentDetails")
                // Both setAccessToken and setOauthToken works
//                .setAccessToken(accessToken)
                .setOauthToken(accessToken)
                .setMaxResults(50L)
                .setMyRating("like")
                .execute()
        }
    }
}

enum class State {
    CHECKING_CONTACTS_PERMISSION,
    SELECTING_ACCOUNT,
    CHECK_NEED_CONSENT,
    ALL_SET,
}
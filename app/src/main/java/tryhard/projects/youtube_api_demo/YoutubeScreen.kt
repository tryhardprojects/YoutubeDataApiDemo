package tryhard.projects.youtube_api_demo

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun YoutubeScreen() {
    val viewmodel = viewModel<YoutubeViewModel>()
    val state by viewmodel.state.collectAsState()

    // A launcher fot account chooser
    val selectAccountLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            // If failed or cancel, we still have a action button to prompt again.
            if (it.resultCode == RESULT_OK && it.data != null && it.data!!.extras != null) {
                val accountName: String =
                    it.data!!.extras!!.getString(AccountManager.KEY_ACCOUNT_NAME)!!

                viewmodel.chooseAccount(accountName)
            }
        }

    // For user consent
    val revokeLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            // If failed or cancel, we still have a action button to prompt again.
            if (it.resultCode == RESULT_OK) {
                viewmodel.revokeConsent()
            }
        }

    when (state) {
        State.CHECKING_CONTACTS_PERMISSION -> {
            val selectAccountPermissionState =
                rememberPermissionState(permission = Manifest.permission.GET_ACCOUNTS)

            // Check permission grated or not
            when (selectAccountPermissionState.status) {
                is PermissionStatus.Denied -> {
                    // Permission is denied, show a button to request permission
                    val textToShow =
                        if ((selectAccountPermissionState.status as PermissionStatus.Denied).shouldShowRationale) {
                            "I know you denied, but here's why we need this permission ..."
                        } else {
                            "Need permission to proceed"
                        }
                    ShowAction(textToShow, "Check Permission") {
                        selectAccountPermissionState.launchPermissionRequest()
                    }
                }

                PermissionStatus.Granted -> {
                    viewmodel.contactsPermissionGranted()
                }
            }
        }

        State.SELECTING_ACCOUNT -> {
            val chooseAccountIntentCallback = { intent: Intent ->
                selectAccountLauncher.launch(intent)
            }
            ShowAction("Choose a account", "Select Account") {
                viewmodel.showSelectAccountPrompt(chooseAccountIntentCallback)
            }
            viewmodel.showSelectAccountPrompt(chooseAccountIntentCallback)
        }

        State.CHECK_NEED_CONSENT -> {
            val revokeIntentCallback = { intent: Intent ->
                revokeLauncher.launch(intent)
            }
            ShowAction("Need user consent", "Show Consent Prompt") {
                viewmodel.checkNeedConsent(revokeIntentCallback)
            }
            viewmodel.checkNeedConsent(revokeIntentCallback)
        }

        State.ALL_SET -> {
            viewmodel.getMyLikeVideos()

            MyLikeVideos(viewmodel)
        }
    }
}

@Composable
fun MyLikeVideos(viewmodel: YoutubeViewModel) {
    val videos by viewmodel.likeVideos.collectAsState()

    if (videos == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
        }
    } else {
        LazyColumn {
            items(videos!!.items) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = it.snippet.title)
                    Text(text = it.snippet.channelTitle)
                }
            }
        }
    }
}

@Composable
fun ShowAction(text: String, buttonText: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text)
        Button(onClick = onClick) {
            Text(text = buttonText)
        }
    }
}
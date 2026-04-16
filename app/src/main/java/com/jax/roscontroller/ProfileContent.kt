package com.jax.roscontroller

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jax.roscontroller.ui.theme.JaxGamepadTheme
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jax.roscontroller.ui.theme.MyColors

@Composable
fun ProfileContent(
    onBack: () -> Unit = {},
    onProfileCreated: (UserProfile) -> Unit = {},
    previewMode: Boolean = false,
    isLoginMode: Boolean = true,
    onModeChange: (Boolean) -> Unit = {},
    submitTrigger: Int = 0,
    onLoadingChange: (Boolean) -> Unit = {},
    onStatusChange: (String) -> Unit = {}
) {
    val auth = if (!previewMode) FirebaseAuth.getInstance() else null
    val db = if (!previewMode) FirebaseFirestore.getInstance() else null

    var displayName by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var loading by rememberSaveable { mutableStateOf(false) }
    var statusMessage by rememberSaveable { mutableStateOf("") }

    // Sync local loading/status to parent if callbacks provided
    LaunchedEffect(loading) { onLoadingChange(loading) }
    LaunchedEffect(statusMessage) { onStatusChange(statusMessage) }

    // Handle external submit trigger
    LaunchedEffect(submitTrigger) {
        if (submitTrigger > 0) {
            if (previewMode) {
                statusMessage = "PREVIEW_MODE_ACTIVE"
                return@LaunchedEffect
            }

            if (isLoginMode) {
                if (email.isBlank() || password.isBlank()) {
                    statusMessage = "ERROR: EMPTY_CREDENTIALS"
                    return@LaunchedEffect
                }
                loading = true
                statusMessage = "AUTHENTICATING..."
                auth?.signInWithEmailAndPassword(email, password)
                    ?.addOnSuccessListener { result ->
                        loading = false
                        statusMessage = "AUTH_SUCCESS"
                        onProfileCreated(UserProfile(result.user?.uid.orEmpty(), email, "", "", ""))
                    }
                    ?.addOnFailureListener { e ->
                        loading = false
                        statusMessage = "ERROR: ${e.localizedMessage?.uppercase()}"
                    }
            } else {
                val error = validateProfileInputs(displayName, email, password, confirmPassword)
                if (error != null) {
                    statusMessage = "ERROR: ${error.uppercase().replace(" ", "_")}"
                    return@LaunchedEffect
                }
                loading = true
                statusMessage = "INITIALIZING_PROFILE..."
                auth?.createUserWithEmailAndPassword(email, password)
                    ?.addOnSuccessListener { result ->
                        val user = result.user
                        val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                            this.displayName = displayName
                        }
                        
                        user?.updateProfile(profileUpdates)
                            ?.addOnCompleteListener { task ->
                                val uid = user.uid
                                val profile = UserProfile(uid, email, displayName, location, "")
                                db?.collection("users")?.document(uid)?.set(profile)
                                    ?.addOnSuccessListener {
                                        loading = false
                                        statusMessage = "PROFILE_READY"
                                        onProfileCreated(profile)
                                    }
                            }
                    }
                    ?.addOnFailureListener { e ->
                        loading = false
                        statusMessage = "REGISTRATION_FAILED: ${e.localizedMessage?.uppercase()}"
                    }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isLoginMode) "Enter credentials to synchronize data" else "Register for cloud synchronization",
            style = MaterialTheme.typography.bodyMedium,
            color = MyColors.HudText.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace,
            lineHeight = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!isLoginMode) {
            MyColors.HudTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = "Display Name"
            )
            Spacer(modifier = Modifier.height(12.dp))
            MyColors.HudTextField(
                value = location,
                onValueChange = { location = it },
                label = "Location"
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        MyColors.HudTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = "Email Address"
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Access Password", fontSize = 11.sp) },
            visualTransformation = PasswordVisualTransformation(),
            textStyle = TextStyle(color = MyColors.HudText, fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MyColors.HudBlue,
                unfocusedBorderColor = MyColors.HudText.copy(alpha = 0.4f),
                focusedTextColor = MyColors.HudText,
                unfocusedTextColor = MyColors.HudText,
                unfocusedLabelColor = MyColors.HudBlue,
                focusedLabelColor = MyColors.HudBlue
            ),
            shape = RoundedCornerShape(8.dp)
        )

        if (!isLoginMode) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password", fontSize = 11.sp) },
                visualTransformation = PasswordVisualTransformation(),
                textStyle = TextStyle(color = MyColors.HudText, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MyColors.HudBlue,
                    unfocusedBorderColor = MyColors.HudText.copy(alpha = 0.4f),
                    focusedTextColor = MyColors.HudText,
                    unfocusedTextColor = MyColors.HudText,
                    unfocusedLabelColor = MyColors.HudBlue,
                    focusedLabelColor = MyColors.HudBlue
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (statusMessage.isNotBlank()) {
            Text(
                text = statusMessage,
                color = if (statusMessage.contains("failed", true) || statusMessage.contains("error", true)) Color.Red else MyColors.HudBlue,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { onModeChange(!isLoginMode) }) {
            Text(
                text = if (isLoginMode) "NO_ACCOUNT?_CREATE_NEW_PROFILE" else "ALREADY_REGISTERED?_LOGIN",
                color = MyColors.HudBlue,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(4.dp))


        }
    }




private fun validateProfileInputs(
    displayName: String,
    email: String,
    password: String,
    confirmPassword: String
): String? {
    if (displayName.isBlank()) return "Enter a display name"
    if (email.isBlank()) return "Enter an email"
    if (password.isBlank()) return "Enter a password"
    if (password.length < 6) return "Password must be at least 6 characters"
    if (password != confirmPassword) return "Passwords do not match"
    return null
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Login Mode")
@Composable
fun ProfileLoginPreview() {
    JaxGamepadTheme {
        Box(modifier = Modifier.background(Color.Black).padding(16.dp)) {
            var isLogin by remember { mutableStateOf(true) }
            ProfileContent(previewMode = true, isLoginMode = isLogin, onModeChange = { isLogin = it })
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_5", name = "Register Mode")
@Composable
fun ProfileRegisterPreview() {
    JaxGamepadTheme {
        Box(modifier = Modifier.background(Color.Black).padding(16.dp)) {
            var isLogin by remember { mutableStateOf(false) }
            ProfileContent(previewMode = true, isLoginMode = isLogin, onModeChange = { isLogin = it })
        }
    }
}




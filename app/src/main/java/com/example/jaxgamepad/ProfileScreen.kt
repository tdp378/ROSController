package com.example.jaxgamepad

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onBack: () -> Unit = {},
    onProfileCreated: (UserProfile) -> Unit = {},
    previewMode: Boolean = false
) {
    val context = LocalContext.current

    val auth = if (!previewMode) FirebaseAuth.getInstance() else null
    val db = if (!previewMode) FirebaseFirestore.getInstance() else null

    var displayName by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var loading by rememberSaveable { mutableStateOf(false) }
    var statusMessage by rememberSaveable { mutableStateOf("") }

    val googleAuthManager = remember(previewMode) {
        if (!previewMode) GoogleAuthManager(context) else null
    }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_app),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ROS Controller Profile",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Create an account for cloud profile features",
                style = MaterialTheme.typography.bodyMedium,
                color = HudText
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { newValue -> displayName = newValue },
                label = { Text("Display Name", color = HudBlue, fontSize = 14.sp) },
                textStyle = LocalTextStyle.current.copy(color = HudText),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HudBlue,
                    unfocusedBorderColor = HudText.copy(alpha = 0.4f),
                    focusedTextColor = HudText,
                    unfocusedTextColor = HudText,
                    focusedLabelColor = HudBlue,
                    unfocusedLabelColor = HudBlue.copy(alpha = 0.7f),
                    cursorColor = HudBlue
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { newValue -> location = newValue },
                label = { Text("Location", color = HudBlue, fontSize = 14.sp) },
                textStyle = LocalTextStyle.current.copy(color = HudText),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HudBlue,
                    unfocusedBorderColor = HudText.copy(alpha = 0.4f),
                    focusedTextColor = HudText,
                    unfocusedTextColor = HudText,
                    focusedLabelColor = HudBlue,
                    unfocusedLabelColor = HudBlue.copy(alpha = 0.7f),
                    cursorColor = HudBlue
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { newValue -> email = newValue.trim() },
                label = { Text("Email", color = HudBlue, fontSize = 14.sp) },
                textStyle = LocalTextStyle.current.copy(color = HudText),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HudBlue,
                    unfocusedBorderColor = HudText.copy(alpha = 0.4f),
                    focusedTextColor = HudText,
                    unfocusedTextColor = HudText,
                    focusedLabelColor = HudBlue,
                    unfocusedLabelColor = HudBlue.copy(alpha = 0.7f),
                    cursorColor = HudBlue
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { newValue -> password = newValue },
                label = { Text("Password", color = HudBlue, fontSize = 14.sp) },
                textStyle = LocalTextStyle.current.copy(color = HudText),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HudBlue,
                    unfocusedBorderColor = HudText.copy(alpha = 0.4f),
                    focusedTextColor = HudText,
                    unfocusedTextColor = HudText,
                    focusedLabelColor = HudBlue,
                    unfocusedLabelColor = HudBlue.copy(alpha = 0.7f),
                    cursorColor = HudBlue
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { newValue -> confirmPassword = newValue },
                label = { Text("Confirm Password", color = HudBlue, fontSize = 14.sp) },
                textStyle = LocalTextStyle.current.copy(color = HudText),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HudBlue,
                    unfocusedBorderColor = HudText.copy(alpha = 0.4f),
                    focusedTextColor = HudText,
                    unfocusedTextColor = HudText,
                    focusedLabelColor = HudBlue,
                    unfocusedLabelColor = HudBlue.copy(alpha = 0.7f),
                    cursorColor = HudBlue
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (statusMessage.isNotBlank()) {
                Text(
                    text = statusMessage,
                    color = HudBlue
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    if (previewMode) {
                        statusMessage = "Preview mode (no Firebase)"
                        return@Button
                    }

                    val error = validateProfileInputs(
                        displayName = displayName,
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword
                    )

                    if (error != null) {
                        statusMessage = error
                        return@Button
                    }

                    loading = true
                    statusMessage = "Creating account..."

                    auth?.createUserWithEmailAndPassword(email, password)
                        ?.addOnSuccessListener { result ->
                            val uid = result.user?.uid.orEmpty()

                            val profile = UserProfile(
                                uid = uid,
                                email = email,
                                displayName = displayName,
                                location = location,
                                photoPath = ""
                            )

                            db?.collection("users")
                                ?.document(uid)
                                ?.set(profile)
                                ?.addOnSuccessListener {
                                    loading = false
                                    statusMessage = "Profile created"
                                    Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show()
                                    onProfileCreated(profile)
                                }
                                ?.addOnFailureListener { errorResult ->
                                    loading = false
                                    statusMessage = "Firestore failed: ${errorResult.message.orEmpty()}"
                                }
                        }
                        ?.addOnFailureListener { errorResult ->
                            loading = false
                            statusMessage = "Signup failed: ${errorResult.message.orEmpty()}"
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HudBlue,
                    contentColor = HudBackground
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(color = HudBackground)
                } else {
                    Text("Create Profile")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (previewMode) {
                        statusMessage = "Preview mode (no Google sign-in)"
                        return@Button
                    }

                    loading = true
                    statusMessage = "Signing in with Google..."

                    scope.launch {
                        val result = googleAuthManager?.signInWithGoogle()

                        result
                            ?.onSuccess { user ->
                                val profile = UserProfile(
                                    uid = user.uid,
                                    email = user.email.orEmpty(),
                                    displayName = user.displayName.orEmpty(),
                                    location = "",
                                    photoPath = ""
                                )

                                db?.collection("users")
                                    ?.document(user.uid)
                                    ?.set(profile)
                                    ?.addOnSuccessListener {
                                        loading = false
                                        statusMessage = "Google sign-in successful"
                                        onProfileCreated(profile)
                                    }
                                    ?.addOnFailureListener { e ->
                                        loading = false
                                        statusMessage = "Firestore failed: ${e.message}"
                                    }
                            }
                            ?.onFailure { e ->
                                loading = false
                                statusMessage = "Google sign-in failed: ${e.message}"
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Text("Continue with Google")
            }

            Spacer(modifier = Modifier.height(10.dp))



            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onBack) {
                    Text(
                        text = "Back",
                        color = HudBlue,
                        fontSize = 14.sp
                    )
                }

                TextButton(
                    onClick = {
                        statusMessage = "Sign-in coming next"
                    }
                ) {
                    Text(
                        text = "Already have an account?",
                        color = HudBlue,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen(previewMode = true)
    }
}
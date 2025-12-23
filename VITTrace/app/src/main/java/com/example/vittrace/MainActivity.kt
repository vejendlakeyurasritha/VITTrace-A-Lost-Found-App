package com.example.vittrace

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.Locale

/* ================= EMAIL RULE ================= */

fun isAllowedEmail(email: String): Boolean {
    val e = email.lowercase(Locale.ROOT)
    return e.endsWith("@vitap.ac.in") ||
            e.endsWith("@vitapstudent.ac.in") ||
            e.endsWith("@gmail.com")
}

/* ================= MAIN ACTIVITY ================= */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                        )
                    )
                ) {
                    AppNav()
                }
            }
        }
    }
}

/* ================= NAV ================= */

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val start = if (Firebase.auth.currentUser != null) "home" else "login"

    NavHost(navController = nav, startDestination = start) {
        composable("login") { LoginScreen(nav) }
        composable("signup") { SignupScreen(nav) }
        composable("home") { HomeScreen(nav) }
        composable("forgot") { ForgotScreen(nav) }
    }
}

/* ================= LOGIN ================= */

@Composable
fun LoginScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    CenterCard {
        Text("Login", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        TextField(email, { email = it }, label = { Text("Email") })
        Spacer(Modifier.height(8.dp))
        TextField(
            password,
            { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            if (!isAllowedEmail(email)) {
                Toast.makeText(
                    ctx,
                    "Use VIT-AP or Gmail account",
                    Toast.LENGTH_SHORT
                ).show()
                return@Button
            }

            Firebase.auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    nav.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(ctx, "Login failed", Toast.LENGTH_SHORT).show()
                }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Login")
        }

        TextButton(onClick = { nav.navigate("forgot") }) {
            Text("Forgot password?")
        }
        TextButton(onClick = { nav.navigate("signup") }) {
            Text("Create account")
        }
    }
}

/* ================= SIGNUP ================= */

@Composable
fun SignupScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    CenterCard {
        Text("Signup", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        TextField(email, { email = it }, label = { Text("Email") })
        Spacer(Modifier.height(8.dp))
        TextField(pass, { pass = it }, label = { Text("Password") })
        Spacer(Modifier.height(8.dp))
        TextField(confirm, { confirm = it }, label = { Text("Confirm Password") })

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            when {
                pass != confirm ->
                    Toast.makeText(ctx, "Passwords do not match", Toast.LENGTH_SHORT).show()

                !isAllowedEmail(email) ->
                    Toast.makeText(
                        ctx,
                        "Use VIT-AP or Gmail account",
                        Toast.LENGTH_SHORT
                    ).show()

                else -> {
                    Firebase.auth.createUserWithEmailAndPassword(email, pass)
                        .addOnSuccessListener {
                            Toast.makeText(ctx, "Signup successful", Toast.LENGTH_SHORT).show()
                            nav.navigate("login")
                        }
                        .addOnFailureListener {
                            Toast.makeText(ctx, it.message, Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Create Account")
        }
    }
}

/* ================= FORGOT ================= */

@Composable
fun ForgotScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    var email by remember { mutableStateOf("") }

    CenterCard {
        Text("Reset Password", fontSize = 24.sp)
        Spacer(Modifier.height(16.dp))

        TextField(email, { email = it }, label = { Text("Email") })
        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            if (!isAllowedEmail(email)) {
                Toast.makeText(
                    ctx,
                    "Use VIT-AP or Gmail account",
                    Toast.LENGTH_SHORT
                ).show()
                return@Button
            }

            Firebase.auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(ctx, "Reset link sent", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(ctx, "Email not found", Toast.LENGTH_SHORT).show()
                }
        }) {
            Text("Send Reset Link")
        }

        TextButton(onClick = { nav.popBackStack() }) {
            Text("Back")
        }
    }
}

/* ================= HOME ================= */

@Composable
fun HomeScreen(nav: NavHostController) {
    val user = Firebase.auth.currentUser

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Welcome ${user?.email}",
                color = Color.White,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                Firebase.auth.signOut()
                nav.navigate("login") {
                    popUpTo(0)
                }
            }) {
                Text("Logout")
            }
        }
    }
}

/* ================= UI HELPER ================= */

@Composable
fun CenterCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                content = content
            )
        }
    }
}

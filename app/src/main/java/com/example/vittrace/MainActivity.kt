package com.example.vittrace

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.compose.runtime.Composable
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TextFieldDefaults

fun getDisplayNameFromEmail(email: String): String {
    val namePart = email.substringBefore('.').substringBefore('@')
    return namePart.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}

fun isVitapEmail(email: String): Boolean {
    val lowerCaseEmail = email.lowercase(Locale.ROOT)
    return lowerCaseEmail.endsWith("@vitap.ac.in") ||
            lowerCaseEmail.endsWith("@vitapstudent.ac.in") ||
            lowerCaseEmail.endsWith("@vit.ac.in")
}

data class Item(
    val id: String = "",
    val name: String = "",
    val place: String = "",
    val contact: String = "",
    val dateTime: String = "",
    val imageUrl: String = "",
    val uid: String = "",
    val status: String = "Pending"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                            )
                        )
                ) {
                    VITTraceApp()
                }
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(navController: NavHostController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Reset Password", fontSize = 32.sp, color = Color.White)
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Enter your VIT-AP email to receive a password reset link.",
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TextField(value = email, onValueChange = { email = it }, label = { Text("College Email (@vitap.ac.in)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (!isVitapEmail(email)) {
                message = "Error: Only @vitap.ac.in emails are accepted."
                return@Button
            }

            Firebase.auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        message = "Success! Check your email for the reset link."
                    } else {
                        message = "Error: Could not find account or email is invalid."
                    }
                }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Send Reset Link")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = if (message.startsWith("Error")) Color.Yellow else Color.White)

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { navController.popBackStack() }) {
            Text("Back to Login", color = Color.White)
        }
    }
}

@Composable
fun ProfileScreen(navController: NavHostController) {
    val context = LocalContext.current
    val user = Firebase.auth.currentUser
    val userEmail = user?.email ?: "Guest User"

    // Get the display name (e.g., Tarun)
    val displayName = getDisplayNameFromEmail(userEmail)
    val initials = if (displayName.isNotEmpty()) displayName.first().toString() else "V"
    val purpleButtonColor = Color(0xFF6A1B9A) // Color of Change Password button

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("My Profile", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(40.dp))

        // Profile Picture Placeholder (Circle with Initial)
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF4A00E0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // --- FIX: User Name and Email Display Color Changed to Black ---
        Card(modifier = Modifier.fillMaxWidth(0.9f).padding(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(displayName, fontSize = 24.sp, color = Color.Black, fontWeight = FontWeight.Bold) // Name is now Black
                Text(userEmail, fontSize = 16.sp, color = Color.Gray) // Email is now Gray
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        // ----------------------------------------------------------------

        // --- Account Actions ---

        // 1. Change Password
        Button(
            onClick = { navController.navigate("forgotPassword") },
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = purpleButtonColor)
        ) {
            Text("Change Password", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // 2. Share App Link (Matches Change Password button color)
        Button(
            onClick = {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Check out the official VIT-AP Lost & Found app, VITTrace! [Link to Play Store will go here]")
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share VITTrace via"))
            },
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = purpleButtonColor)
        ) {
            Text("Share App Link", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(32.dp))

        // 3. Logout
        Button(
            onClick = { Firebase.auth.signOut(); navController.navigate("login") { popUpTo(0) } },
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
        ) {
            Text("Logout", fontSize = 16.sp)
        }
    }
}


@Composable
fun VITTraceApp() {
    val navController = rememberNavController()
    val auth = Firebase.auth
    val startDestination = if (auth.currentUser != null) "home" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") { LoginScreen(navController) }
        composable("signup") { SignupScreen(navController) }
        composable("home") { HomeScreen(navController) }
        composable("report") { ReportItemScreen(navController) }
        composable("find") { FindItemScreen(navController) }
        composable("details/{itemId}") { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            ItemDetailsScreen(navController, itemId)
        }
        composable("forgotPassword") { ForgotPasswordScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
    }
}

@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 40.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Log in", fontSize = 32.sp, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(40.dp))

                // Email Field
                Text("Email", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth())
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Black,
                        unfocusedIndicatorColor = Color.LightGray,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Password Field
                Text("Password", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth())
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Black,
                        unfocusedIndicatorColor = Color.LightGray,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Forgot Password Link
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { navController.navigate("forgotPassword") }) {
                        Text("Forgot password?", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Login Button (Black Fill)
                Button(
                    onClick = {
                        if (!isVitapEmail(email)) {
                            Toast.makeText(context, "Use VIT email only", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        Firebase.auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                                } else {
                                    Toast.makeText(context, "Login failed. Check credentials or verify email.", Toast.LENGTH_LONG).show()
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("Login", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Register Link and Button
                Text("Don't have an account?", fontSize = 14.sp, color = Color.Gray)
                OutlinedButton(
                    onClick = { navController.navigate("signup") },
                    modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 8.dp),
                    border = BorderStroke(1.dp, Color.Black)
                ) {
                    Text("Register", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("Test account: test@vitap.ac.in / 123456", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
fun SignupScreen(navController: NavHostController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("VITTrace Signup", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email (@vitap.ac.in)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        TextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirm Password") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            when {
                password != confirmPassword -> Toast.makeText(context, "Passwords don't match", Toast.LENGTH_SHORT).show()
                !isVitapEmail(email) -> Toast.makeText(context, "Use only VIT email", Toast.LENGTH_SHORT).show()
                else -> {
                    Firebase.auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                task.result?.user?.sendEmailVerification()
                                Toast.makeText(context, "Success! Check email to verify.", Toast.LENGTH_LONG).show()
                                navController.navigate("login")
                            } else {
                                Toast.makeText(context, "Signup failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Sign Up")
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { navController.navigate("login") }) {
            Text("Already have an account? Log In", color = Color.White)
        }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    val user = Firebase.auth.currentUser
    val displayName = user?.email?.let { getDisplayNameFromEmail(it) } ?: "Guest"

    // Use a Box to allow absolute positioning of the profile button
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // --- Profile Icon/Button in the top-left corner ---
        TextButton(
            onClick = { navController.navigate("profile") },
            modifier = Modifier
                .align(Alignment.TopStart) // Align to top-left of the Box
                .padding(top = 4.dp, start = 4.dp) // Small padding from the edge
        ) {
            // Use initials as the temporary Profile icon
            Text(
                displayName.first().toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF6A1B9A))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Main content of the HomeScreen, centered
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.vittrace_logo_placeholder),
                contentDescription = "VIT-AP Logo",
                modifier = Modifier.size(80.dp).padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Vit AP Trace App", fontSize = 28.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    Text("Find & Lost System", fontSize = 18.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(onClick = { navController.navigate("report") }, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                        Text("Report Lost/Found Item", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { navController.navigate("find") }, modifier = Modifier.fillMaxWidth().height(60.dp)) {
                        Text("Find an Item", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportItemScreen(navController: NavHostController) {
    val context = LocalContext.current
    var itemName by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf("") }
    var useCurrentTime by remember { mutableStateOf(true) } // Default to checked
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
    }
    val calendar = remember { Calendar.getInstance() }

    // Logic to manage dateTime based on checkbox state
    LaunchedEffect(useCurrentTime) {
        if (useCurrentTime) {
            dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        } else {
            dateTime = "" // Clear if not using current time
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. COLLEGE IMAGE HEADER AREA (300.dp height)
        Image(
            painter = painterResource(id = R.drawable.campus_vibe_placeholder),
            contentDescription = "VIT AP Campus Background",
            contentScale = ContentScale.Crop, // Crop to fill bounds
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp) // Height of the background image
                .align(Alignment.TopCenter)
        )

        // 2. Report Item Title (Pushed down to the required position)
        Text(
            "Report Item",
            fontSize = 32.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart) // Aligned to top-start
                .padding(top = 220.dp, start = 24.dp) // Adjusted top padding for perfect alignment
        )

        // 3. MAIN FORM CARD (Starting below the image header, filling remaining space)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter) // Aligned to bottom
                .padding(horizontal = 16.dp)
                .fillMaxHeight(0.65f), // Relative height
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), // Only top corners rounded for overlap
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .verticalScroll(rememberScrollState()), // Make content scrollable if it exceeds card height
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // --- Boxed Input Fields (OutlinedTextField) ---
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = place,
                    onValueChange = { place = it },
                    label = { Text("Place Found / Lost") },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Details") },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Date/Time Logic
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = useCurrentTime, onCheckedChange = { useCurrentTime = it })
                    Text("Use current date & time", color = Color.Black)
                }

                if (!useCurrentTime) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            DatePickerDialog(context as Context, { _, year, month, day ->
                                calendar.set(year, month, day)
                                TimePickerDialog(context, { _, hour, minute ->
                                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                                    calendar.set(Calendar.MINUTE, minute)
                                    dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(calendar.time)
                                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Date & Time")
                    }
                    if (dateTime.isNotBlank()) {
                        Text(dateTime, color = Color.Black, modifier = Modifier.padding(top = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Button(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A00E0))
                ) {
                    Text("Choose Photo")
                }
                selectedImageUri?.let {
                    AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(100.dp).padding(top = 8.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (selectedImageUri == null || itemName.isBlank() || place.isBlank() || contact.isBlank() || dateTime.isBlank()) {
                            Toast.makeText(context, "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val storageRef = Firebase.storage.reference.child("images/${UUID.randomUUID()}")
                        storageRef.putFile(selectedImageUri!!).addOnSuccessListener { taskSnapshot ->
                            taskSnapshot.storage.downloadUrl.addOnSuccessListener { url ->
                                val item = hashMapOf(
                                    "name" to itemName,
                                    "place" to place,
                                    "contact" to contact,
                                    "dateTime" to dateTime,
                                    "imageUrl" to url.toString(),
                                    "uid" to Firebase.auth.currentUser?.uid,
                                    "status" to "Pending"
                                )
                                Firebase.firestore.collection("items").add(item)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Reported! Status: Pending.", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    }
                            }
                        }.addOnFailureListener {
                            Toast.makeText(context, "Upload failed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A00E0))
                ) {
                    Text("Submit Report")
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    border = BorderStroke(1.dp, Color.Black)
                ) {
                    Text("Back to Home", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun FindItemScreen(navController: NavHostController) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<Item>() }

    LaunchedEffect(Unit) {
        Firebase.firestore.collection("items")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    items.clear()
                    for (doc in snapshot.documents) {
                        val data = doc.data ?: continue
                        val item = Item(
                            id = doc.id,
                            name = data["name"] as? String ?: "",
                            place = data["place"] as? String ?: "",
                            contact = data["contact"] as? String ?: "",
                            dateTime = data["dateTime"] as? String ?: "",
                            imageUrl = data["imageUrl"] as? String ?: "",
                            uid = data["uid"] as? String ?: "",
                            status = data["status"] as? String ?: "Pending"
                        )
                        items.add(item)
                    }
                }
            }
    }

    val filteredItems = items.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.place.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Find Item", fontSize = 32.sp, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by name or place") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredItems) { item ->
                Card(
                    modifier = Modifier
                        .clickable { navController.navigate("details/${item.id}") }
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                        AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.size(100.dp))
                        Text("Item: ${item.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Place: ${item.place}", fontSize = 12.sp)
                        Text("Date: ${item.dateTime}", fontSize = 10.sp)
                        Text("Status: ${item.status}", fontSize = 12.sp, color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun ItemDetailsScreen(navController: NavHostController, itemId: String?) {
    val context = LocalContext.current
    var item by remember { mutableStateOf<Item?>(null) }

    val statusColor = if (item?.status == "Pending") Color.Red else Color.Green

    LaunchedEffect(itemId) {
        if (itemId != null) {
            Firebase.firestore.collection("items").document(itemId).get()
                .addOnSuccessListener { doc ->
                    val data = doc.data ?: return@addOnSuccessListener
                    item = Item(
                        id = doc.id,
                        name = data["name"] as? String ?: "",
                        place = data["place"] as? String ?: "",
                        contact = data["contact"] as? String ?: "",
                        dateTime = data["dateTime"] as? String ?: "",
                        imageUrl = data["imageUrl"] as? String ?: "",
                        uid = data["uid"] as? String ?: "",
                        status = data["status"] as? String ?: "Pending"
                    )
                }
        }
    }

    item?.let { itm ->
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(model = itm.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(200.dp))
            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Item Name: ${itm.name}", color = Color.Black, fontWeight = FontWeight.Bold)
                    Text("Place Found/Lost: ${itm.place}", color = Color.Black)
                    Text("Date & Time: ${itm.dateTime}", color = Color.Black)
                    Text("Contact Details: ${itm.contact}", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
            }

            Text("Current Status: ${itm.status}", color = statusColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            if (itm.uid == Firebase.auth.currentUser?.uid) {
                Button(
                    onClick = {
                        Firebase.firestore.collection("items").document(itemId!!).delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Item Marked as Found (Report Vanished)", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)) // Green Button
                ) {
                    Text("Mark as Found (Delivered)")
                }
            }
        }
    } ?: Text("Loading...", color = Color.White)
}
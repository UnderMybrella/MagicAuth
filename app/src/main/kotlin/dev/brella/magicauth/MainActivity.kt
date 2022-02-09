package dev.brella.magicauth

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import coil.compose.rememberImagePainter
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.Constants.TAG
import com.google.firebase.messaging.FirebaseMessaging
import dev.brella.magicauth.ui.theme.MagicAuthTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Size as SizeGeom

class MainActivity : ComponentActivity() {
    val permissions = AppPermissions(this)

    @ExperimentalMaterialApi
    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val magicAuth = application as MagicAuth
        magicAuth.selectedAccount = savedInstanceState?.getInt("SELECTED_ACCOUNT", 0) ?: 0

        setContent {
            val scaffoldState = rememberScaffoldState()
            val scope = rememberCoroutineScope()

            MagicAuthTheme {
                // A surface container using the 'background' color from the theme
                val padding = 8.dp

                Scaffold(
                    scaffoldState = scaffoldState,
                    drawerContent = {
                        val listState = rememberLazyListState()
                        val iconSize = 48.dp

                        var dismissing by remember { mutableStateOf<TOTPAccount?>(null) }

                        LazyColumn(
                            modifier = Modifier.padding(padding),
                            verticalArrangement = Arrangement.spacedBy(padding),
                            state = listState
                        ) {
                            itemsIndexed(magicAuth.accounts) { index, account ->
                                val dismissState = rememberDismissState {
                                    if (it == DismissValue.DismissedToStart) dismissing = account

                                    false
                                }

                                DismissableAccount(
                                    state = dismissState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            magicAuth.selectedAccount = index
                                            magicAuth.counterValue = -1L
                                            scope.launch { scaffoldState.drawerState.close() }
                                        }
                                ) {
                                    Row {
                                        if (account.iconUrl == null) {
                                            Icon(
                                                Icons.Filled.AccountCircle,
                                                null,
                                                modifier = Modifier
                                                    .size(iconSize)
                                                    .clip(CircleShape)
//                                                .border(
//                                                    1.5.dp,
//                                                    MaterialTheme.colors.secondaryVariant,
//                                                    CircleShape
//                                                )
                                            )
                                        } else {
                                            Image(
                                                painter = rememberImagePainter(
                                                    data = account.iconUrl,
                                                    builder = {
                                                        crossfade(true)
                                                    }
                                                ),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(iconSize)
                                                    .clip(CircleShape)
//                                                .border(
//                                                    1.5.dp,
//                                                    MaterialTheme.colors.secondaryVariant,
//                                                    CircleShape
//                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // We toggle the isExpanded variable when we click on this Column
                                        Column {
                                            Text(
                                                text = account.issuer ?: "(Unknown Issuer)",
                                                color = MaterialTheme.colors.secondaryVariant,
                                                style = MaterialTheme.typography.subtitle2
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = account.accountName,
                                                style = MaterialTheme.typography.body2
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.padding(padding / 2))
                                Divider()
                            }
                        }

                        dismissing?.let { account ->
                            DeleteAccountDialog(
                                account = account,
                                onDismiss = { dismissing = null }) {
                                dismissing = null
                            }
                        }
                    },
                    floatingActionButton = {
                        SpeedDial(
                            openButton = { Icon(Icons.Rounded.Add, "Add new account") },
                            closeButton = { Icon(Icons.Rounded.Close, "Add new account") },
                            animationSpec = tween(1250)
                        ) {
                            button(content = {
                                Icon(painter = painterResource(id = R.drawable.ic_round_camera_alt_24), "Scan QR Code")
                            }) {
                                permissions.requestPermissions {
                                    camera(onGranted = {
                                        startActivity(
                                            Intent(
                                                this@MainActivity,
                                                QRScanner::class.java
                                            )
                                        )
                                    })
                                }
                            }

                            button(content = {
                                Icon(painter = painterResource(id = R.drawable.ic_round_fingerprint_24), "Scan QR Code")
                            }) {

                            }
                        }
                    },
                    floatingActionButtonPosition = FabPosition.Center,
                    bottomBar = {
                        BottomAppBar {
                            IconToggleButton(checked = false, onCheckedChange = {
                                scope.launch {
                                    scaffoldState.drawerState.apply {
                                        if (isClosed) open() else close()
                                    }
                                }
                            }) {
                                Icon(Icons.Outlined.Menu, "Menu")
                            }
                        }
                    }
                ) { contentPadding ->
                    Surface(
                        color = MaterialTheme.colors.background,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                    ) {
                        val animatedProgress by animateFloatAsState(
                            targetValue = (application as MagicAuth).timeProgress,
                            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            OTPCircle(
                                progress = animatedProgress,
                                strokeWidth = 6.dp
                            )
                        }
                    }
                }
            }
        }

        permissions.register()

        FirebaseMessaging
            .getInstance()
            .token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result

                // Log and toast
                Log.d(TAG, "Got token $token")
                Toast.makeText(baseContext, "Got token $token", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val magicAuth = application as MagicAuth
        outState.putInt("SELECTED_ACCOUNT", magicAuth.selectedAccount)
    }

    @Composable
    fun OTPCircle(
        /*@FloatRange(from = 0.0, to = 1.0)*/
        progress: Float,
        modifier: Modifier = Modifier,
        primaryColour: Color = MaterialTheme.colors.primary,
        secondaryColour: Color = MaterialTheme.colors.secondary,
        strokeWidth: Dp = ProgressIndicatorDefaults.StrokeWidth
    ) {
        val stroke = with(LocalDensity.current) {
            Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
        }

        Box(contentAlignment = Alignment.Center) {
            Text(text = (application as MagicAuth).token)

            Canvas(
                modifier
                    .size(120.dp)
                    .focusable()
                    .progressSemantics(progress)
            ) {
                // Start at 12 O'clock
                if (progress > 1.0)
                    drawOTPCircle(
                        270f,
                        360f,
                        if (progress % 2 > 1) primaryColour else secondaryColour,
                        stroke
                    )

                drawOTPCircle(
                    270f,
                    (progress - progress.toInt()) * 360f,
                    if (progress % 2 > 1.0) secondaryColour else primaryColour,
                    stroke
                )
            }
        }
    }

    private fun DrawScope.drawOTPCircle(
        startAngle: Float,
        sweep: Float,
        color: Color,
        stroke: Stroke
    ) {
        // To draw this circle we need a rect with edges that line up with the midpoint of the stroke.
        // To do this we need to remove half the stroke width from the total diameter for both sides.
        val diameterOffset = stroke.width / 2
        val arcDimen = size.width - 2 * diameterOffset
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(diameterOffset, diameterOffset),
            size = SizeGeom(arcDimen, arcDimen),
            style = stroke
        )
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}
package dev.brella.magicauth

import android.graphics.ImageFormat
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import dev.brella.magicauth.ui.theme.MagicAuthTheme
import java.net.URISyntaxException
import java.security.NoSuchAlgorithmException
import java.util.*
import javax.crypto.Mac

class QRScanner : ComponentActivity() {
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var previewView: PreviewView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        previewView = PreviewView(this)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview: Preview = Preview.Builder()
                .build()

            val cameraSelector: CameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder()
                // enable the following line if RGBA output is needed.
                // .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
//                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val reader = QRCodeMultiReader()
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                imageProxy.use { _ ->
                    when (imageProxy.format) {
                        ImageFormat.YUV_420_888, ImageFormat.YUV_422_888, ImageFormat.YUV_444_888 -> {
                            val buffer = imageProxy.planes.first().buffer
                            val data = ByteArray(buffer.remaining())
                            buffer.get(data)

                            try {
                                val result = reader.decodeMultiple(
                                    BinaryBitmap(
                                        HybridBinarizer(
                                            PlanarYUVLuminanceSource(
                                                data, imageProxy.width, imageProxy.height,
                                                0, 0,
                                                imageProxy.width, imageProxy.height,
                                                false
                                            )
                                        )
                                    ),
                                    mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))
                                )
                                    .firstOrNull { result -> result.barcodeFormat == BarcodeFormat.QR_CODE }

                                if (result != null && result.text.startsWith("otpauth://totp/")) {
                                    val uri = Uri.parse(result.text)

                                    val label = uri.path?.trimStart('/') ?: return@setAnalyzer
                                    val accountName = label.substringAfter(':')
                                    val labelIssuer =
                                        label.substringBefore(':', "").takeIf(String::isNotBlank)

                                    val secret =
                                        uri.getQueryParameter("secret") ?: return@setAnalyzer
                                    val issuer =
                                        uri.getQueryParameter("issuer")
                                    val algorithm =
                                        uri.getQueryParameter("algorithm")?.let { "Hmac$it" }
                                            ?: "HmacSHA1"

                                    val digits =
                                        uri.getQueryParameter("digits")?.toInt() ?: 6
                                    val period =
                                        uri.getQueryParameter("period")?.toLongOrNull()?.times(1000)
                                            ?: 30_000L


                                    imageAnalysis.clearAnalyzer()


                                    try {
                                        Mac.getInstance(algorithm)
                                    } catch (_: NoSuchAlgorithmException) {
                                        Toast.makeText(
                                            this,
                                            "No such algorithm $algorithm",
                                            Toast.LENGTH_SHORT
                                        )
                                            .show()

                                        return@setAnalyzer
                                    }

                                    val toast = Toast.makeText(
                                        this,
                                        "Hi $accountName @ ${issuer ?: labelIssuer}",
                                        Toast.LENGTH_SHORT
                                    )

                                    toast.show()

                                    val magicAuth = application as MagicAuth
                                    magicAuth.counterValue = -1

                                    val account = TOTPAccount(
                                        algorithm = algorithm,
                                        periodMilliseconds = period,
                                        digits = digits,
                                        accountName = accountName,
                                        issuer = issuer ?: labelIssuer,
                                        tokenName = UUID.randomUUID().toString(),
                                        iconUrl = if (issuer ?: labelIssuer == "Discord") "https://discord.com/assets/9f6f9cd156ce35e2d94c0e62e3eff462.png" else null
                                    )

                                    magicAuth.accounts.add(account)
                                    magicAuth.writeTokenFor(MagicAuth.decodeBase32(secret), account)
                                    magicAuth.writeAccounts()
                                    magicAuth.selectedAccount = magicAuth.accounts.lastIndex

                                    finish()
                                }
                            } catch (_: ReaderException) {
                            } catch (_: URISyntaxException) {
                            }
                        }
                    }
                }
            }

            val camera = cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                imageAnalysis,
                preview
            )
        }, ContextCompat.getMainExecutor(this))

        setContent {
            MagicAuthTheme {
                // A surface container using the 'background' color from the theme
                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(onClick = { finish() }) {
                            Icon(Icons.Outlined.ArrowBack, "Back")
                        }
                    },
                    floatingActionButtonPosition = FabPosition.Center
                ) { contentPadding ->
                    Surface(
                        color = MaterialTheme.colors.background,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                    ) {
                        AndroidView(factory = { previewView })
                    }
                }
            }
        }
    }
}
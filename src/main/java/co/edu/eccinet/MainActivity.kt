package co.edu.eccinet

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8,68,172)
        window.navigationBarColor = android.graphics.Color.WHITE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent { ECCInetTheme { ECCInetApp() } }
    }
}

data class CartEntry(val product: JSONObject, val quantity: Int)
private val navigationScreens = listOf("home", "categories", "cart", "profile")

private fun notificationFor(context: Context, title: String, body: String, id: Int) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(NotificationChannel("eccinet_orders", "Pedidos ECCInet", NotificationManager.IMPORTANCE_DEFAULT))
    }
    if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
    val notif = android.app.Notification.Builder(context, "eccinet_orders")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title).setContentText(body).setAutoCancel(true).build()
    manager.notify(id, notif)
}

@Composable
fun ECCInetApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("eccinet_session", Context.MODE_PRIVATE) }
    val api = remember { ApiClient() }
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var token by remember { mutableStateOf(prefs.getString("token", "") ?: "") }
    var screen by remember { mutableStateOf(if (token.isNotBlank()) "home" else "welcome") }
    var user by remember { mutableStateOf<JSONObject?>(null) }
    var campusId by remember { mutableIntStateOf(1) }
    var campuses by remember { mutableStateOf(emptyList<JSONObject>()) }
    var categories by remember { mutableStateOf(emptyList<JSONObject>()) }
    var stores by remember { mutableStateOf(emptyList<JSONObject>()) }
    var products by remember { mutableStateOf(emptyList<JSONObject>()) }
    var cart = remember { mutableStateListOf<CartEntry>() }
    var orders by remember { mutableStateOf(emptyList<JSONObject>()) }
    var ordersCount by remember { mutableIntStateOf(0) }
    var reviewsCount by remember { mutableIntStateOf(0) }
    var schedule by remember { mutableStateOf(emptyList<JSONObject>()) }
    var notifications by remember { mutableStateOf(emptyList<JSONObject>()) }
    var details by remember { mutableStateOf<JSONObject?>(null) }
    var reviews by remember { mutableStateOf(emptyList<JSONObject>()) }
    var myReviews by remember { mutableStateOf(emptyList<JSONObject>()) }
    var selectedStore by remember { mutableIntStateOf(1) }
    var selectedOrder by remember { mutableIntStateOf(0) }
    var categoryFilter by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var authMode by remember { mutableStateOf("login") }
    var authEmail by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf("") }
    var gpsStatus by remember { mutableStateOf("GPS no consultado") }

    val permissionNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        scope.launch { snack.showSnackbar(if (granted) "Notificaciones permitidas" else "Puedes activarlas en ajustes de Android") }
    }
    val permissionGps = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) gpsStatus = "Permiso concedido. Toca 'Consultar mi GPS' de nuevo."
        else gpsStatus = "Sin permiso de ubicación."
    }

    fun showError(e: Throwable) { scope.launch { snack.showSnackbar(e.message ?: "No se pudo completar la operación") } }
    fun submit(
        action: String,
        request: JSONObject = JSONObject(),
        onError: (Throwable) -> Unit = { showError(it) },
        after: (JSONObject) -> Unit = {}
    ) {
        if (busy) return
        scope.launch {
            busy = true
            try { after(api.post(action, request, token)) }
            catch (e: Exception) { onError(e) }
            finally { busy = false }
        }
    }
    fun refreshProfile() {
        // Capturamos el token de esta petición; jamás invalidar una sesión más reciente.
        val requestedToken = token
        if (requestedToken.isBlank()) return
        scope.launch {
            try {
                val d = api.get("profile", requestedToken)
                if (token != requestedToken) return@launch
                user = d.optJSONObject("user")
                user?.let { campusId = it.int("campus_id") }
                ordersCount = d.int("orders_count")
                reviewsCount = d.int("reviews_count")
            } catch (e: Exception) {
                if (token != requestedToken) return@launch
                if (e is ApiHttpException && e.statusCode == 401) {
                    // Solo una respuesta explícita 401 invalida la sesión.
                    token = ""
                    user = null
                    prefs.edit().remove("token").apply()
                    authMode = "login"
                    authError = "Tu sesión venció. Inicia sesión nuevamente."
                    screen = "auth"
                } else {
                    // Errores de red/servidor no implican que el token sea inválido.
                    showError(e)
                }
            }
        }
    }
    fun refreshCatalog() {
        scope.launch {
            try {
                val d = api.get("bootstrap")
                campuses = d.optJSONArray("campuses").asObjects()
                categories = d.optJSONArray("categories").asObjects()
                stores = d.optJSONArray("stores").asObjects()
                products = d.optJSONArray("products").asObjects()
            } catch (e: Exception) { showError(e) }
        }
    }
    fun refreshOrderData() {
        if (token.isBlank()) return
        scope.launch {
            try { orders = api.get("orders", token).optJSONArray("orders").asObjects() }
            catch (e: Exception) { showError(e) }
        }
    }
    fun refreshNotifs(announce: Boolean = false) {
        if (token.isBlank()) return
        scope.launch {
            try {
                val result = api.get("notifications",token).optJSONArray("notifications").asObjects()
                notifications = result
                val newest = result.maxOfOrNull { it.int("id") } ?: 0
                val previous = prefs.getInt("latest_notif", 0)
                if (announce && newest > previous && previous > 0) {
                    result.filter { it.int("id") > previous }.take(3).forEach {
                        notificationFor(context, it.text("title"), it.text("body"), it.int("id"))
                    }
                }
                if (newest > previous) prefs.edit().putInt("latest_notif", newest).apply()
            } catch (_: Exception) { /* Notificaciones no bloquean los pedidos */ }
        }
    }
    fun openTracking(id: Int) { selectedOrder = id; details = null; screen = "tracking" }
    fun openReviews(id: Int) { selectedStore = id; screen = "reviews" }
    fun addCart(p: JSONObject) {
        val existing = cart.indexOfFirst { it.product.int("id") == p.int("id") }
        val limit = p.int("stock")
        if (existing == -1) {
            if (limit > 0) cart.add(CartEntry(p, 1))
        } else if (cart[existing].quantity < limit && cart[existing].quantity < 20) {
            cart[existing] = cart[existing].copy(quantity = cart[existing].quantity + 1)
        }
        scope.launch { snack.showSnackbar("${p.text("name")} agregado al carrito") }
    }

    LaunchedEffect(Unit) { refreshCatalog() }
    LaunchedEffect(token) { if (token.isNotBlank()) refreshProfile() }
    LaunchedEffect(screen, token, selectedStore) {
        if (token.isBlank()) return@LaunchedEffect
        when (screen) {
            "orders" -> refreshOrderData()
            "profile" -> refreshProfile()
            "schedule" -> try { schedule = api.get("schedule",token).optJSONArray("entries").asObjects() } catch(e:Exception){ showError(e) }
            "notifications" -> refreshNotifs()
            "reviews" -> try { reviews=api.get("reviews",token, mapOf("store_id" to "$selectedStore")).optJSONArray("reviews").asObjects() } catch(e:Exception){ showError(e) }
            "my_reviews" -> try { myReviews=api.get("my_reviews",token).optJSONArray("reviews").asObjects() } catch(e:Exception){ showError(e) }
        }
    }
    LaunchedEffect(screen, selectedOrder, token) {
        if ((screen != "tracking" && screen != "chat") || token.isBlank()) return@LaunchedEffect
        while (true) {
            try {
                details = api.get("order_detail",token,mapOf("order_id" to "$selectedOrder"))
                refreshNotifs(announce = true)
            } catch(e:Exception) { showError(e) }
            delay(5000)
        }
    }

    Box(Modifier.fillMaxSize().background(EcciBackground)) {
        if (token.isBlank() || screen == "welcome" || screen == "auth") {
            when (screen) {
                "auth" -> AuthScreen(
                    mode=authMode, initialEmail=authEmail, serverError=authError, busy=busy,
                    onBack = { if (authMode == "login" || authMode == "register") screen = "welcome" else authMode = "login" },
                    onMode = { authMode = it; authError = "" },
                    onSubmit = { data ->
                        authEmail = data.optString("email",authEmail)
                        val action = when(authMode) { "register"->"register";"verify"->"verify";"forgot"->"forgot";"reset"->"reset";else->"login" }
                        authError = ""
                        submit(action, data, onError = { e ->
                            authError = e.message ?: "No se pudo completar la solicitud."
                        }) { result ->
                            when(action) {
                                "register" -> authMode="verify"
                                "verify" -> { authMode="login"; authError=""; scope.launch { snack.showSnackbar("Cuenta verificada. Inicia sesión") } }
                                "forgot" -> authMode="reset"
                                "reset" -> { authMode="login"; authError=""; scope.launch { snack.showSnackbar("Contraseña actualizada") } }
                                "login" -> {
                                    val newToken = result.text("token")
                                    if (newToken.isBlank()) throw IllegalStateException("El servidor no devolvió un token de acceso.")
                                    prefs.edit().putString("token", newToken).apply()
                                    user = result.optJSONObject("user")
                                    campusId = user?.int("campus_id") ?: 1
                                    token = newToken
                                    screen = "home"
                                    // LaunchedEffect(token) carga el perfil una sola vez
                                    // utilizando el token recién recibido.
                                    refreshCatalog()
                                }
                            }
                        }
                    },
                    onResend = {
                        authError = ""
                        submit("resend_verify", payload("email" to authEmail),
                            onError = { authError = it.message ?: "No se pudo reenviar el correo." }
                        ) { scope.launch { snack.showSnackbar("Código reenviado al correo institucional.") } }
                    }
                )
                else -> WelcomeScreen(
                    onLogin={ authMode="login"; authError=""; screen="auth" },
                    onRegister={ authMode="register"; authError=""; screen="auth" }
                )
            }
        } else {
            Scaffold(
                containerColor = EcciBackground,
                snackbarHost = { },
                bottomBar = {
                    if (screen in navigationScreens) {
                        NavigationBar(containerColor = Color.White, tonalElevation = 1.dp) {
                            listOf(
                                Triple("home", "Inicio", Icons.Default.Home),
                                Triple("categories", "Categorías", Icons.Default.Storefront),
                                Triple("cart", "Carrito", Icons.Default.ShoppingCart),
                                Triple("profile", "Perfil", Icons.Default.Person)
                            ).forEach { (id, title, icon) ->
                                NavigationBarItem(
                                    selected = screen==id, onClick={screen=id},
                                    icon={ Icon(icon,contentDescription=title) },
                                    label={Text(title, fontSize=10.sp)}, alwaysShowLabel=true,
                                    colors=NavigationBarItemDefaults.colors(selectedIconColor=EcciBlue,selectedTextColor=EcciBlue,indicatorColor=Color.White)
                                )
                            }
                        }
                    }
                }
            ) { pads ->
                Box(Modifier.fillMaxSize().padding(pads)) {
                    when(screen) {
                        "home" -> HomeScreen(
                            name = user?.text("full_name")?.split(" ")?.firstOrNull() ?: "Estudiante ECCI",
                            campuses=campuses, campusId=campusId, categories=categories,
                            products=products,search=search,
                            onSearch={ search=it }, onCampus={ id -> submit("update_campus",payload("campus_id" to id)) { campusId=id;refreshProfile() } },
                            onCategory={categoryFilter=it;screen="categories"},
                            onAdd={addCart(it)}, onAll={categoryFilter=0;screen="categories"},
                            onNotifications={screen="notifications"},onReviews={openReviews(it)}
                        )
                        "categories" -> CategoriesScreen(
                            categories,products,stores,categoryFilter,search,
                            onFilter={categoryFilter=it},onSearch={search=it},onAdd={addCart(it)},onReview={openReviews(it)}
                        )
                        "cart" -> CartScreen(
                            cart=cart,campuses=campuses,campusId=campusId,firstFree=(ordersCount==0),
                            onCampus={id->submit("update_campus",payload("campus_id" to id)) { campusId=id;refreshProfile() }},
                            onQuantity={id,diff->
                                val idx=cart.indexOfFirst{it.product.int("id")==id}
                                if(idx>=0) {
                                    val next=cart[idx].quantity+diff
                                    if(next<=0) cart.removeAt(idx)
                                    else if(next<=20 && next<=cart[idx].product.int("stock")) cart[idx]=cart[idx].copy(quantity=next)
                                }
                            },
                            onCheckout={method->
                                val items=JSONArray()
                                cart.forEach { items.put(payload("product_id" to it.product.int("id"),"quantity" to it.quantity)) }
                                submit("create_order",payload("campus_id" to campusId,"payment_method" to method,"items" to items)) { response ->
                                    cart.clear(); refreshProfile(); refreshCatalog()
                                    openTracking(response.int("order_id"))
                                }
                            },busy=busy
                        )
                        "profile" -> ProfileScreen(
                            user=user, ordersCount=ordersCount,reviewsCount=reviewsCount,campus=campuses.firstOrNull{it.int("id")==campusId},
                            onClick={ id ->
                                when(id) {
                                    "orders"->screen="orders";"campuses"->screen="campuses";"payments"->screen="payments"
                                    "reviews"->screen="my_reviews";"notifications"->screen="notifications"
                                    "support"->screen="support";"schedule"->screen="schedule"
                                    "logout"->{ val oldToken=token; scope.launch { try {api.post("logout",JSONObject(),oldToken)} catch (_:Exception) {} }; token=""; prefs.edit().remove("token").apply();user=null;cart.clear();screen="welcome" }
                                }
                            }
                        )
                        "orders" -> OrdersScreen(orders,onBack={screen="profile"},onTrack={openTracking(it)})
                        "tracking" -> TrackingScreen(
                            details=details,gpsStatus=gpsStatus,
                            onBack={screen="orders"},onChat={screen="chat"},onGps={
                                val fine=context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
                                val coarse=context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
                                if(!fine&&!coarse) permissionGps.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                else {
                                    val mgr=context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                    try {
                                        val provider = if (mgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
                                        if (Build.VERSION.SDK_INT >= 30) {
                                            mgr.getCurrentLocation(provider,null,context.mainExecutor) { loc ->
                                                gpsStatus=loc?.let { "Tu GPS: %.5f, %.5f".format(Locale.US,it.latitude,it.longitude) } ?: "No hay coordenadas disponibles; configura la ubicación del emulador."
                                            }
                                        } else {
                                            @Suppress("DEPRECATION")
                                            mgr.requestSingleUpdate(provider, { loc -> gpsStatus="Tu GPS: %.5f, %.5f".format(Locale.US,loc.latitude,loc.longitude) },null)
                                        }
                                    } catch(e:Exception) { gpsStatus="GPS: ${e.message}" }
                                }
                            }
                        )
                        "chat" -> ChatScreen(details,onBack={screen="tracking"},onSend={ msg ->
                            submit("send_message",payload("order_id" to selectedOrder,"message" to msg)) {
                                scope.launch { try { details=api.get("order_detail",token,mapOf("order_id" to "$selectedOrder")) } catch (_:Exception) {} }
                            }
                        })
                        "schedule" -> ScheduleScreen(schedule,onBack={screen="profile"})
                        "reviews" -> ReviewsScreen(
                            store=stores.firstOrNull{it.int("id")==selectedStore},reviews=reviews,
                            onBack={screen="categories"},onSave={rating,comment ->
                                submit("review_add",payload("store_id" to selectedStore,"rating" to rating,"comment" to comment)) {
                                    scope.launch { try {reviews=api.get("reviews",token,mapOf("store_id" to "$selectedStore")).optJSONArray("reviews").asObjects();refreshCatalog()} catch (_:Exception) {} }
                                }
                            }
                        )
                        "notifications" -> NotificationsScreen(notifications,onBack={screen="profile"},onMark={
                            submit("notifications_read") { refreshNotifs() }
                        },onTrack={openTracking(it)},onEnable={
                            if(Build.VERSION.SDK_INT>=33) permissionNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else scope.launch {snack.showSnackbar("Notificaciones locales habilitadas")}
                        })
                        "campuses" -> GenericOptionsScreen("Mis sedes favoritas","Selecciona tu sede principal para recibir pedidos:",campuses.map{it.int("id") to "${it.text("name")} · ${it.text("zone")}"},campusId,onBack={screen="profile"},onSelect={id->submit("update_campus",payload("campus_id" to id)){campusId=id;screen="profile";refreshProfile()}})
                        "payments" -> PaymentInfoScreen(onBack={screen="profile"})
                        "my_reviews" -> MyReviewsScreen(myReviews,onBack={screen="profile"},onSelect={openReviews(it)},onExplore={screen="categories"})
                        "support" -> SupportScreen(onBack={screen="profile"})
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snack,
            modifier = Modifier.align(Alignment.BottomCenter).padding(
                bottom = if (token.isNotBlank() && screen in navigationScreens) 80.dp else 12.dp
            )
        )
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter),color=EcciYellow)
    }
}

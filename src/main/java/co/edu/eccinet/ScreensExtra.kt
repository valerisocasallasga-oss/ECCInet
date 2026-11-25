package co.edu.eccinet

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.util.Locale

private fun orderStatus(status:String):String=when(status) {
    "confirmado"->"Confirmado"; "preparando"->"En preparación"; "en_camino"->"En camino";
    "entregado"->"Entregado"; "cancelado"->"Cancelado"; else->status
}

@Composable
fun OrdersScreen(orders:List<JSONObject>,onBack:()->Unit,onTrack:(Int)->Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Mis pedidos",onBack)
        if(orders.isEmpty()) {
            Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                Text("📦",fontSize=58.sp)
                Text("Aún no tienes pedidos",fontWeight=FontWeight.Bold)
                Text("Tu primer domicilio tendrá costo de entrega $0.",fontSize=12.sp,color=EcciMuted)
            }
        } else LazyColumn(contentPadding=PaddingValues(17.dp)) {
            items(orders) { order ->
                Card(modifier=Modifier.fillMaxWidth().padding(bottom=11.dp).clickable{onTrack(order.int("id"))},
                    colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(15.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("Pedido #${order.int("id")}",fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                            Text("● ${orderStatus(order.text("status"))}",fontSize=11.sp,color=EcciBlue)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(order.text("campus_name"),color=EcciMuted,fontSize=12.sp)
                        Text(order.text("created_at"),color=EcciMuted,fontSize=11.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text(money(order.int("total")),color=EcciDark,fontWeight=FontWeight.Black,modifier=Modifier.weight(1f))
                            Text("Ver seguimiento ›",color=EcciBlue,fontSize=12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingScreen(details:JSONObject?,gpsStatus:String,onBack:()->Unit,onChat:()->Unit,onGps:()->Unit) {
    val order=details?.optJSONObject("order")
    val items=details?.optJSONArray("items").asObjects()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Seguimiento del pedido",onBack)
        if(order==null) {
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
        } else Column(Modifier.verticalScroll(rememberScrollState()).padding(17.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
            Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Pedido #${order.int("id")}",fontWeight=FontWeight.Black,fontSize=19.sp,color=EcciText)
                    Text("📍 ${order.text("campus_name")}",color=EcciMuted,fontSize=12.sp)
                    Spacer(Modifier.height(9.dp))
                    Text("${orderStatus(order.text("status"))}",color=EcciBlue,fontWeight=FontWeight.Bold,fontSize=17.sp)
                    LinearProgressIndicator(progress={when(order.text("status")){"confirmado"->.12f;"preparando"->.35f;"en_camino"->.73f;"entregado"->1f;else->0f}},
                        modifier=Modifier.fillMaxWidth().padding(top=10.dp).height(8.dp).clip(RoundedCornerShape(8.dp)),color=EcciYellow,trackColor=EcciBackground)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("Confirmado",fontSize=10.sp,color=EcciMuted)
                        Text("En camino",fontSize=10.sp,color=EcciMuted)
                        Text("Entregado",fontSize=10.sp,color=EcciMuted)
                    }
                }
            }
            Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(15.dp)) {
                    Text("🛵 Ubicación del domiciliario",fontWeight=FontWeight.Bold,fontSize=16.sp)
                    Text("SIMULACIÓN LOCAL · actualiza cada 5 segundos",color=EcciMuted,fontSize=10.sp)
                    Spacer(Modifier.height(10.dp))
                    CourierMap(order.text("status"))
                    Spacer(Modifier.height(10.dp))
                    Text("Coordenadas demo del repartidor:",fontSize=12.sp,fontWeight=FontWeight.Medium)
                    Text("${order.text("courier_latitude")}, ${order.text("courier_longitude")}",fontSize=12.sp,color=EcciBlue)
                    Text("Destino aproximado: ${order.text("campus_latitude")}, ${order.text("campus_longitude")}",fontSize=11.sp,color=EcciMuted)
                    Spacer(Modifier.height(9.dp))
                    OutlinedButton(onClick=onGps,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.LocationOn,null,modifier=Modifier.size(17.dp));Spacer(Modifier.width(6.dp));Text("Consultar mi GPS") }
                    Text(gpsStatus,fontSize=11.sp,color=EcciMuted)
                    Text("El dibujo no es un mapa a escala. La ruta se simula desde MySQL; no hay repartidor conectado.",fontSize=10.sp,color=EcciMuted)
                }
            }
            Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(15.dp)) {
                    Text("Productos",fontWeight=FontWeight.Bold)
                    items.forEach{ p ->
                        Row(Modifier.fillMaxWidth().padding(vertical=5.dp)) {
                            Text("${p.text("emoji")} ${p.text("name")} ×${p.int("quantity")}",fontSize=12.sp,modifier=Modifier.weight(1f))
                            Text(money(p.int("unit_price")*p.int("quantity")),fontSize=12.sp)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical=9.dp))
                    PriceLine("Total",money(order.int("total")),true)
                    Text("Pago: ${order.text("payment_method")} (no procesado)",fontSize=11.sp,color=EcciMuted)
                }
            }
            Button(onClick=onChat,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Chat,null); Spacer(Modifier.width(8.dp));Text("Chat con el domiciliario (demo)")
            }
        }
    }
}

@Composable
fun CourierMap(status:String) {
    val progress=when(status){"confirmado"->.04f;"preparando"->.12f;"en_camino"->.68f;"entregado"->1f;else->.0f}
    Canvas(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFFEAF3F2))) {
        val w=size.width; val h=size.height
        // Cuadrícula ilustrativa de calles; no son datos cartográficos oficiales.
        for (i in 0..8) {
            val x=w*i/8f; drawLine(Color.White.copy(alpha=.97f),Offset(x,0f),Offset(x,h),strokeWidth=9f)
        }
        for (j in 0..5) {
            val y=h*j/5f; drawLine(Color.White.copy(alpha=.97f),Offset(0f,y),Offset(w,y),strokeWidth=9f)
        }
        val a=Offset(w*.13f,h*.74f);val b=Offset(w*.87f,h*.22f)
        drawLine(EcciBlue.copy(alpha=.42f),a,b,strokeWidth=13f)
        drawLine(EcciYellow,a,b,strokeWidth=6f)
        drawCircle(EcciBlue,18f, a)
        drawCircle(EcciYellow,20f,b)
        val marker=Offset(a.x+(b.x-a.x)*progress,a.y+(b.y-a.y)*progress)
        drawCircle(Color.White,21f,marker)
        drawCircle(EcciBlue,16f,marker)
        drawCircle(Color.White,5f,marker)
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text("● Tienda demo",color=EcciDark,fontSize=11.sp)
        Text("● Sede ECCI",color=EcciText,fontSize=11.sp)
    }
}

@Composable
fun ChatScreen(details:JSONObject?,onBack:()->Unit,onSend:(String)->Unit) {
    var input by remember { mutableStateOf("") }
    val messages=details?.optJSONArray("messages").asObjects()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Chat del pedido (demo)",onBack)
        Surface(color=Color(0xFFFFF0D0)) {
            Text("Solo modo piloto: los mensajes se guardan en MySQL. No hay operador ni repartidor real respondiendo.",
                fontSize=11.sp,color=EcciText,modifier=Modifier.fillMaxWidth().padding(10.dp))
        }
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(15.dp),verticalArrangement=Arrangement.spacedBy(9.dp)) {
            if(messages.isEmpty()) item {Text("El repartidor simulado enviará un mensaje cuando el pedido salga en camino.",color=EcciMuted,fontSize=12.sp)}
            items(messages) { msg ->
                val student=msg.text("sender")=="estudiante"
                Row(Modifier.fillMaxWidth(),horizontalArrangement=if(student)Arrangement.End else Arrangement.Start) {
                    Surface(color=if(student)EcciBlue else Color.White,shape=RoundedCornerShape(15.dp),modifier=Modifier.widthIn(max=280.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(if(student)"Tú" else "Domiciliario demo",fontSize=10.sp,color=if(student)EcciYellow else EcciBlue,fontWeight=FontWeight.Bold)
                            Text(msg.text("body"),fontSize=13.sp,color=if(student)Color.White else EcciText)
                            Text(msg.text("created_at"),fontSize=9.sp,color=if(student)Color.White.copy(alpha=.75f) else EcciMuted)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(Color.White).padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
            OutlinedTextField(value=input,onValueChange={input=it.take(500)},placeholder={Text("Escribir mensaje...")},
                modifier=Modifier.weight(1f),shape=RoundedCornerShape(18.dp),maxLines=3)
            IconButton(onClick={if(input.isNotBlank()){onSend(input.trim());input=""}},enabled=input.isNotBlank()) {Icon(Icons.Default.Send,"Enviar",tint=EcciBlue)}
        }
    }
}

@Composable
fun ScheduleScreen(entries:List<JSONObject>,onBack:()->Unit) {
    var day by remember { mutableIntStateOf(1) }
    val days=listOf("Lunes","Martes","Miércoles","Jueves","Viernes")
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Gestión de horario",onBack)
        Column(Modifier.padding(17.dp)) {
            Text("Horario académico de demostración",fontWeight=FontWeight.Bold,color=EcciDark)
            Text("No está conectado con el sistema de la universidad. Es el horario piloto cargado en MySQL.",fontSize=11.sp,color=EcciMuted)
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                days.forEachIndexed{ index,name -> FilterChip(selected=day==index+1,onClick={day=index+1},label={Text(name,fontSize=12.sp)}) }
            }
            Spacer(Modifier.height(13.dp))
            Text(days[day-1],fontWeight=FontWeight.Black,color=EcciText,fontSize=20.sp)
            Spacer(Modifier.height(9.dp))
            val filtered=entries.filter{it.int("weekday")==day}
            if(filtered.isEmpty()) Text("Sin clases asignadas.",color=EcciMuted)
            filtered.forEach { item ->
                Card(colors=CardDefaults.cardColors(containerColor=Color.White),modifier=Modifier.fillMaxWidth().padding(bottom=10.dp),shape=RoundedCornerShape(15.dp)) {
                    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                        Surface(color=Color(0xFFE8EFFF),shape=RoundedCornerShape(11.dp)) {
                            Text("🕒",modifier=Modifier.padding(11.dp),fontSize=20.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(item.text("subject"),fontWeight=FontWeight.Bold,fontSize=14.sp,color=EcciText)
                            Text("${item.text("start_time").take(5)} – ${item.text("end_time").take(5)}",fontSize=12.sp,color=EcciBlue)
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Ejemplo: 07:00 – 09:00 equivale a 7:00 a. m. – 9:00 a. m.",fontSize=11.sp,color=EcciMuted)
        }
    }
}

@Composable
fun ReviewsScreen(store:JSONObject?,reviews:List<JSONObject>,onBack:()->Unit,onSave:(Int,String)->Unit) {
    var rating by remember(store?.int("id")){mutableIntStateOf(5)}
    var text by remember(store?.int("id")){mutableStateOf("")}
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Reseñas de la tienda",onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(17.dp)) {
            Text("${store?.text("emoji")?:"🏬"} ${store?.text("name")?:"Comercio demo"}",fontSize=18.sp,fontWeight=FontWeight.Bold,color=EcciText)
            Text("Comercio ficticio para el piloto académico",fontSize=11.sp,color=EcciMuted)
            Spacer(Modifier.height(14.dp))
            Card(colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("Escribe tu reseña",fontWeight=FontWeight.Bold)
                    Row(horizontalArrangement=Arrangement.spacedBy(1.dp)) {
                        (1..5).forEach{i -> Text(if(i<=rating)"★" else "☆",fontSize=33.sp,
                            color=EcciYellow,modifier=Modifier.clickable{rating=i}.padding(2.dp))}
                    }
                    OutlinedTextField(value=text,onValueChange={text=it.take(450)},placeholder={Text("¿Qué te pareció este comercio?")},
                        modifier=Modifier.fillMaxWidth(),maxLines=4)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick={onSave(rating,text.trim());text=""},enabled=text.trim().length>=3,modifier=Modifier.fillMaxWidth()) {Text("Guardar reseña")}
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("Opiniones de estudiantes (${reviews.size})",fontWeight=FontWeight.Bold)
            if(reviews.isEmpty()) Text("Todavía no hay reseñas. ¡Puedes escribir la primera!",color=EcciMuted,fontSize=12.sp)
            reviews.forEach{ r ->
                Surface(color=Color.White,shape=RoundedCornerShape(13.dp),modifier=Modifier.fillMaxWidth().padding(top=10.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row {
                            Text(r.text("author"),fontWeight=FontWeight.Bold,fontSize=13.sp,modifier=Modifier.weight(1f))
                            Text("★".repeat(r.int("rating")),color=EcciYellow)
                        }
                        Text(r.text("comment"),fontSize=12.sp,color=EcciText)
                        Text(r.text("created_at"),fontSize=10.sp,color=EcciMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    notifications:List<JSONObject>,onBack:()->Unit,onMark:()->Unit,onTrack:(Int)->Unit,onEnable:()->Unit
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Notificaciones",onBack,right={TextButton(onClick=onMark){Text("Leídas",fontSize=11.sp)}})
        Column(Modifier.verticalScroll(rememberScrollState()).padding(15.dp)) {
            OutlinedButton(onClick=onEnable,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.Notifications,null,modifier=Modifier.size(18.dp));Spacer(Modifier.width(7.dp));Text("Habilitar avisos locales") }
            Text("Las notificaciones se muestran mientras consultas los pedidos. No hay servidor push externo.",fontSize=11.sp,color=EcciMuted)
            Spacer(Modifier.height(12.dp))
            if(notifications.isEmpty()) Text("Aún no tienes notificaciones.",color=EcciMuted)
            notifications.forEach{ n ->
                Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(13.dp),
                    modifier=Modifier.fillMaxWidth().padding(bottom=9.dp).clickable { if(n.int("order_id")>0) onTrack(n.int("order_id")) }) {
                    Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text(if(n.int("is_read")==0)"🔔" else "✅",fontSize=24.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(n.text("title"),fontWeight=FontWeight.Bold,fontSize=12.sp)
                            Text(n.text("body"),fontSize=11.sp,color=EcciMuted)
                            Text(n.text("created_at"),fontSize=10.sp,color=EcciMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GenericOptionsScreen(title:String,description:String,options:List<Pair<Int,String>>,selected:Int,onBack:()->Unit,onSelect:(Int)->Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title,onBack)
        Column(Modifier.padding(18.dp)) {
            Text(description,color=EcciMuted,fontSize=13.sp)
            Spacer(Modifier.height(14.dp))
            options.forEach{(id,label)->
                Surface(color=Color.White,shape=RoundedCornerShape(15.dp),modifier=Modifier.fillMaxWidth().padding(bottom=9.dp).clickable{onSelect(id)}) {
                    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text("📍",fontSize=23.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(label,modifier=Modifier.weight(1f),fontSize=13.sp,fontWeight=FontWeight.Medium)
                        if(id==selected) Icon(Icons.Default.CheckCircle,"Seleccionada",tint=EcciBlue)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentInfoScreen(onBack:()->Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Métodos de pago",onBack)
        Column(Modifier.padding(18.dp)) {
            Text("Puedes elegir estos métodos al confirmar tu carrito:",fontSize=13.sp,color=EcciMuted)
            Spacer(Modifier.height(14.dp))
            listOf("💵  Efectivo","📱  Nequi","📱  DaviPlata","💳  Tarjeta débito o crédito").forEach { label ->
                Surface(color=Color.White,shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().padding(bottom=10.dp)) {
                    Text(label,modifier=Modifier.padding(17.dp),fontWeight=FontWeight.Bold)
                }
            }
            Text("Esta versión no integra pasarelas de pago ni realiza transacciones. No introduzcas información financiera.",fontSize=12.sp,color=EcciMuted)
        }
    }
}

@Composable
fun MyReviewsScreen(reviews:List<JSONObject>,onBack:()->Unit,onSelect:(Int)->Unit,onExplore:()->Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Mis reseñas",onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(17.dp)) {
            Text("Tus opiniones guardadas en la base de datos",fontSize=12.sp,color=EcciMuted)
            Spacer(Modifier.height(11.dp))
            if(reviews.isEmpty()) Text("Aún no has escrito reseñas. Puedes comenzar desde las categorías.",fontSize=12.sp,color=EcciMuted)
            reviews.forEach{ review->
                Surface(color=Color.White,shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().padding(bottom=10.dp).clickable{onSelect(review.int("store_id"))}) {
                    Column(Modifier.padding(15.dp)) {
                        Text("${review.text("store_emoji")} ${review.text("store_name")}",fontWeight=FontWeight.Bold,fontSize=13.sp)
                        Text("★".repeat(review.int("rating")),color=EcciYellow,fontSize=18.sp)
                        Text(review.text("comment"),fontSize=12.sp)
                        Text("Editar o consultar ›",color=EcciBlue,fontSize=11.sp)
                    }
                }
            }
            Button(onClick=onExplore,modifier=Modifier.fillMaxWidth()){Text("Explorar comercios")}
        }
    }
}

@Composable
fun SupportScreen(onBack:()->Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Ayuda y soporte",onBack)
        Column(Modifier.padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text("❓",fontSize=66.sp)
            Spacer(Modifier.height(12.dp))
            Text("Centro de ayuda ECCInet",fontWeight=FontWeight.Bold,fontSize=20.sp,color=EcciText)
            Text("Módulo en construcción para la versión piloto",fontSize=12.sp,color=EcciMuted)
            Spacer(Modifier.height(23.dp))
            Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("¿Cómo funciona mi pedido?",fontWeight=FontWeight.Bold)
                    Text("Agrega productos del catálogo demo, selecciona tu sede y confirma un pedido simulado.",fontSize=12.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("¿Puedo hablar con soporte especializado?",fontWeight=FontWeight.Bold)
                    Text("No en esta fase. Esta sección está lista visualmente, pero todavía no se conecta con un equipo de soporte de la Universidad ECCI.",fontSize=12.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("¿Se realizan pagos reales?",fontWeight=FontWeight.Bold)
                    Text("No. Nequi, DaviPlata, tarjeta y efectivo son opciones de prueba.",fontSize=12.sp)
                }
            }
        }
    }
}

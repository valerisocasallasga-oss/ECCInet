package co.edu.eccinet

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

@Composable
fun CampusPicker(
    campuses:List<JSONObject>, selected:Int, onSelect:(Int)->Unit, dark:Boolean=false
) {
    var expanded by remember { mutableStateOf(false) }
    val campus=campuses.firstOrNull { it.int("id")==selected }
    Box {
        Surface(
            color=if(dark)Color.White.copy(alpha=.14f) else Color(0xFFEEF2FF),
            shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().clickable{expanded=true}
        ) {
            Row(Modifier.padding(horizontal=14.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("📍",fontSize=21.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Entregar en",fontSize=11.sp,color=if(dark) Color.White.copy(alpha=.65f) else EcciMuted)
                    Text(campus?.text("name")?:"Sede Principal",fontSize=14.sp,fontWeight=FontWeight.Bold,
                        color=if(dark)Color.White else EcciDark,maxLines=1,overflow=TextOverflow.Ellipsis)
                    if(!dark) Text(campus?.text("zone")?:"Teusaquillo",color=EcciMuted,fontSize=11.sp)
                }
                Icon(Icons.Default.KeyboardArrowDown,"Elegir sede",tint=if(dark)Color.White else EcciText)
            }
        }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            campuses.forEach { c -> DropdownMenuItem(
                text={Column{Text(c.text("name"),fontWeight=FontWeight.SemiBold);Text(c.text("zone"),color=EcciMuted,fontSize=11.sp)}},
                onClick={onSelect(c.int("id"));expanded=false}
            ) }
        }
    }
}

@Composable
fun ScreenHeader(title:String,onBack:(()->Unit)?=null,right:(@Composable ()->Unit)?=null) {
    Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal=12.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically) {
        if(onBack!=null) IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,"Volver",tint=EcciDark)}
        else Spacer(Modifier.width(13.dp))
        Text(title,fontSize=20.sp,fontWeight=FontWeight.Bold,color=EcciText,modifier=Modifier.weight(1f))
        if(right!=null) right()
    }
}

@Composable
fun HomeScreen(
    name:String,campuses:List<JSONObject>,campusId:Int,categories:List<JSONObject>,products:List<JSONObject>,search:String,
    onSearch:(String)->Unit,onCampus:(Int)->Unit,onCategory:(Int)->Unit,onAdd:(JSONObject)->Unit,
    onAll:()->Unit,onNotifications:()->Unit,onReviews:(Int)->Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().background(EcciBlue).padding(horizontal=22.dp,vertical=18.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Bienvenido 👋",color=Color.White.copy(alpha=.72f),fontSize=12.sp)
                    Text(name,color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Black,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                IconButton(onClick=onNotifications) { Icon(Icons.Default.Notifications,"Notificaciones",tint=EcciYellow) }
                Surface(color=EcciYellow,shape=RoundedCornerShape(14.dp),modifier=Modifier.size(46.dp)) {
                    Box(contentAlignment=Alignment.Center) {Text("EC",color=EcciDark,fontSize=18.sp,fontWeight=FontWeight.Black)}
                }
            }
            Spacer(Modifier.height(12.dp))
            CampusPicker(campuses,campusId,onCampus,dark=true)
            Spacer(Modifier.height(13.dp))
            OutlinedTextField(value=search,onValueChange=onSearch,
                placeholder={Text("Buscar comida, materiales, servicios...",fontSize=12.sp,color=EcciMuted)},
                leadingIcon={Icon(Icons.Default.Search,null,tint=Color(0xFF4C99BC))},singleLine=true,
                modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),
                colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=Color.White,focusedContainerColor=Color.White,
                    focusedBorderColor=EcciYellow,unfocusedBorderColor=Color.White))
        }
        Column(Modifier.padding(horizontal=20.dp,vertical=18.dp)) {
            if(search.isBlank()) {
                Box(Modifier.fillMaxWidth().height(115.dp).clip(RoundedCornerShape(17.dp))
                    .background(Brush.horizontalGradient(listOf(EcciYellow,Color(0xFFFF9E12))))) {
                    Column(Modifier.align(Alignment.CenterStart).padding(start=20.dp)) {
                        Text("Primer pedido\n¡GRATIS! 🎉",color=EcciText,fontSize=20.sp,lineHeight=22.sp,fontWeight=FontWeight.Black)
                        Text("Sin costo de domicilio en tu primer pedido.",fontSize=11.sp,color=EcciText.copy(alpha=.7f))
                    }
                    Text("🛵",fontSize=53.sp,modifier=Modifier.align(Alignment.BottomEnd).padding(end=12.dp,bottom=2.dp))
                }
                Spacer(Modifier.height(22.dp))
                Text("¿Qué necesitas?",fontSize=19.sp,fontWeight=FontWeight.Bold,color=EcciText)
                Spacer(Modifier.height(12.dp))
                categories.take(3).chunked(2).forEach { row ->
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth().padding(bottom=10.dp)) {
                        row.forEach { cat -> CategoryCard(cat,Modifier.weight(1f)) {onCategory(cat.int("id"))} }
                        if(row.size==1) Spacer(Modifier.weight(1f))
                    }
                }
                if (categories.size>3) {
                    Text("Más categorías",fontSize=15.sp,fontWeight=FontWeight.Bold,color=EcciText,modifier=Modifier.padding(top=9.dp,bottom=8.dp))
                    categories.drop(3).chunked(2).forEach { row ->
                        Row(horizontalArrangement=Arrangement.spacedBy(9.dp),modifier=Modifier.fillMaxWidth().padding(bottom=9.dp)) {
                            row.forEach { cat -> CategoryCard(cat,Modifier.weight(1f)) {onCategory(cat.int("id"))} }
                            if(row.size==1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top=13.dp,bottom=11.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("🔥 Los más pedidos",fontSize=17.sp,fontWeight=FontWeight.Bold,color=EcciText,modifier=Modifier.weight(1f))
                    TextButton(onClick=onAll){Text("Ver todos",fontSize=12.sp)}
                }
            } else {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text("Resultados de búsqueda",fontSize=17.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                    TextButton(onClick=onAll){Text("Ver catálogo")}
                }
            }
            val list=if(search.isNotBlank()) products.filter { it.text("name").contains(search,true)||it.text("description").contains(search,true) }
                else products.filter {it.int("featured")==1}
            if(list.isEmpty()) Text("No encontramos productos. Explora las categorías.",color=EcciMuted)
            list.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    row.forEach { p -> ProductTile(p,Modifier.weight(1f),onAdd,onReviews) }
                    if(row.size==1) Spacer(Modifier.weight(1f))
                }
            }
            Text("Productos y comercios de demostración · Piloto académico",fontSize=10.sp,color=EcciMuted)
        }
    }
}

@Composable
fun CategoryCard(cat:JSONObject,modifier:Modifier=Modifier,onClick:()->Unit) {
    val background=runCatching{Color(android.graphics.Color.parseColor(cat.text("tint")))}.getOrDefault(Color.White)
    Surface(modifier=modifier.height(89.dp).clickable(onClick=onClick),shape=RoundedCornerShape(16.dp),color=background,
        border=BorderStroke(1.dp, EcciBlue.copy(alpha=.20f))) {
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(cat.text("emoji"),fontSize=24.sp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(cat.text("name"),fontWeight=FontWeight.Bold,fontSize=12.sp,color=EcciText,maxLines=1)
                Text(cat.text("subtitle"),fontSize=10.sp,color=EcciMuted,maxLines=2,lineHeight=12.sp)
            }
        }
    }
}

@Composable
fun ProductTile(product:JSONObject,modifier:Modifier=Modifier,onAdd:(JSONObject)->Unit,onReviews:(Int)->Unit) {
    Card(modifier=modifier,shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(1.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().height(115.dp).background(Color(0xFFE9F1FB)),contentAlignment=Alignment.Center) {
                val photo=when(product.int("id")){1->R.drawable.sopa;2->R.drawable.sushi;else->0}
                if(photo>0) Image(painterResource(photo),contentDescription=product.text("name"),modifier=Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
                else Text(product.text("emoji"),fontSize=54.sp)
            }
            Column(Modifier.padding(horizontal=11.dp,vertical=10.dp)) {
                Text(product.text("name"),fontWeight=FontWeight.Bold,fontSize=13.sp,color=EcciText,maxLines=2,minLines=2,lineHeight=15.sp)
                Text(product.text("store_name"),fontSize=10.sp,color=EcciMuted,maxLines=1,overflow=TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Text(money(product.int("price")),fontWeight=FontWeight.Black,color=EcciDark,fontSize=16.sp)
                Row(verticalAlignment=Alignment.CenterVertically) {
                    TextButton(onClick={onReviews(product.int("store_id"))},contentPadding=PaddingValues(0.dp),modifier=Modifier.weight(1f)) {Text("⭐ Reseñas",fontSize=10.sp)}
                    FilledIconButton(onClick={onAdd(product)},modifier=Modifier.size(33.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=EcciBlue)) {
                        Icon(Icons.Default.Add,"Agregar",tint=Color.White,modifier=Modifier.size(19.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CategoriesScreen(
    categories:List<JSONObject>,products:List<JSONObject>,stores:List<JSONObject>,category:Int,search:String,
    onFilter:(Int)->Unit,onSearch:(String)->Unit,onAdd:(JSONObject)->Unit,onReview:(Int)->Unit
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(EcciBlue).padding(horizontal=18.dp, vertical=14.dp)) {
            Text("Categorías",color=Color.White,fontWeight=FontWeight.Bold,fontSize=24.sp)
            Text("Encuentra todo para tu día en la ECCI",color=Color.White.copy(alpha=.78f),fontSize=12.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value=search,onValueChange=onSearch,leadingIcon={Icon(Icons.Default.Search,null)},
                placeholder={Text("Buscar productos o tiendas",fontSize=12.sp)},modifier=Modifier.fillMaxWidth(),singleLine=true,
                shape=RoundedCornerShape(16.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=Color.White,focusedContainerColor=Color.White))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(selected=category==0,onClick={onFilter(0)},label={Text("Todos")})
            categories.forEach { c -> FilterChip(selected=category==c.int("id"),onClick={onFilter(c.int("id"))},label={Text("${c.text("emoji")} ${c.text("name")}")}) }
        }
        val visible=products.filter { p -> (category==0 || p.int("category_id")==category) &&
            (search.isBlank() || p.text("name").contains(search,true) || p.text("description").contains(search,true) || p.text("store_name").contains(search,true)) }
        LazyColumn(contentPadding=PaddingValues(start=16.dp,end=16.dp,bottom=16.dp)) {
            item { Text("${visible.size} productos disponibles (demostración)",fontSize=12.sp,color=EcciMuted,modifier=Modifier.padding(bottom=11.dp)) }
            items(visible.chunked(2)) { row ->
                Row(Modifier.fillMaxWidth().padding(bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    row.forEach{ product->ProductTile(product,Modifier.weight(1f),onAdd,onReview) }
                    if(row.size==1) Spacer(Modifier.weight(1f))
                }
            }
            item {
                Text("Comercios registrados en el piloto",fontWeight=FontWeight.Bold,fontSize=16.sp,modifier=Modifier.padding(top=10.dp,bottom=8.dp))
                stores.filter { category==0 || it.int("category_id")==category }.forEach{ s ->
                    Surface(color=Color.White,shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth().padding(bottom=8.dp).clickable{onReview(s.int("id"))}) {
                        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(s.text("emoji"),fontSize=24.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.text("name"),fontSize=13.sp,fontWeight=FontWeight.Bold)
                                Text("⭐ ${s.text("rating")} · ${s.text("review_count")} reseñas · demo",fontSize=11.sp,color=EcciMuted)
                            }
                            Icon(Icons.Default.ChevronRight,null,tint=EcciMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartScreen(
    cart:List<CartEntry>,campuses:List<JSONObject>,campusId:Int,firstFree:Boolean,
    onCampus:(Int)->Unit,onQuantity:(Int,Int)->Unit,onCheckout:(String)->Unit,busy:Boolean
) {
    var paymentDialog by remember { mutableStateOf(false) }
    val subtotal=cart.sumOf{it.product.int("price")*it.quantity}
    val delivery=if(cart.isEmpty() || firstFree)0 else 3000
    val total=subtotal+delivery
    Column(Modifier.fillMaxSize().background(EcciBackground)) {
        ScreenHeader("Tu carrito",onBack=null)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(17.dp)) {
            Card(colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(17.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp)) {
                    Text("📍 Entregar en",fontWeight=FontWeight.Bold,color=EcciText,fontSize=14.sp)
                    Spacer(Modifier.height(8.dp))
                    CampusPicker(campuses,campusId,onCampus)
                }
            }
            Spacer(Modifier.height(15.dp))
            if(cart.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(vertical=50.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text("🛒",fontSize=55.sp)
                    Text("Tu carrito está vacío",fontSize=17.sp,fontWeight=FontWeight.Bold)
                    Text("Agrega productos desde las categorías.",fontSize=12.sp,color=EcciMuted)
                }
            } else {
                cart.forEach{ line ->
                    Surface(Modifier.fillMaxWidth().padding(bottom=9.dp),color=Color.White,shape=RoundedCornerShape(15.dp)) {
                        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(line.product.text("emoji"),fontSize=34.sp)
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(line.product.text("name"),fontWeight=FontWeight.Bold,fontSize=13.sp)
                                Text(money(line.product.int("price")),color=EcciBlue,fontSize=14.sp,fontWeight=FontWeight.Bold)
                            }
                            IconButton(onClick={onQuantity(line.product.int("id"),-1)},modifier=Modifier.size(31.dp)) {Icon(Icons.Default.Remove,"Restar")}
                            Text("${line.quantity}",fontWeight=FontWeight.Bold)
                            IconButton(onClick={onQuantity(line.product.int("id"),1)},modifier=Modifier.size(31.dp)) {Icon(Icons.Default.Add,"Sumar")}
                        }
                    }
                }
            }
            Card(shape=RoundedCornerShape(17.dp),colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Column(Modifier.padding(17.dp)) {
                    PriceLine("Subtotal",money(subtotal),false)
                    Spacer(Modifier.height(10.dp))
                    PriceLine("Domicilio",if(cart.isNotEmpty() && firstFree) "GRATIS 🎉" else money(delivery),false)
                    HorizontalDivider(Modifier.padding(vertical=12.dp))
                    PriceLine("Total",money(total),true)
                }
            }
            Spacer(Modifier.height(9.dp))
            Text("El pago y el seguimiento son simulados. No se cobra dinero real.",fontSize=11.sp,color=EcciMuted)
        }
        Surface(color=Color.White) {
            Button(onClick={paymentDialog=true},enabled=cart.isNotEmpty()&&!busy,
                modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp).height(55.dp),shape=RoundedCornerShape(14.dp)) {
                Text("Confirmar pedido · ${money(total)}",fontWeight=FontWeight.Bold,fontSize=15.sp)
            }
        }
    }
    if(paymentDialog) {
        var selected by remember { mutableStateOf("efectivo") }
        AlertDialog(
            onDismissRequest={paymentDialog=false},title={Text("Método de pago (simulado)")},
            text={Column {
                listOf("efectivo" to "💵 Efectivo al recibir", "nequi" to "📱 Nequi", "daviplata" to "📱 DaviPlata", "tarjeta" to "💳 Tarjeta débito o crédito").forEach { (id, label) ->
                    Row(Modifier.fillMaxWidth().clickable {selected=id}.padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically) {
                        RadioButton(selected=selected==id,onClick={selected=id})
                        Text(label,fontSize=13.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Modo piloto: no se solicita número de tarjeta, claves ni se envía dinero.",fontSize=11.sp,color=EcciMuted)
            }},
            confirmButton={TextButton(onClick={paymentDialog=false;onCheckout(selected)}){Text("Crear pedido")}},
            dismissButton={TextButton(onClick={paymentDialog=false}){Text("Cancelar")}}
        )
    }
}

@Composable
fun PriceLine(label:String,value:String,emphasis:Boolean) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(label,color=if(emphasis) EcciText else EcciMuted,fontWeight=if(emphasis)FontWeight.Bold else FontWeight.Normal,
            fontSize=if(emphasis)15.sp else 13.sp,modifier=Modifier.weight(1f))
        Text(value,color=if(emphasis)EcciDark else EcciText,fontWeight=if(emphasis)FontWeight.Black else FontWeight.Medium,fontSize=if(emphasis)17.sp else 13.sp)
    }
}

@Composable
fun ProfileScreen(user:JSONObject?,ordersCount:Int,reviewsCount:Int,campus:JSONObject?,onClick:(String)->Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(EcciBlue).padding(horizontal=20.dp,vertical=25.dp)) {
            Text("Mi perfil",color=Color.White,fontWeight=FontWeight.Black,fontSize=23.sp)
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment=Alignment.CenterVertically) {
                Surface(color=EcciYellow,shape=RoundedCornerShape(14.dp),modifier=Modifier.size(65.dp)) {
                    Box(contentAlignment=Alignment.Center) { Text("EC",fontWeight=FontWeight.Black,color=EcciDark,fontSize=22.sp) }
                }
                Spacer(Modifier.width(15.dp))
                Column {
                    Text(user?.text("full_name")?:"Estudiante ECCI",color=Color.White,fontSize=16.sp,fontWeight=FontWeight.Bold,maxLines=2)
                    Text("Código: ${user?.text("student_code")?:"—"}",color=Color.White.copy(alpha=.73f),fontSize=12.sp)
                    Text(campus?.text("name")?:"Sede Principal",color=Color.White.copy(alpha=.73f),fontSize=12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=18.dp,vertical=16.dp)) {
            ProfileItem("📦","Mis pedidos","$ordersCount pedidos realizados"){onClick("orders")}
            ProfileItem("📍","Mis sedes favoritas",campus?.text("name")?:"Sede Principal"){onClick("campuses")}
            ProfileItem("💳","Métodos de pago","Efectivo · Nequi · DaviPlata · Tarjeta"){onClick("payments")}
            ProfileItem("⭐","Mis reseñas","$reviewsCount reseñas escritas"){onClick("my_reviews")}
            ProfileItem("🔔","Notificaciones","Avisos de tus pedidos"){onClick("notifications")}
            ProfileItem("🗓️","Gestión de horario","Consulta tus clases"){onClick("schedule")}
            ProfileItem("❓","Ayuda y soporte","Centro de ayuda ECCInet (piloto)"){onClick("support")}
            ProfileItem("🚪","Cerrar sesión","Salir de tu cuenta"){onClick("logout")}
        }
    }
}

@Composable
fun ProfileItem(emoji:String,title:String,subtitle:String,onClick:()->Unit) {
    Surface(color=Color.White,shape=RoundedCornerShape(16.dp),shadowElevation=2.dp,
        modifier=Modifier.fillMaxWidth().padding(bottom=12.dp).clickable(onClick=onClick)) {
        Row(Modifier.padding(horizontal=14.dp,vertical=16.dp),verticalAlignment=Alignment.CenterVertically) {
            Surface(color=EcciBackground,shape=RoundedCornerShape(11.dp),modifier=Modifier.size(43.dp)) {
                Box(contentAlignment=Alignment.Center) {Text(emoji,fontSize=21.sp)}
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title,fontSize=14.sp,fontWeight=FontWeight.Bold,color=EcciText)
                Text(subtitle,fontSize=11.sp,color=EcciMuted,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight,"Abrir",tint=EcciMuted)
        }
    }
}

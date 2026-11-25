package co.edu.eccinet

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

@Composable
fun EcciLogo(white: Boolean = true) {
    Surface(color=if(white) Color.White else EcciYellow,shape=RoundedCornerShape(24.dp),modifier=Modifier.size(104.dp)) {
        Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            Text("ECN",color=EcciDark,fontSize=30.sp,fontWeight=FontWeight.Black)
            Text("🔥",fontSize=24.sp)
        }
    }
}

@Composable
fun WelcomeScreen(onLogin:()->Unit,onRegister:()->Unit) {
    Box(Modifier.fillMaxSize().background(EcciBlue)) {
        Column(
            Modifier.fillMaxSize().padding(horizontal=36.dp, vertical=24.dp),
            horizontalAlignment=Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1.2f))
            EcciLogo()
            Spacer(Modifier.height(23.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color=EcciYellow)) {append("ECCI")}
                    withStyle(SpanStyle(color=Color.White)) {append("net")}
                },fontSize=36.sp,fontWeight=FontWeight.Black
            )
            Spacer(Modifier.height(4.dp))
            Text("Tu domicilio dentro de la ECCI",fontSize=15.sp,color=Color.White.copy(alpha=.76f))
            Spacer(Modifier.weight(1.4f))
            Button(onClick=onLogin,modifier=Modifier.fillMaxWidth().height(55.dp),shape=RoundedCornerShape(14.dp),
                colors=ButtonDefaults.buttonColors(containerColor=EcciYellow,contentColor=EcciText)) {
                Text("Iniciar sesión",fontWeight=FontWeight.Bold,fontSize=16.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick=onRegister,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(14.dp),
                border=BorderStroke(1.dp,Color.White.copy(alpha=.5f))) {
                Text("Crear cuenta",fontWeight=FontWeight.Bold,color=Color.White)
            }
            Spacer(Modifier.height(23.dp))
            Text("Universidad ECCI · Bogotá, Colombia",color=Color.White.copy(alpha=.55f),fontSize=11.sp)
        }
    }
}

@Composable
fun AuthScreen(
    mode:String, initialEmail:String, serverError:String, busy:Boolean,
    onBack:()->Unit,onMode:(String)->Unit,onSubmit:(JSONObject)->Unit,onResend:()->Unit
) {
    var fullName by remember(mode) { mutableStateOf("") }
    var studentCode by remember(mode) { mutableStateOf("") }
    var email by remember(mode,initialEmail) { mutableStateOf(initialEmail) }
    var phone by remember(mode) { mutableStateOf("") }
    var password by remember(mode) { mutableStateOf("") }
    var repeatPassword by remember(mode) { mutableStateOf("") }
    var code by remember(mode) { mutableStateOf("") }
    var campus by remember(mode) { mutableIntStateOf(1) }
    var terms by remember(mode) { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var formError by remember(mode) { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val isRegister=mode=="register"
    val title=when(mode){"register"->"Crea tu cuenta";"verify"->"Verifica tu correo";"forgot"->"Recuperar contraseña";"reset"->"Nueva contraseña";else->"Bienvenido de nuevo"}
    val note=when(mode){"register"->"Regístrate con tu correo institucional @ecci.edu.co";"verify"->"Ingresa el código de 6 dígitos enviado a tu correo";"forgot"->"Usaremos tu correo institucional registrado";"reset"->"Escribe el código recibido y tu nueva contraseña";else->"Ingresa con tu cuenta de estudiante"}
    Column(Modifier.fillMaxSize().background(EcciBackground).verticalScroll(rememberScrollState())) {
        Column(Modifier.fillMaxWidth().background(EcciBlue).padding(22.dp)) {
            IconButton(onClick=onBack) { Icon(Icons.Default.ArrowBack,"Volver",tint=Color.White) }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Surface(color=EcciYellow,shape=RoundedCornerShape(15.dp),modifier=Modifier.size(60.dp)) {
                    Box(contentAlignment=Alignment.Center) { Text("EC",color=EcciDark,fontSize=23.sp,fontWeight=FontWeight.Black) }
                }
                Spacer(Modifier.width(15.dp))
                Column {
                    Text(title,color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold)
                    Text(note,color=Color.White.copy(alpha=.8f),fontSize=12.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        Column(Modifier.padding(horizontal=22.dp, vertical=26.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
            if (isRegister) {
                AuthField(fullName,{fullName=it},"Nombre completo")
                AuthField(studentCode,{studentCode=it.filter{c->c.isDigit()}.take(6)},"Código estudiantil (6 dígitos)",KeyboardType.Number)
            }
            AuthField(email,{email=it.trim()},"Correo institucional @ecci.edu.co",KeyboardType.Email)
            if (isRegister) {
                AuthField(phone,{phone=it},"Número de teléfono",KeyboardType.Phone)
                Text("Sede de preferencia",color=EcciText,fontWeight=FontWeight.SemiBold)
                Box {
                    OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {
                        Text(listOf("Sede Principal","Sede Sur","Sede Crisanto Luque","Sede Campus 170")[campus-1])
                    }
                    DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                        listOf("Sede Principal","Sede Sur","Sede Crisanto Luque","Sede Campus 170").forEachIndexed { idx,name ->
                            DropdownMenuItem(text={Text(name)},onClick={campus=idx+1;expanded=false})
                        }
                    }
                }
            }
            if (mode=="verify" || mode=="reset") {
                AuthField(code,{code=it.filter{c->c.isDigit()}.take(6)},"Código de verificación",KeyboardType.Number)
            }
            if (mode=="login" || isRegister || mode=="reset") {
                OutlinedTextField(
                    value=password,onValueChange={password=it},label={Text(if(mode=="reset")"Contraseña nueva" else "Contraseña")},
                    singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth(),
                    shape=RoundedCornerShape(15.dp)
                )
            }
            if (isRegister || mode=="reset") {
                OutlinedTextField(value=repeatPassword,onValueChange={repeatPassword=it},label={Text("Confirma la contraseña")},
                    singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp))
            }
            if (isRegister) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=terms,onCheckedChange={terms=it})
                    Text("Acepto los ",fontSize=12.sp)
                    Text("términos y condiciones",fontSize=12.sp,color=EcciBlue,modifier=Modifier.clickable{showTerms=true})
                }
            }
            if (formError.isNotBlank()) Text(formError,color=Color(0xFFB22330),fontSize=13.sp)
            if (serverError.isNotBlank()) {
                Surface(color=Color(0xFFFFE7E8),shape=RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline,contentDescription="Error de acceso",tint=Color(0xFFAB1D2C))
                        Spacer(Modifier.width(10.dp))
                        Text(serverError,color=Color(0xFF8D1421),fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                    }
                }
            }
            Button(
                onClick={
                    formError = when {
                        !email.lowercase().endsWith("@ecci.edu.co") -> "Debes usar un correo @ecci.edu.co."
                        isRegister && (fullName.trim().length<5 || studentCode.length!=6 || phone.length<7) -> "Completa nombre, código de 6 dígitos y teléfono."
                        (isRegister || mode=="reset") && (password.length<10 || password!=repeatPassword) -> "Las contraseñas deben coincidir y tener mínimo 10 caracteres."
                        isRegister && !terms -> "Acepta los términos y condiciones."
                        (mode=="verify" || mode=="reset") && code.length!=6 -> "Escribe el código de 6 dígitos."
                        mode=="login" && password.isBlank() -> "Ingresa tu contraseña."
                        else -> ""
                    }
                    if(formError.isBlank()) {
                        val data=when(mode) {
                            "register"->payload("full_name" to fullName.trim(),"student_code" to studentCode,"email" to email.lowercase(),"phone" to phone,"password" to password,"campus_id" to campus,"accepted_terms" to true)
                            "verify"->payload("email" to email.lowercase(),"code" to code)
                            "forgot"->payload("email" to email.lowercase())
                            "reset"->payload("email" to email.lowercase(),"code" to code,"new_password" to password)
                            else->payload("email" to email.lowercase(),"password" to password)
                        }
                        onSubmit(data)
                    }
                },enabled=!busy,modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(14.dp),
                colors=ButtonDefaults.buttonColors(containerColor=EcciBlue)
            ) {
                Text(when(mode){"register"->"Crear cuenta";"verify"->"Verificar cuenta";"forgot"->"Enviar código";"reset"->"Actualizar contraseña";else->"Iniciar sesión"},fontWeight=FontWeight.Bold)
            }
            when(mode) {
                "login" -> {
                    TextButton(onClick={onMode("forgot")},modifier=Modifier.align(Alignment.CenterHorizontally)){Text("¿Olvidaste tu contraseña?")}
                    TextButton(onClick={onMode("register")},modifier=Modifier.align(Alignment.CenterHorizontally)){Text("¿No tienes cuenta? Regístrate")}
                }
                "register" -> TextButton(onClick={onMode("login")}){Text("¿Ya tienes cuenta? Inicia sesión")}
                "verify" -> TextButton(onClick=onResend){Text("Reenviar código (espera 60 segundos)")}
                else -> TextButton(onClick={onMode("login")}){Text("Volver a iniciar sesión")}
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (showTerms) AlertDialog(
        onDismissRequest={showTerms=false},title={Text("Términos del piloto ECCInet")},
        text={Text("ECCInet es un proyecto académico de demostración sin afiliación comercial ni integración oficial con sistemas universitarios. Los datos de registro se guardan en el MySQL local de XAMPP. Los productos, los domicilios, los pagos y el seguimiento del repartidor se simulan. No ingreses datos bancarios ni información médica. Usa información de prueba. La eliminación y privacidad de datos requieren medidas adicionales antes de un lanzamiento real.")},
        confirmButton={TextButton(onClick={showTerms=false}){Text("Entendido")}}
    )
}

@Composable
fun AuthField(value:String,onChange:(String)->Unit,label:String,keyboard:KeyboardType=KeyboardType.Text) {
    OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},singleLine=true,
        keyboardOptions=KeyboardOptions(keyboardType=keyboard),modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp))
}

<?php
declare(strict_types=1);
date_default_timezone_set('America/Bogota');
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
header('X-Content-Type-Options: nosniff');

$cfg = require __DIR__ . '/config.php';
require_once __DIR__ . '/mailer.php';
try {
    $pdo = new PDO(
        sprintf('mysql:host=%s;port=%s;dbname=%s;charset=utf8mb4', $cfg['db_host'], $cfg['db_port'], $cfg['db_name']),
        $cfg['db_user'], $cfg['db_pass'],
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION, PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC, PDO::ATTR_EMULATE_PREPARES => false]
    );
    $pdo->exec("SET time_zone = '-05:00'");
    $action = (string)($_GET['action'] ?? '');
    $body = json_decode((string)file_get_contents('php://input'), true);
    if (!is_array($body)) $body = [];
    $method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
    $postActions = ['register','verify','resend_verify','login','forgot','reset','logout','update_campus','create_order','send_message','review_add','notifications_read'];
    if (in_array($action, $postActions, true) && $method !== 'POST') fail('Se requiere POST.',405);
    if (!in_array($action, $postActions, true) && $method !== 'GET') fail('Se requiere GET.',405);

    switch ($action) {
      case 'health':
        output(['ok'=>true,'service'=>'ECCInet API','mode'=>'piloto_local','smtp_configured'=>smtpReady($cfg)]);
      case 'bootstrap':
        output(['ok'=>true,
          'campuses'=>all($pdo,'SELECT * FROM campuses ORDER BY id'),
          'categories'=>all($pdo,'SELECT * FROM categories ORDER BY id'),
          'stores'=>all($pdo,'SELECT s.*,c.name AS category_name, COALESCE((SELECT ROUND(AVG(r.rating),1) FROM reviews r WHERE r.store_id=s.id),0) AS rating, (SELECT COUNT(*) FROM reviews r2 WHERE r2.store_id=s.id) AS review_count FROM stores s JOIN categories c ON c.id=s.category_id ORDER BY s.id'),
          'products'=>all($pdo,'SELECT p.*,s.name AS store_name,s.pilot_only FROM products p JOIN stores s ON s.id=p.store_id WHERE p.active=1 ORDER BY p.featured DESC,p.id')
        ]);
      case 'register':
        $name=trim((string)($body['full_name']??''));
        $code=trim((string)($body['student_code']??''));
        $email=strtolower(trim((string)($body['email']??'')));
        $phone=trim((string)($body['phone']??''));
        $password=(string)($body['password']??'');
        $campus=(int)($body['campus_id']??1);
        if (mb_strlen($name)<5 || mb_strlen($name)>120) fail('Escribe tu nombre completo (mínimo 5 caracteres).');
        if (!preg_match('/^[0-9]{6}$/D',$code)) fail('El código estudiantil debe tener exactamente 6 dígitos.');
        if (!filter_var($email,FILTER_VALIDATE_EMAIL) || !str_ends_with($email,'@ecci.edu.co')) fail('Usa exclusivamente un correo @ecci.edu.co.');
        if (!preg_match('/^[0-9+ ()-]{7,20}$/D',$phone)) fail('Ingresa un teléfono válido.');
        if (strlen($password)<10 || strlen($password)>120) fail('La contraseña debe tener entre 10 y 120 caracteres.');
        if (($body['accepted_terms']??false)!==true) fail('Debes aceptar los términos y condiciones.');
        if (!one($pdo,'SELECT id FROM campuses WHERE id=?',[$campus])) fail('Selecciona una sede válida.');
        if (one($pdo,'SELECT id FROM users WHERE email=? OR student_code=?',[$email,$code])) fail('Ya existe una cuenta con ese correo o código.',409);
        requireSmtpReady($cfg);
        // Transacción: si el SMTP falla no se deja una cuenta huérfana.
        $pdo->beginTransaction();
        execSql($pdo,'INSERT INTO users(full_name,student_code,email,phone,password_hash,campus_id,accepted_terms_at) VALUES(?,?,?,?,?,?,NOW())',[$name,$code,$email,$phone,password_hash($password,PASSWORD_DEFAULT),$campus]);
        $uid=(int)$pdo->lastInsertId();
        $otp=createOtp($pdo,$uid,'verify');
        sendSmtpMail($cfg,$email,'Verifica tu cuenta ECCInet','Tu código de verificación es: '.$otp."

Válido por 15 minutos. Si no lo solicitaste, ignora este mensaje.");
        $pdo->commit();
        output(['ok'=>true,'message'=>'Cuenta creada. Revisa tu correo institucional para verificarla.']);
      case 'verify':
        $email=strtolower(trim((string)($body['email']??'')));
        $code=trim((string)($body['code']??''));
        $user=one($pdo,'SELECT id,verified_at FROM users WHERE email=?',[$email]);
        if (!$user) fail('Correo o código incorrecto.',400);
        if ($user['verified_at']) fail('La cuenta ya está verificada. Inicia sesión.');
        checkOtp($pdo,(int)$user['id'],'verify',$code);
        execSql($pdo,'UPDATE users SET verified_at=NOW() WHERE id=?',[$user['id']]);
        output(['ok'=>true,'message'=>'Correo verificado. Ya puedes iniciar sesión.']);
      case 'resend_verify':
        $email=strtolower(trim((string)($body['email']??'')));
        $user=one($pdo,'SELECT id,verified_at FROM users WHERE email=?',[$email]);
        if (!$user || $user['verified_at']) fail('No hay una cuenta pendiente de verificar.');
        requireSmtpReady($cfg);
        $pdo->beginTransaction();
        $otp=createOtp($pdo,(int)$user['id'],'verify');
        sendSmtpMail($cfg,$email,'Verifica tu cuenta ECCInet','Tu código de verificación es: '.$otp."

Válido por 15 minutos. Si no lo solicitaste, ignora este mensaje.");
        $pdo->commit();
        output(['ok'=>true,'message'=>'Reenviamos el código a tu correo institucional.']);
      case 'login':
        $email=strtolower(trim((string)($body['email']??'')));
        $password=(string)($body['password']??'');
        $user=one($pdo,'SELECT * FROM users WHERE email=?',[$email]);
        if (!$user || !password_verify($password,$user['password_hash'])) fail('Correo o contraseña incorrectos.',401);
        if (!$user['verified_at']) fail('Primero verifica tu correo institucional.',403);
        $token=bin2hex(random_bytes(32));
        execSql($pdo,'INSERT INTO auth_sessions(user_id,token_hash,expires_at) VALUES(?,?,DATE_ADD(NOW(), INTERVAL 14 DAY))',[$user['id'],hash('sha256',$token)]);
        output(['ok'=>true,'token'=>$token,'user'=>safeUser($user)]);
      case 'forgot':
        $email=strtolower(trim((string)($body['email']??'')));
        $user=one($pdo,'SELECT id FROM users WHERE email=? AND verified_at IS NOT NULL',[$email]);
        requireSmtpReady($cfg);
        if ($user) {
           $pdo->beginTransaction();
           $otp=createOtp($pdo,(int)$user['id'],'reset');
           sendSmtpMail($cfg,$email,'Restablecer contraseña ECCInet','Tu código para restablecer la contraseña es: '.$otp."

Válido por 15 minutos. Si no lo solicitaste, ignora este mensaje.");
           $pdo->commit();
        }
        output(['ok'=>true,'message'=>'Si el correo está registrado y verificado, recibirás instrucciones.']);
      case 'reset':
        $email=strtolower(trim((string)($body['email']??'')));
        $password=(string)($body['new_password']??'');
        if (strlen($password)<10 || strlen($password)>120) fail('La contraseña debe tener al menos 10 caracteres.');
        $user=one($pdo,'SELECT id FROM users WHERE email=? AND verified_at IS NOT NULL',[$email]);
        if (!$user) fail('No se pudo restablecer la cuenta.');
        checkOtp($pdo,(int)$user['id'],'reset',trim((string)($body['code']??'')));
        execSql($pdo,'UPDATE users SET password_hash=? WHERE id=?',[password_hash($password,PASSWORD_DEFAULT),$user['id']]);
        execSql($pdo,'DELETE FROM auth_sessions WHERE user_id=?',[$user['id']]);
        output(['ok'=>true,'message'=>'Contraseña actualizada. Inicia sesión.']);
      case 'profile':
        $uid=authorized($pdo);
        $user=one($pdo,'SELECT u.*,c.name AS campus_name FROM users u JOIN campuses c ON c.id=u.campus_id WHERE u.id=?',[$uid]);
        $orders=one($pdo,'SELECT COUNT(*) AS total FROM orders WHERE user_id=?',[$uid]);
        $reviews=one($pdo,'SELECT COUNT(*) AS total FROM reviews WHERE user_id=?',[$uid]);
        output(['ok'=>true,'user'=>safeUser($user),'orders_count'=>(int)$orders['total'],'reviews_count'=>(int)$reviews['total']]);
      case 'update_campus':
        $uid=authorized($pdo); $campus=(int)($body['campus_id']??0);
        if (!one($pdo,'SELECT id FROM campuses WHERE id=?',[$campus])) fail('Sede inválida.');
        execSql($pdo,'UPDATE users SET campus_id=? WHERE id=?',[$campus,$uid]);
        output(['ok'=>true,'message'=>'Sede favorita actualizada.']);
      case 'schedule':
        authorized($pdo);
        output(['ok'=>true,'template'=>true,'entries'=>all($pdo,'SELECT * FROM schedule_template ORDER BY weekday,start_time')]);
      case 'create_order':
        $uid=authorized($pdo);
        $campus=(int)($body['campus_id']??0);
        $payment=(string)($body['payment_method']??'');
        $items=$body['items']??null;
        if (!one($pdo,'SELECT id FROM campuses WHERE id=?',[$campus])) fail('Sede no disponible.');
        if (!in_array($payment,['efectivo','nequi','daviplata','tarjeta'],true)) fail('Elige un método de pago.');
        if (!is_array($items) || count($items)<1 || count($items)>30) fail('Agrega productos al carrito.');
        $quantities=[];
        foreach ($items as $item) {
          $pid=(int)($item['product_id']??0); $qty=(int)($item['quantity']??0);
          if ($pid<1 || $qty<1 || $qty>20) fail('Cantidad inválida (máximo 20 por producto).');
          $quantities[$pid]=($quantities[$pid]??0)+$qty;
          if ($quantities[$pid]>20) fail('Cantidad excesiva de un producto.');
        }
        try {
          $pdo->beginTransaction(); $subtotal=0; $lines=[];
          foreach ($quantities as $pid=>$qty) {
            $p=one($pdo,'SELECT id,price,stock FROM products WHERE id=? AND active=1 FOR UPDATE',[$pid]);
            if (!$p || (int)$p['stock']<$qty) fail('No hay existencias suficientes de uno de los productos.',409);
            $subtotal+=(int)$p['price']*$qty;
            $lines[]=['id'=>$pid,'quantity'=>$qty,'price'=>(int)$p['price']];
          }
          $prior=one($pdo,'SELECT COUNT(*) AS total FROM orders WHERE user_id=?',[$uid]);
          $delivery=(int)$prior['total']===0?0:3000;
          $total=$subtotal+$delivery;
          execSql($pdo,'INSERT INTO orders(user_id,campus_id,payment_method,subtotal,delivery_fee,total) VALUES(?,?,?,?,?,?)',[$uid,$campus,$payment,$subtotal,$delivery,$total]);
          $orderId=(int)$pdo->lastInsertId();
          foreach ($lines as $line) {
            execSql($pdo,'INSERT INTO order_items(order_id,product_id,quantity,unit_price) VALUES(?,?,?,?)',[$orderId,$line['id'],$line['quantity'],$line['price']]);
            execSql($pdo,'UPDATE products SET stock=stock-? WHERE id=?',[$line['quantity'],$line['id']]);
          }
          execSql($pdo,'INSERT INTO notifications(user_id,order_id,title,body) VALUES(?,?,?,?)',[$uid,$orderId,'Pedido #'.$orderId.' confirmado','Tu pedido piloto está registrado.']);
          $pdo->commit();
          output(['ok'=>true,'order_id'=>$orderId,'total'=>$total,'message'=>'Pedido creado. El pago es únicamente demostrativo.']);
        } catch(Throwable $e) { if ($pdo->inTransaction()) $pdo->rollBack(); throw $e; }
      case 'orders':
        $uid=authorized($pdo);
        $list=all($pdo,'SELECT o.*,c.name AS campus_name FROM orders o JOIN campuses c ON c.id=o.campus_id WHERE o.user_id=? ORDER BY o.id DESC LIMIT 100',[$uid]);
        foreach ($list as &$order) { advanceDemo($pdo,$order); }
        unset($order);
        output(['ok'=>true,'orders'=>$list]);
      case 'order_detail':
      case 'tracking':
        $uid=authorized($pdo); $oid=(int)($_GET['order_id']??0);
        $order=one($pdo,'SELECT o.*,c.name AS campus_name,c.latitude AS campus_latitude,c.longitude AS campus_longitude FROM orders o JOIN campuses c ON c.id=o.campus_id WHERE o.id=? AND o.user_id=?',[$oid,$uid]);
        if (!$order) fail('Pedido no encontrado.',404);
        advanceDemo($pdo,$order);
        $items=all($pdo,'SELECT oi.*,p.name,p.emoji FROM order_items oi JOIN products p ON p.id=oi.product_id WHERE oi.order_id=?',[$oid]);
        $chat=all($pdo,'SELECT id,sender,body,created_at FROM chat_messages WHERE order_id=? ORDER BY id',[$oid]);
        output(['ok'=>true,'order'=>$order,'items'=>$items,'messages'=>$chat,'tracking_mode'=>'SIMULACION_LOCAL']);
      case 'send_message':
        $uid=authorized($pdo); $oid=(int)($body['order_id']??0);
        $message=trim((string)($body['message']??''));
        if (mb_strlen($message)<1 || mb_strlen($message)>500) fail('El mensaje debe tener entre 1 y 500 caracteres.');
        if (!one($pdo,'SELECT id FROM orders WHERE id=? AND user_id=?',[$oid,$uid])) fail('Pedido inexistente.',404);
        execSql($pdo,'INSERT INTO chat_messages(order_id,sender,body) VALUES(?,\'estudiante\',?)',[$oid,$message]);
        output(['ok'=>true,'message'=>'Mensaje registrado en el chat de demostración.']);
      case 'notifications':
        $uid=authorized($pdo);
        output(['ok'=>true,'notifications'=>all($pdo,'SELECT * FROM notifications WHERE user_id=? ORDER BY id DESC LIMIT 80',[$uid])]);
      case 'notifications_read':
        $uid=authorized($pdo); execSql($pdo,'UPDATE notifications SET is_read=1 WHERE user_id=?',[$uid]);
        output(['ok'=>true]);
      case 'my_reviews':
        $uid=authorized($pdo);
        output(['ok'=>true,'reviews'=>all($pdo,'SELECT r.*,s.name AS store_name,s.emoji AS store_emoji FROM reviews r JOIN stores s ON s.id=r.store_id WHERE r.user_id=? ORDER BY r.id DESC',[$uid])]);
      case 'reviews':
        $store=(int)($_GET['store_id']??0);
        output(['ok'=>true,'reviews'=>all($pdo,'SELECT r.id,r.rating,r.comment,r.created_at,u.full_name AS author FROM reviews r JOIN users u ON u.id=r.user_id WHERE r.store_id=? ORDER BY r.id DESC',[$store])]);
      case 'review_add':
        $uid=authorized($pdo); $store=(int)($body['store_id']??0);
        $rating=(int)($body['rating']??0); $comment=trim((string)($body['comment']??''));
        if (!one($pdo,'SELECT id FROM stores WHERE id=?',[$store])) fail('Tienda no encontrada.');
        if ($rating<1 || $rating>5 || mb_strlen($comment)<3 || mb_strlen($comment)>450) fail('Escribe una reseña de 3 a 450 caracteres y una calificación de 1 a 5.');
        execSql($pdo,'INSERT INTO reviews(user_id,store_id,rating,comment) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE rating=VALUES(rating),comment=VALUES(comment),created_at=NOW()',[$uid,$store,$rating,$comment]);
        output(['ok'=>true,'message'=>'Reseña guardada.']);
      case 'logout':
        $token=bearerToken();
        if ($token!=='') execSql($pdo,'DELETE FROM auth_sessions WHERE token_hash=?',[hash('sha256',$token)]);
        output(['ok'=>true]);
      default: fail('Acción no encontrada.',404);
    }
} catch (PDOException $e) {
    if (isset($pdo) && $pdo->inTransaction()) $pdo->rollBack();
    error_log('[ECCInet DB] '.$e->getMessage());
    http_response_code(500);
    echo json_encode(['ok'=>false,'error'=>'Error de conexión o consulta MySQL. Verifica XAMPP e install.sql.'],JSON_UNESCAPED_UNICODE);
} catch (Throwable $e) {
    if (isset($pdo) && $pdo->inTransaction()) $pdo->rollBack();
    error_log('[ECCInet] '.$e->getMessage());
    http_response_code(500);
    echo json_encode(['ok'=>false,'error'=>'Error interno del servidor.'],JSON_UNESCAPED_UNICODE);
}

function output(array $data): never { echo json_encode($data,JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES); exit; }
function fail(string $message,int $status=400): never { http_response_code($status); output(['ok'=>false,'error'=>$message]); }
function one(PDO $db,string $sql,array $bind=[]): array|false { $q=$db->prepare($sql);$q->execute($bind);return $q->fetch(); }
function all(PDO $db,string $sql,array $bind=[]): array { $q=$db->prepare($sql);$q->execute($bind);return $q->fetchAll(); }
function execSql(PDO $db,string $sql,array $bind=[]): void { $q=$db->prepare($sql);$q->execute($bind); }
function safeUser(array $u): array { return ['id'=>(int)$u['id'],'full_name'=>$u['full_name'],'student_code'=>$u['student_code'],'email'=>$u['email'],'phone'=>$u['phone'],'campus_id'=>(int)$u['campus_id'],'campus_name'=>$u['campus_name']??null]; }
function bearerToken():string {
    // Compatible con Apache/XAMPP que no reenvía Authorization a PHP.
    $direct=(string)($_SERVER['HTTP_X_ECCINET_TOKEN']??'');
    if (preg_match('/^[a-f0-9]{64}$/D',$direct)) return $direct;
    $h=(string)($_SERVER['HTTP_AUTHORIZATION']??($_SERVER['REDIRECT_HTTP_AUTHORIZATION']??''));
    if ($h==='') {
        foreach (function_exists('getallheaders') ? getallheaders() : [] as $name=>$value) {
            if (strcasecmp($name,'Authorization')===0) { $h=(string)$value; break; }
        }
    }
    return preg_match('/^Bearer ([a-f0-9]{64})$/D',$h,$m)?$m[1]:'';
}
function authorized(PDO $db): int {
    $token=bearerToken(); if ($token==='') fail('Debes iniciar sesión.',401);
    $row=one($db,'SELECT user_id FROM auth_sessions WHERE token_hash=? AND expires_at>NOW()',[hash('sha256',$token)]);
    if (!$row) fail('Tu sesión venció. Inicia sesión otra vez.',401);
    return (int)$row['user_id'];
}
function createOtp(PDO $db,int $userId,string $purpose): string {
    // Solo un código activo por usuario y propósito. Reenvío limitado a una vez por minuto.
    $recent=one($db,'SELECT id,created_at FROM otp_codes WHERE user_id=? AND purpose=? ORDER BY id DESC LIMIT 1',[$userId,$purpose]);
    if ($recent && strtotime($recent['created_at'])>time()-60) fail('Por seguridad, espera un minuto antes de solicitar otro código.',429);
    execSql($db,'DELETE FROM otp_codes WHERE user_id=? AND purpose=?',[$userId,$purpose]);
    $otp=(string)random_int(100000,999999);
    execSql($db,'INSERT INTO otp_codes(user_id,purpose,otp_hash,expires_at) VALUES(?,?,?,DATE_ADD(NOW(),INTERVAL 15 MINUTE))',[$userId,$purpose,hash('sha256',$otp)]);
    return $otp;
}
function checkOtp(PDO $db,int $userId,string $purpose,string $code):void {
    if (!preg_match('/^[0-9]{6}$/D',$code)) fail('Código de 6 dígitos inválido.');
    $row=one($db,'SELECT * FROM otp_codes WHERE user_id=? AND purpose=? ORDER BY id DESC LIMIT 1',[$userId,$purpose]);
    if (!$row || strtotime($row['expires_at'])<time() || (int)$row['attempts']>=5) fail('Código vencido o bloqueado. Solicita otro.');
    execSql($db,'UPDATE otp_codes SET attempts=attempts+1 WHERE id=?',[$row['id']]);
    if (!hash_equals($row['otp_hash'],hash('sha256',$code))) fail('Código incorrecto.');
    execSql($db,'DELETE FROM otp_codes WHERE user_id=? AND purpose=?',[$userId,$purpose]);
}
function advanceDemo(PDO $db,array &$order):void {
    if (in_array($order['status'],['entregado','cancelado'],true)) return;
    $secs=max(0,time()-strtotime($order['created_at']));
    $new=$secs>=150?'entregado':($secs>=65?'en_camino':($secs>=25?'preparando':'confirmado'));
    $orderRank=['confirmado'=>0,'preparando'=>1,'en_camino'=>2,'entregado'=>3,'cancelado'=>4];
    if ($orderRank[$new] > $orderRank[$order['status']]) {
       execSql($db,'UPDATE orders SET status=? WHERE id=?',[$new,$order['id']]);
       $order['status']=$new;
       $text=['preparando'=>'Tu pedido ya está en preparación.','en_camino'=>'El domiciliario de DEMOSTRACIÓN va en camino.','entregado'=>'Tu pedido piloto aparece como entregado.'][$new]??'';
       execSql($db,'INSERT INTO notifications(user_id,order_id,title,body) VALUES(?,?,?,?)',[$order['user_id'],$order['id'],'Pedido #'.$order['id'].': '.str_replace('_',' ',$new),$text]);
       if ($new==='en_camino') {
         execSql($db,'INSERT INTO chat_messages(order_id,sender,body) VALUES(?,\'domiciliario\',?)',[$order['id'],'¡Hola! Soy el repartidor simulado. Tu pedido va en camino.']);
       }
    }
    $campus=one($db,'SELECT latitude,longitude FROM campuses WHERE id=?',[$order['campus_id']]);
    $store=one($db,'SELECT s.latitude,s.longitude FROM order_items oi JOIN products p ON p.id=oi.product_id JOIN stores s ON s.id=p.store_id WHERE oi.order_id=? LIMIT 1',[$order['id']]);
    if (!$campus || !$store) return;
    $fraction=$secs<65?0.0:min(1.0,($secs-65)/85.0);
    $lat=(float)$store['latitude'] + ((float)$campus['latitude']-(float)$store['latitude'])*$fraction;
    $lng=(float)$store['longitude'] + ((float)$campus['longitude']-(float)$store['longitude'])*$fraction;
    $order['courier_latitude']=round($lat,7);
    $order['courier_longitude']=round($lng,7);
    execSql($db,'UPDATE orders SET courier_latitude=?,courier_longitude=? WHERE id=?',[$order['courier_latitude'],$order['courier_longitude'],$order['id']]);
}

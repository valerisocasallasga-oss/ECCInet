<?php
// Solo consola. Uso: C:\xampp\php\php.exe test_smtp.php correo@ejemplo.com
declare(strict_types=1);
if (PHP_SAPI !== 'cli') { http_response_code(403); exit; }
require __DIR__.'/mailer.php';
function fail(string $message, int $status=400): never { fwrite(STDERR,$message."
"); exit(1); }
$cfg=require __DIR__.'/config.php';
$recipient=$argv[1]??'';
if (!filter_var($recipient,FILTER_VALIDATE_EMAIL)) fail('Proporciona un correo de destino válido.');
if (!smtpReady($cfg)) fail('SMTP incompleto. Ejecuta composer install y edita config.php.');
sendSmtpMail($cfg,$recipient,'Prueba de correo ECCInet','Si recibiste este mensaje, el envío SMTP desde XAMPP funciona correctamente.');
echo "Correo de prueba enviado. Revisa la bandeja de entrada y spam.
";

<?php
declare(strict_types=1);
/** Configuración y envío SMTP. Nunca se muestra un OTP en Android ni en JSON. */
function smtpReady(array $cfg): bool {
    foreach (['smtp_host','smtp_user','smtp_password','smtp_from_email'] as $field) {
        if (trim((string)($cfg[$field] ?? ''))==='') return false;
    }
    return ($cfg['mail_enabled']??false)===true
        && filter_var($cfg['smtp_from_email'],FILTER_VALIDATE_EMAIL)!==false
        && in_array((string)($cfg['smtp_security']??'starttls'),['starttls','smtps'],true)
        && is_file(__DIR__.'/vendor/autoload.php');
}
function requireSmtpReady(array $cfg): void {
    if (!smtpReady($cfg)) fail('El correo SMTP no está configurado. Revisa Composer y config.php.',503);
}
function sendSmtpMail(array $cfg, string $recipient, string $subject, string $message): void {
    requireSmtpReady($cfg);
    require_once __DIR__.'/vendor/autoload.php';
    try {
        $mail = new PHPMailer\PHPMailer\PHPMailer(true);
        $mail->isSMTP();
        $mail->Host=(string)$cfg['smtp_host'];
        $mail->SMTPAuth=true;
        $mail->Username=(string)$cfg['smtp_user'];
        $mail->Password=(string)$cfg['smtp_password'];
        $mail->Port=(int)($cfg['smtp_port']??587);
        $mail->SMTPSecure=($cfg['smtp_security']??'starttls')==='smtps'
            ? PHPMailer\PHPMailer\PHPMailer::ENCRYPTION_SMTPS
            : PHPMailer\PHPMailer\PHPMailer::ENCRYPTION_STARTTLS;
        $mail->Timeout=20;
        $mail->CharSet='UTF-8';
        $mail->setFrom((string)$cfg['smtp_from_email'],(string)($cfg['smtp_from_name']??'ECCInet'));
        $mail->addAddress($recipient);
        $mail->Subject=$subject;
        $mail->Body=$message;
        $mail->send();
    } catch (Throwable $e) {
        error_log('[ECCInet SMTP] '.$e->getMessage());
        fail('No fue posible enviar el correo. Revisa SMTP y los registros de Apache.',503);
    }
}

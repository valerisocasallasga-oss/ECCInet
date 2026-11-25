<?php
// Configuración LOCAL: cambia estos valores en tu XAMPP.
// No publiques este archivo ni uses modo desarrollo fuera del computador de pruebas.
return [
    'db_host' => '127.0.0.1',
    'db_port' => '3306',
    'db_name' => 'eccinet',
    'db_user' => 'root',
    'db_pass' => '',
    'mail_enabled' => false, // Activar solo después de instalar Composer y colocar los datos SMTP.
    'smtp_host' => '',
    'smtp_port' => 587,
    'smtp_security' => 'starttls', // starttls:587 o smtps:465
    'smtp_user' => '',
    'smtp_password' => '',
    'smtp_from_email' => '',
    'smtp_from_name' => 'ECCInet Piloto',
];

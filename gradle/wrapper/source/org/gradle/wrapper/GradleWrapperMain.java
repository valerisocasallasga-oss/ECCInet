package org.gradle.wrapper;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/**
 * Lanzador mínimo independiente para el piloto. NO es el jar oficial de Gradle.
 * Descarga la distribución oficial de Gradle 8.9, comprueba el hash SHA-256
 * publicado en gradle.org y ejecuta su distribución local.
 * Para un proyecto de producción se recomienda regenerar el wrapper oficial.
 */
public final class GradleWrapperMain {
    private static final String NAME = "gradle-8.9";
    private static final String URL = "https://services.gradle.org/distributions/gradle-8.9-bin.zip";
    private static final String SHA256 = "d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab";
    public static void main(String[] args) throws Exception {
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        String home = System.getenv("GRADLE_USER_HOME");
        if (home == null || home.isBlank()) home = System.getProperty("user.home") + "/.gradle";
        Path base = Paths.get(home,"wrapper","dists","eccinet-verified");
        Path command = base.resolve(NAME).resolve("bin").resolve(windows ? "gradle.bat" : "gradle");
        if (!Files.isRegularFile(command)) {
            Files.createDirectories(base);
            Path zip = Files.createTempFile("eccinet-gradle", ".zip");
            try {
                System.out.println("Descargando Gradle 8.9 desde services.gradle.org (primera ejecución)...");
                HttpURLConnection con = (HttpURLConnection)new URL(URL).openConnection();
                con.setInstanceFollowRedirects(true);
                con.setConnectTimeout(20000);
                con.setReadTimeout(120000);
                // Algunos endpoints redirigen a GitHub; URL.openConnection sigue redirects HTTPS.
                try (InputStream in = con.getInputStream()) { Files.copy(in, zip, StandardCopyOption.REPLACE_EXISTING); }
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (InputStream in=Files.newInputStream(zip)) {
                    byte[] buf=new byte[65536];int count;
                    while((count=in.read(buf))!=-1) digest.update(buf,0,count);
                }
                byte[] sha=digest.digest();StringBuilder hex=new StringBuilder();
                for(byte b:sha) hex.append(String.format("%02x",b));
                if(!hex.toString().equals(SHA256)) throw new SecurityException("SHA-256 de Gradle no coincide. Descarga rechazada.");
                try(ZipInputStream z = new ZipInputStream(Files.newInputStream(zip))) {
                    ZipEntry item;
                    while((item=z.getNextEntry())!=null) {
                        Path out=base.resolve(item.getName()).normalize();
                        if(!out.startsWith(base)) throw new SecurityException("ZIP inválido.");
                        if(item.isDirectory()) Files.createDirectories(out);
                        else {Files.createDirectories(out.getParent());Files.copy(z,out,StandardCopyOption.REPLACE_EXISTING);}
                        z.closeEntry();
                    }
                }
                if(!windows) command.toFile().setExecutable(true,false);
            } finally {Files.deleteIfExists(zip);}
        }
        List<String> processArgs = new ArrayList<>();
        if(windows) {processArgs.add("cmd");processArgs.add("/c");}
        processArgs.add(command.toAbsolutePath().toString());
        processArgs.addAll(Arrays.asList(args));
        ProcessBuilder process = new ProcessBuilder(processArgs);
        process.directory(new File(System.getProperty("user.dir")));
        process.inheritIO();
        System.exit(process.start().waitFor());
    }
}

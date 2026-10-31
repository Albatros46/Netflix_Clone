package com.NetflixClone.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // application.properties (veya yml) dosyasından app.cors.allowedOrigins değerini okur
    // Eğer tanımlı değilse varsayılan olarak http://localhost:5175 kabul edilir (React/Vite dev server)
    @Value("${app.cors.allowedOrigins:http://localhost:5175}")
    private String[] allowedOrigins;

    // CORS kurallarını tanımladığımız metot
    // WebMvcConfigurer arayüzünden gelir ve Spring boot tarafından otomatik çağrılır
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        // "/api/**" yolundaki tüm endpoint'ler için CORS ayarlarını uygula
        registry.addMapping("/api/**")

                // Sadece belirtilen origin(ler)den gelen istekleri kabul et
                // Örneğin: http://localhost:5175 veya production'da gerçek frontend URL'leri
                .allowedOrigins(allowedOrigins)

                // Bu HTTP metodlarına izin ver
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")

                // Tüm header'lara izin ver (client tarafında özel header gönderebilmek için)
                .allowedHeaders("*")

                // Response'ta client'a görünür kılınacak header'lar
                // Örneğin: Location (redirect için), Content-Disposition (dosya indirme için)
                .exposedHeaders("Location", "Content-Disposition")

                // Cookie veya Authorization header gibi kimlik bilgilerinin gönderilmesine izin ver
                // false yapmamızın sebebi JWT token'ı genellikle Authorization: Bearer header'ında taşıyoruz
                // Bu durumda credentials gerekmez
                .allowCredentials(false)

                // Preflight (OPTIONS) isteklerinin cache'te ne kadar süre tutulacağı (saniye)
                // 3600 saniye = 1 saat, tarayıcı bu süre içinde tekrar preflight yapmaz
                .maxAge(3600);
    }
}
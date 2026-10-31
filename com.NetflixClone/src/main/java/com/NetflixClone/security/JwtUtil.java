package com.NetflixClone.security;

// JWT (JSON Web Token) işlemleri için yardımcı sınıf
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// Spring tarafından yönetilen bir bileşen (bean) olarak tanımlıyoruz
@Component
public class JwtUtil {

    // Token'ın geçerlilik süresi: 30 gün (milisaniye cinsinden hesaplandı)
    private static final long JWT_TOKEN_VALIDITY = 30L * 24 * 60 * 60 * 1000;

    // application.properties dosyasından jwt.secret değerini alacak.
    // Eğer tanımlı değilse varsayılan bir secret key kullanılacak.
    @Value("${jwt.secret:defaultSecretKeyForNetflixClone}")
    private String secret;

    // Secret key'i HMAC-SHA algoritması için uygun formata dönüştürür ve döndürür
    private SecretKey getSignKey() {
        // Ayni secret'tan her seferinde ayni anahtar uretilmeli; aksi halde uretilen token'lar dogrulanamaz.
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Token'dan kullanıcı adını (subject) çıkarır
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // Token'dan belirli bir claim'i (bilgiyi) generic olarak alır
    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // Token'dan "role" claim'ini (kullanıcı rolünü) String olarak alır
    public String getRoleFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("role", String.class));
    }

    // Token'ın sona erme tarihini alır
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // Token'ı parse eder ve içindeki tüm claim'leri (payload) döndürür
    // Secret key ile doğrulamayı da yapar
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())  // Secret key ile doğrulama
                .build()
                .parseSignedClaims(token) // Token'ı imzalı olarak parse et
                .getPayload();            // Payload (claim'ler) kısmını döndür
    }

    // Token'ın süresi dolmuş mu diye kontrol eder
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date()); // Şu anki tarihten önceyse süresi dolmuş demektir
    }

    // Yeni bir JWT token'ı oluşturur. Kullanıcı adı ve rol bilgisini alır
    public String generateToken(String username, String role) {
        // Ekstra claim'ler için bir Map oluşturuyoruz (örneğin rol)
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role); // Rolü claim olarak ekliyoruz
        // İsterseniz başka bilgiler de eklenebilir, örneğin email
        // claims.put("email", email);

        return doGenerateToken(claims, username);
    }

    // Gerçek token oluşturma işlemini yapan private metot
    private String doGenerateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)                                      // Ekstra claim'leri ekle
                .subject(subject)                                    // Kullanıcı adı (genelde username)
                .issuedAt(new Date(System.currentTimeMillis()))      // Token'ın oluşturulma zamanı
                .expiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY)) // Son geçerlilik tarihi
                .signWith(getSignKey())                              // Secret key ile imza at
                .compact();                                          // Token'ı string haline getir ve döndür
    }

    // Token'ın geçerli olup olmadığını kontrol eder
    // Hem imza doğrulaması hem de süre kontrolü yapar
    public Boolean validateToken(String token) {
        try {
            // Token parse edilebiliyorsa imza doğru demektir
            getAllClaimsFromToken(token);
            // Süresi dolmamış mı kontrol et
            return !isTokenExpired(token);
        } catch (Exception e) {
            // Herhangi bir hata olursa (imza hatalı, format yanlış vs.) token geçersiz
            return false;
        }
    }
}
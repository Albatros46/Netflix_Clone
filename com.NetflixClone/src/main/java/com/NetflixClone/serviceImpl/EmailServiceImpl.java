package com.NetflixClone.serviceImpl;


import com.NetflixClone.exception.EmailSendingException;
import com.NetflixClone.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Bu sınıfı Spring'in bir servis bean'i olarak işaretliyoruz
@Service
public class EmailServiceImpl implements EmailService {

    // Loglama için Logger nesnesi (hata ve bilgi mesajlarını konsola yazmak için)
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    // Spring Mail ile e-posta göndermek için kullanılan sender bean'i
    @Autowired
    private JavaMailSender mailSender;

    // application.properties'den frontend URL'sini alıyoruz
    // Varsayılan: http://localhost:5175 (React/Vite geliştirme sunucusu)
    @Value("${app.frontend.url:http://localhost:5175}")
    private String frontendUrl;

    // Gönderen e-posta adresi (spring.mail.username'den geliyor, genellikle Gmail veya başka provider)
    @Value("${app.mail.from:no-reply@netflix-clone.local}")
    private String fromEmail;

    // Kullanıcı kayıt olduktan sonra e-posta doğrulama maili gönderir
    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        try {
            // Basit metin tabanlı bir e-posta mesajı oluşturuyoruz
            SimpleMailMessage message = new SimpleMailMessage();

            // Gönderen adresi (genellikle no-reply@netflixclone.com veya Gmail adresi)
            message.setFrom(fromEmail);

            // Alıcı adresi
            message.setTo(toEmail);

            // E-posta konusu
            message.setSubject("Netflix Clone - E-Mail-Adresse bestätigen");

            String verificationLink = frontendUrl + "/verify-email?token=" + token;

            // E-posta içeriği (metin tabanlı, arayüz dili Almanca)
            String emailBody = "Willkommen bei Netflix Clone!\n\n"
                    + "Vielen Dank für Ihre Registrierung. Bitte bestätigen Sie Ihre E-Mail-Adresse über diesen Link:\n\n"
                    + verificationLink + "\n\n"
                    + "Der Link ist 24 Stunden gültig.\n\n"
                    + "Falls Sie kein Konto erstellt haben, ignorieren Sie diese E-Mail einfach.\n\n"
                    + "Viele Grüße\n"
                    + "Ihr Netflix-Clone-Team";

            message.setText(emailBody);

            // E-postayı gönder
            mailSender.send(message);

            // Başarılı olursa logla
            logger.info("Verification email sent to {}", toEmail);

        } catch (Exception ex) {
            // Hata olursa logla ve özel exception fırlat
            logger.error("Failed to send verification email to {} : {}", toEmail, ex.getMessage());
            throw new EmailSendingException("Failed to send verification email", ex);
        }
    }

    // Şifre sıfırlama isteği geldiğinde gönderilen e-posta
    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Netflix Clone - Passwort zurücksetzen");

            // Şifre sıfırlama linki doğru şekilde oluşturuluyor (= işareti var)
            String resetLink = frontendUrl + "/reset-password?token=" + token;

            // E-posta içeriği
            String emailBody = "Hallo,\n\n"
                    + "wir haben eine Anfrage zum Zurücksetzen Ihres Passworts erhalten. Über diesen Link vergeben Sie ein neues Passwort:\n\n"
                    + resetLink + "\n\n"
                    + "Der Link ist 1 Stunde gültig.\n\n"
                    + "Falls Sie das nicht angefordert haben, ignorieren Sie diese E-Mail einfach.\n\n"
                    + "Viele Grüße\n"
                    + "Ihr Netflix-Clone-Team";

            message.setText(emailBody);

            // E-postayı gönder
            mailSender.send(message);

            // Başarılı log
            logger.info("Password reset email sent to {}", toEmail);

        } catch (Exception ex) {
            // Hata logla ve genel RuntimeException fırlat
            logger.error("Failed to send password reset email to {} : {}", toEmail, ex.getMessage());
            throw new EmailSendingException("Failed to send password reset email", ex);
        }
    }
}
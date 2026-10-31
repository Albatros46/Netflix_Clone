package com.NetflixClone.serviceImpl;

import com.NetflixClone.dao.UserRepository;
import com.NetflixClone.dto.request.UserRequest;
import com.NetflixClone.dto.response.EmailValidationResponse;
import com.NetflixClone.dto.response.LoginResponse;
import com.NetflixClone.dto.response.MessageResponse;
import com.NetflixClone.entity.User;
import com.NetflixClone.enums.Role;
import com.NetflixClone.exception.*;
import com.NetflixClone.security.JwtUtil;
import com.NetflixClone.service.AuthService;
import com.NetflixClone.service.EmailService;
import com.NetflixClone.util.ServiceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EmailService emailService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ServiceUtils serviceUtils;

    @Override
    public MessageResponse signup(UserRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists!");
        }

        User user = new User();
        user.setEmail(userRequest.getEmail());
        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        user.setFullName(userRequest.getFullName());

        // Kullanici adi verilmezse e-postanin @ oncesi kullanilir
        String username = userRequest.getUsername();
        if (username == null || username.isBlank()) {
            username = userRequest.getEmail().substring(0, userRequest.getEmail().indexOf('@'));
        }
        user.setUsername(username);

        user.setRole(Role.USER);
        user.setActive(true);
        user.setEmailVerified(false);

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(Instant.now().plusSeconds(86400)); // 24 saat

        userRepository.save(user);

        emailService.sendVerificationEmail(userRequest.getEmail(), verificationToken);

        return new MessageResponse("Registration successful! Please check your email to verify your account!");
    }

    @Override
    public LoginResponse login(String email, String password) {
        // 1. Verilen email adresine sahip kullanıcıyı veritabanından bulmaya çalışır.
        //    Kullanıcı bulunamazsa Optional.empty() döner ve hemen aşağıda orElseThrow ile hata fırlatılır.
        //    Bu sayede "email yok" durumunda şifre kontrolü yapılmaz ve güvenlik açığı oluşmaz.
        User user = userRepository.findByEmail(email)
                // Email bulunamazsa doğrudan "Invalid email or password!" hatası fırlatılır.
                // Bu, hem email'in yanlış olması hem de şifrenin yanlış olması durumlarında aynı mesajı verir
                // (güvenlik açısından hangi kısmın yanlış olduğunu belli etmemek iyi bir pratiktir).
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password!"));

        // 2. Kullanıcı bulundu, şimdi şifreyi kontrol et.
        //    Gönderilen düz metin şifre ile veritabanındaki hash'lenmiş şifreyi karşılaştırır.
        //    Eşleşmezse yine aynı genel hata mesajı fırlatılır.
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password!");
        }

        // 3. Hesap aktif mi kontrol eder.
        //    Admin tarafından deaktive edilmiş hesaplar giriş yapamaz.
        if (!user.isActive()) {
            throw new AccountDeactivatedException("Your account has been deactivated. Please contact support for assistance.");
        }

        // 4. E-posta doğrulanmış mı kontrol eder.
        //    Doğrulama mailindeki linke tıklanmamışsa girişe izin verilmez.
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Please verify your email address before logging in. Check your inbox for the verification link.");
        }

        // 5. Tüm kontroller geçildiyse JWT token üretilir.
        //    Token'a email (username olarak) ve rol bilgisi eklenir.
        final String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        // 6. Başarılı giriş için LoginResponse nesnesi oluşturulup döndürülür.
        //    İçinde JWT token, email, tam ad, kullanıcı adı ve rol bilgisi yer alır.
        return new LoginResponse(token, user.getEmail(), user.getFullName(), user.getUsername(), user.getRole().name());
    }

    @Override
    public EmailValidationResponse validateEmail(String email) {
        boolean exists= userRepository.existsByEmail(email);
        return new EmailValidationResponse(exists,!exists);
    }

    @Override
    public MessageResponse verifyEmail(String token) {
        User user= userRepository.findByVerificationToken(token)
                .orElseThrow(()->new InvalidTokenException("Invalid or expired verification token."));

        if(user.getVerificationTokenExpiry()==null || user.getVerificationTokenExpiry().isBefore(Instant.now()))
        {
            throw new InvalidTokenException("Verification link has expired. Please request  a new one.");
        }
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
        return new MessageResponse("Email verified successfully! You can new login.");
    }

    @Override
    public MessageResponse resendVerificationEmail(String email) {
        User user = serviceUtils.getUserByEmailOrThrow(email);
        if (user.isEmailVerified()) {
            return new MessageResponse("Email is already verified! You can log in directly.");
            // Veya daha yumuşak bir mesaj dön:
        }
        // Yeni token üret
        String verificationToken = UUID.randomUUID().toString();

        // Token ve son geçerlilik tarihini güncelle
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(Instant.now().plusSeconds(86400)); // 24 saat
/*       if (user.getVerificationTokenExpiry() != null &&
                user.getVerificationTokenExpiry().isBefore(Instant.now())) {
            // Süre dolmuş, yeni token üret ve gönder (kod aynı devam eder)
        }
*/
        userRepository.save(user);
        emailService.sendVerificationEmail(email, verificationToken);
        return new MessageResponse("Verification email resent successfully! Please check your inbox.");
    }

    @Override
    public MessageResponse forgotPassword(String email) {
        User user=serviceUtils.getUserByEmailOrThrow(email);
        String resetToken=UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(Instant.now().plusSeconds(3600));
        userRepository.save(user);
        emailService.sendPasswordResetEmail(email,resetToken);
        return new MessageResponse("Password reset email sent successfully! Please check your inbox.");
    }

    @Override
    public MessageResponse resetPassword(String token, String newPassword) {
        User user= userRepository.findByPasswordResetToken(token).orElseThrow(()->new InvalidTokenException("Invalid or expired reset toekn!"));
        if(user.getPasswordResetTokenExpiry()==null || user.getPasswordResetTokenExpiry().isBefore(Instant.now()))
        {
            throw new InvalidTokenException("Reset token has expired!");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
        return new MessageResponse("Password reset successfully. You can now log in with your new password.");
    }

    @Override
    public MessageResponse changePassword(String email, String currentPassword, String newPassword) {
        User user=serviceUtils.getUserByEmailOrThrow(email);

        if (!passwordEncoder.matches(currentPassword,user.getPassword())){
            throw new InvalidCredentialsException("Current password is incorrect!");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return new MessageResponse("Password changed successfully...");
    }

    @Override
    public LoginResponse currentUser(String email) {
        User user=serviceUtils.getUserByEmailOrThrow(email);
        return new LoginResponse(null, user.getEmail(), user.getFullName(), user.getUsername(), user.getRole().name());

    }


}

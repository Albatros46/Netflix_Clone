package com.NetflixClone.config;

import com.NetflixClone.dao.UserRepository;
import com.NetflixClone.dao.VideoRepository;
import com.NetflixClone.entity.User;
import com.NetflixClone.entity.Video;
import com.NetflixClone.enums.Role;
import com.NetflixClone.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Legt beim Start einen Admin, einen Demo-Benutzer und Beispielfilme an.
 * Idempotent: vorhandene Benutzer (E-Mail) und Filme (Titel) werden nicht doppelt angelegt,
 * fehlende Poster werden neu erzeugt. Abschalten mit SEED_ENABLED=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private record SampleVideo(String title, String description, int year, String rating, int duration,
                               List<String> categories, String colorFrom, String colorTo) {}

    // Frei erfundene Titel - keine echten Filme
    private static final List<SampleVideo> SAMPLES = List.of(
            new SampleVideo("Die letzte Grenze", "Eine Raumfahrerin entdeckt am Rand des Sonnensystems ein Signal, das älter ist als die Menschheit.", 2023, "PG-13", 128, List.of("Sci-Fi", "Abenteuer"), "#0f2027", "#2c5364"),
            new SampleVideo("Schatten über Hamburg", "Ein Kommissar jagt einen Serientäter durch die nebligen Gassen der Speicherstadt.", 2022, "R", 112, List.of("Thriller", "Krimi"), "#232526", "#414345"),
            new SampleVideo("Sommer in Lissabon", "Zwei Fremde, ein verpasster Zug und eine Stadt voller Musik.", 2021, "PG", 98, List.of("Romantik", "Drama"), "#f7971e", "#ffd200"),
            new SampleVideo("Codename: Falke", "Eine Agentin muss ihre eigene Behörde ausspionieren, um einen Krieg zu verhindern.", 2024, "PG-13", 134, List.of("Action", "Thriller"), "#8e0e00", "#1f1c18"),
            new SampleVideo("Das Haus am See", "Eine Familie erbt ein altes Haus - und mit ihm ein Geheimnis, das niemand kennen sollte.", 2020, "R", 105, List.of("Horror", "Mystery"), "#000000", "#434343"),
            new SampleVideo("Großstadtchaos", "Ein chaotischer Umzug, drei Mitbewohner und ein Hund, der alles noch schlimmer macht.", 2023, "PG", 94, List.of("Komödie"), "#56ab2f", "#a8e063"),
            new SampleVideo("Tiefsee", "Eine Dokumentation über die unbekannten Lebewesen in 10.000 Metern Tiefe.", 2022, "G", 88, List.of("Dokumentation", "Natur"), "#000046", "#1cb5e0"),
            new SampleVideo("Die Erben des Drachen", "Fünf Königreiche, ein Thron und eine Prophezeiung, die alles verändert.", 2024, "PG-13", 142, List.of("Fantasy", "Abenteuer"), "#4b134f", "#c94b4b"),
            new SampleVideo("Neonnacht", "In einer Zukunftsstadt deckt ein Hacker eine Verschwörung der Konzerne auf.", 2025, "R", 119, List.of("Sci-Fi", "Action"), "#fc466b", "#3f5efb"),
            new SampleVideo("Der Weg nach Hause", "Ein Junge und sein Hund durchqueren die Alpen, um ihre Familie wiederzufinden.", 2019, "G", 101, List.of("Familie", "Abenteuer"), "#11998e", "#38ef7d"),
            new SampleVideo("Stille Wasser", "Eine Anwältin verteidigt den Mann, den sie für schuldig hält.", 2021, "PG-13", 116, List.of("Drama", "Krimi"), "#283c86", "#45a247"),
            new SampleVideo("Lachen verboten", "Ein Comedian muss in einem Land auftreten, in dem Humor illegal ist.", 2022, "PG-13", 92, List.of("Komödie", "Satire"), "#ee0979", "#ff6a00")
    );

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileService fileService;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(UserRepository userRepository, VideoRepository videoRepository, PasswordEncoder passwordEncoder,
                      FileService fileService,
                      @Value("${app.seed.admin-email:admin@netflix.local}") String adminEmail,
                      @Value("${app.seed.admin-password:admin123}") String adminPassword) {
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileService = fileService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        seedUser(adminEmail, adminPassword, "Administrator", Role.ADMIN);
        seedUser("user@netflix.local", "user123", "Demo Benutzer", Role.USER);

        int created = 0;
        for (int i = 0; i < SAMPLES.size(); i++) {
            SampleVideo sample = SAMPLES.get(i);
            String posterName = "seed-poster-" + (i + 1) + ".svg";
            // Poster immer (neu) schreiben - der Upload-Ordner kann geloescht worden sein
            fileService.storeImageBytes(poster(sample).getBytes(StandardCharsets.UTF_8), posterName);
            if (videoRepository.findByTitle(sample.title()).isPresent()) continue;

            Video video = new Video();
            video.setTitle(sample.title());
            video.setDescription(sample.description());
            video.setYear(sample.year());
            video.setRating(sample.rating());
            video.setDuration(sample.duration());
            video.setCategories(new ArrayList<>(sample.categories()));
            video.setPosterUuid(posterName);
            video.setPublished(true);
            videoRepository.save(video);
            created++;
        }
        log.info("Beispieldaten: {} neue Filme angelegt ({} insgesamt).", created, videoRepository.count());
    }

    private void seedUser(String email, String password, String fullName, Role role) {
        if (userRepository.existsByEmail(email)) return;
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setUsername(email.substring(0, email.indexOf('@')));
        user.setRole(role);
        user.setActive(true);
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Beispielbenutzer angelegt: {} ({})", email, role);
    }

    // Einfaches Filmplakat als SVG: Farbverlauf + Titel + Jahr
    private static String poster(SampleVideo sample) {
        String title = escape(sample.title());
        String[] words = title.split(" ");
        StringBuilder lines = new StringBuilder();
        StringBuilder line = new StringBuilder();
        List<String> wrapped = new ArrayList<>();
        for (String word : words) {
            if (line.length() + word.length() > 12 && !line.isEmpty()) {
                wrapped.add(line.toString().trim());
                line.setLength(0);
            }
            line.append(word).append(' ');
        }
        wrapped.add(line.toString().trim());
        int y = 330 - (wrapped.size() - 1) * 26;
        for (String w : wrapped) {
            lines.append("<text x=\"150\" y=\"").append(y).append("\" text-anchor=\"middle\" font-family=\"Arial Black, Arial, sans-serif\" font-size=\"34\" font-weight=\"900\" fill=\"#fff\">")
                    .append(w).append("</text>");
            y += 44;
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"300\" height=\"450\" viewBox=\"0 0 300 450\">"
                + "<defs><linearGradient id=\"g\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">"
                + "<stop offset=\"0\" stop-color=\"" + sample.colorFrom() + "\"/><stop offset=\"1\" stop-color=\"" + sample.colorTo() + "\"/>"
                + "</linearGradient></defs>"
                + "<rect width=\"300\" height=\"450\" fill=\"url(#g)\"/>"
                + "<circle cx=\"240\" cy=\"90\" r=\"120\" fill=\"#fff\" opacity=\"0.06\"/>"
                + "<text x=\"20\" y=\"44\" font-family=\"Arial Black, Arial, sans-serif\" font-size=\"30\" font-weight=\"900\" fill=\"#e50914\">N</text>"
                + lines
                + "<text x=\"150\" y=\"420\" text-anchor=\"middle\" font-family=\"Arial, sans-serif\" font-size=\"16\" fill=\"#ddd\">"
                + sample.year() + " · " + escape(String.join(" · ", sample.categories())) + "</text>"
                + "</svg>";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

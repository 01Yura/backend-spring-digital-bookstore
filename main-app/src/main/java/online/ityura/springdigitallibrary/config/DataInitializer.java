package online.ityura.springdigitallibrary.config;

import online.ityura.springdigitallibrary.model.*;
import online.ityura.springdigitallibrary.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class DataInitializer implements CommandLineRunner {

    private final Random random = new Random();
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private RatingRepository ratingRepository;
    @Autowired
    private ResourceLoader resourceLoader;
    @Value("${app.images.storage-path}")
    private String storagePath;
    @Value("${app.pdf.storage-path}")
    private String pdfStoragePath;
    private List<Book> savedBooks;

    @Override
    public void run(String... args) throws Exception {
        // Проверяем, существует ли уже админ
        if (!userRepository.existsByEmail("admin@gmail.com")) {
            User admin = User.builder()
                    .nickname("admin")
                    .email("admin@gmail.com")
                    .passwordHash(passwordEncoder.encode("admin"))
                    .role(Role.ADMIN)
                    .isVerified(true)  // Админ создается системой, email верифицирован по умолчанию
                    .build();

            userRepository.save(admin);
            System.out.println("Admin user created successfully!");
        } else {
            System.out.println("Admin user already exists.");
        }

        // Проверяем, существует ли уже пользователь crackMyPassword
        if (!userRepository.existsByEmail("crackMyPassword@gmail.com")) {
            User crackMyPassword = User.builder()
                    .nickname("crackMyPassword")
                    .email("crackMyPassword@gmail.com")
                    .passwordHash(passwordEncoder.encode("137Password123!@#"))
                    .role(Role.ADMIN)
                    .isVerified(true)  // Пользователь создается системой, email верифицирован по умолчанию
                    .build();

            userRepository.save(crackMyPassword);
            System.out.println("CrackMyPassword user created successfully!");
        } else {
            System.out.println("CrackMyPassword user already exists.");
        }

        // Инициализация книг
        initializeBooks();

        // Обработка картинок из папки pictures
        processBookImages();

        // Обработка PDF файлов из папки pdf
        processBookPdfs();

        // Добавление смешных отзывов к каждой второй книге
        addFunnyReviews();
    }

    private void initializeBooks() {
        // Массив данных о книгах
        BookData[] booksData = {
                new BookData("Spring Boot in Action", "Craig Walls", "Practical introduction to building applications with Spring Boot", 2021, Genre.TECHNOLOGY, BigDecimal.valueOf(150), BigDecimal.valueOf(20)),
                new BookData("Clean Code Explained", "Robert Martin", "Principles and best practices of writing clean and maintainable code", 2019, Genre.EDUCATION, BigDecimal.valueOf(120), BigDecimal.valueOf(15)),
                new BookData("The Lonely Developer", "Alex Turner", "Reflections on life, work, and solitude in the IT industry", 2020, Genre.FOR_PEOPLE_WITHOUT_PERSONAL_LIFE),
                new BookData("Algorithms for Night Owls", "Eugene Knuth", "Deep dive into algorithms for people who enjoy coding more than sleeping", 2018, Genre.FOR_NERDS, BigDecimal.valueOf(180), BigDecimal.valueOf(25)),
                new BookData("Fantasy of the Forgotten Kingdom", "Lara Moon", "Epic fantasy story about magic, dragons, and lost empires", 2015, Genre.FANTASY, BigDecimal.valueOf(80), BigDecimal.valueOf(10)),
                new BookData("Psychology of Motivation", "Daniel Harper", "How motivation works and how to stay productive", 2022, Genre.PSYCHOLOGY, BigDecimal.valueOf(140), BigDecimal.valueOf(30)),
                new BookData("Business 101", "Michael Roberts", "Basic concepts of modern business and management", 2017, Genre.BUSINESS, BigDecimal.valueOf(100), BigDecimal.valueOf(12)),
                new BookData("Dark Corners", "Stephen Black", "A collection of psychological horror stories", 2016, Genre.HORROR),
                new BookData("Romance Without Drama", "Emily Stone", "A calm and realistic take on modern relationships", 2023, Genre.ROMANCE, BigDecimal.valueOf(70), BigDecimal.valueOf(18)),
                new BookData("Adult Fiction Collection", "Anonymous Author", "Fiction intended for mature audiences", 2014, Genre.PORNO),
                new BookData("Spring Boot Recipes", "Craig Walls", "Collection of practical recipes and patterns for Spring Boot projects", 2020, Genre.TECHNOLOGY, BigDecimal.valueOf(160), BigDecimal.valueOf(22)),
                new BookData("Spring Security Basics", "Craig Walls", "Introduction to authentication and authorization concepts in Spring Security", 2022, Genre.TECHNOLOGY, BigDecimal.valueOf(170), BigDecimal.valueOf(28)),
                new BookData("Refactoring Habits", "Robert Martin", "How to refactor code safely with focus on readability and design", 2016, Genre.EDUCATION, BigDecimal.valueOf(130), BigDecimal.valueOf(20)),
                new BookData("Agile Team Rituals", "Robert Martin", "Team practices that help keep quality high and feedback loops short", 2018, Genre.BUSINESS, BigDecimal.valueOf(110), BigDecimal.valueOf(15)),
                new BookData("The Weekend Coder", "Alex Turner", "Stories and lessons learned from side projects and late-night debugging", 2019, Genre.FOR_PEOPLE_WITHOUT_PERSONAL_LIFE),
                new BookData("Silence in the Open Space", "Alex Turner", "A short novel about burnout, focus, and finding meaning in routine work", 2021, Genre.PSYCHOLOGY, BigDecimal.valueOf(90), BigDecimal.valueOf(10)),
                new BookData("Data Structures for Humans", "Eugene Knuth", "Data structures explained with real-life metaphors and examples", 2017, Genre.EDUCATION, BigDecimal.valueOf(125), BigDecimal.valueOf(18)),
                new BookData("Complexity. A Love Story", "Eugene Knuth", "A nerdy and funny journey through Big-O and algorithmic tradeoffs", 2020, Genre.FOR_NERDS, BigDecimal.valueOf(190), BigDecimal.valueOf(35)),
                new BookData("Dragons of the Northern Isles", "Lara Moon", "Fantasy adventure across icy seas and ancient ruins", 2016, Genre.FANTASY, BigDecimal.valueOf(75), BigDecimal.valueOf(12)),
                new BookData("The Wizard's Last Contract", "Lara Moon", "A mage signs a dangerous deal to save a kingdom", 2019, Genre.FANTASY, BigDecimal.valueOf(85), BigDecimal.valueOf(15)),
                new BookData("Mindset and Discipline", "Daniel Harper", "Practical techniques to build discipline and reduce procrastination", 2021, Genre.PSYCHOLOGY, BigDecimal.valueOf(135), BigDecimal.valueOf(25)),
                new BookData("The Habit Loop", "Daniel Harper", "How habits are formed and how to redesign them for better results", 2018, Genre.PSYCHOLOGY, BigDecimal.valueOf(145), BigDecimal.valueOf(30)),
                new BookData("Startup Finance Made Simple", "Michael Roberts", "Basics of budgeting, runway, and financial planning for startups", 2020, Genre.BUSINESS, BigDecimal.valueOf(115), BigDecimal.valueOf(20)),
                new BookData("Management Without Micromanagement", "Michael Roberts", "How to lead teams with trust, clarity, and measurable outcomes", 2022, Genre.BUSINESS, BigDecimal.valueOf(155), BigDecimal.valueOf(28)),
                new BookData("Shadows Under the Bed", "Stephen Black", "Horror tales about childhood fears returning in adult life", 2015, Genre.HORROR),
                new BookData("The House That Watches", "Stephen Black", "A psychological horror novel about a town with a secret", 2018, Genre.HORROR, BigDecimal.valueOf(95), BigDecimal.valueOf(10)),
                new BookData("Love in Small Steps", "Emily Stone", "A gentle romance about building trust and emotional safety", 2020, Genre.ROMANCE, BigDecimal.valueOf(65), BigDecimal.valueOf(12)),
                new BookData("Messages at Midnight", "Emily Stone", "Romantic drama told through letters, texts, and late-night calls", 2022, Genre.ROMANCE, BigDecimal.valueOf(72), BigDecimal.valueOf(15)),
                new BookData("Intro to Software Testing", "Nina Carter", "Testing basics: test design, bugs, and working with requirements", 2019, Genre.EDUCATION, BigDecimal.valueOf(128), BigDecimal.valueOf(22)),
                new BookData("API Testing Playbook", "Nina Carter", "Practical guide to REST API testing: cases, checks, and common pitfalls", 2023, Genre.EDUCATION, BigDecimal.valueOf(165), BigDecimal.valueOf(32)),
                new BookData("Big-O Until Morning", "Eugene Knuth", "A true story about optimizing algorithms instead of sleeping", 2021, Genre.FOR_NERDS, BigDecimal.valueOf(175), BigDecimal.valueOf(30)),
                new BookData("Hello World, My Old Friend", "Max Compiler", "Existential thoughts of a developer who prints logs for a living", 2019, Genre.FOR_NERDS),
                new BookData("404. Social Life Not Found", "Alex Turner", "Advanced guide to avoiding people using code, headphones, and deadlines", 2020, Genre.FOR_NERDS, BigDecimal.valueOf(88), BigDecimal.valueOf(10)),
                new BookData("Refactor First, Ask Questions Later", "Robert Martin", "Why touching legacy code is scary but still unavoidable", 2018, Genre.FOR_NERDS, BigDecimal.valueOf(142), BigDecimal.valueOf(25)),
                new BookData("Git Push and Pray", "Nina Carter", "A spiritual journey through force-pushes and broken pipelines", 2022, Genre.FOR_NERDS, BigDecimal.valueOf(148), BigDecimal.valueOf(28)),
                new BookData("Fifty Shades of Pull Requests", "Anonymous Author", "A forbidden romance between a junior developer and production branch", 2016, Genre.PORNO),
                new BookData("Hardcoded Desires", "Johnny Runtime", "Passion, constants, and values that should never be in plain text", 2017, Genre.PORNO, BigDecimal.valueOf(60), BigDecimal.valueOf(10)),
                new BookData("Backend After Midnight", "Lola Stacktrace", "Things get hot when the frontend finally stops calling", 2019, Genre.PORNO),
                new BookData("Deploy Me Gently", "Johnny Runtime", "A slow and careful release with unexpected rollbacks", 2021, Genre.PORNO, BigDecimal.valueOf(55), BigDecimal.valueOf(12)),
                new BookData("Touch My Endpoint", "Lola Stacktrace", "A very insecure API exposed to the whole internet", 2023, Genre.PORNO),
                new BookData("Stack Overflow Is My Therapist", "Ivan Debugov", "How copy-paste, downvotes, and comments keep developers sane", 2021, Genre.FOR_NERDS, BigDecimal.valueOf(92), BigDecimal.valueOf(15)),
                new BookData("Segmentation Faults and Broken Hearts", "Anna CoreDump", "When memory leaks hurt more than breakups", 2020, Genre.FOR_NERDS, BigDecimal.valueOf(138), BigDecimal.valueOf(20)),
                new BookData("Big-O and Chill", "Eugene Knuth", "Romantic evenings spent optimizing algorithms", 2019, Genre.FOR_NERDS, BigDecimal.valueOf(168), BigDecimal.valueOf(35)),
                new BookData("Regex Made Me Cry", "Victor Pattern", "True stories of developers vs regular expressions", 2018, Genre.FOR_NERDS, BigDecimal.valueOf(132), BigDecimal.valueOf(22)),
                new BookData("The Zen of NullPointerException", "Sam Exception", "Finding inner peace while debugging production crashes", 2022, Genre.FOR_NERDS, BigDecimal.valueOf(152), BigDecimal.valueOf(28)),
                new BookData("Works on My Machine", "Localhost Hero", "A philosophical study of environment-specific bugs", 2017, Genre.FOR_NERDS),
                new BookData("Concurrency for Masochists", "Thread Master", "Why synchronized blocks destroy your soul", 2020, Genre.FOR_NERDS, BigDecimal.valueOf(185), BigDecimal.valueOf(40)),
                new BookData("Docker Containers and Existential Questions", "Lina DevOps", "Why everything works only after rebuilding the image", 2023, Genre.FOR_NERDS, BigDecimal.valueOf(172), BigDecimal.valueOf(32)),
                new BookData("The Sacred Art of Console Logging", "Println Monk", "Advanced debugging techniques using System.out", 2016, Genre.FOR_NERDS, BigDecimal.valueOf(98), BigDecimal.valueOf(10)),
                new BookData("Git Rebase: Choose Your Own Adventure", "Branch Rider", "Every wrong move leads to force-push", 2022, Genre.FOR_NERDS, BigDecimal.valueOf(158), BigDecimal.valueOf(30)),
                new BookData("Friday Night With Jenkins", "CI Lover", "How pipelines replaced friends and hobbies", 2019, Genre.FOR_PEOPLE_WITHOUT_PERSONAL_LIFE),
                new BookData("My Only Dates Are Deadlines", "Alex Turner", "A developer's guide to emotional availability via Jira", 2021, Genre.FOR_PEOPLE_WITHOUT_PERSONAL_LIFE, BigDecimal.valueOf(105), BigDecimal.valueOf(18)),
                new BookData("Living Alone With Legacy Code", "Refactor Ghost", "Why nobody touches it and everyone is afraid", 2018, Genre.FOR_PEOPLE_WITHOUT_PERSONAL_LIFE),
                new BookData("Weekend Plans. Fix Production", "OnCall Engineer", "Stories of canceled vacations and " +
                        "midnight alerts", 2020, Genre.FOR_PEOPLE_WITHOUT_PERSONAL_LIFE, BigDecimal.valueOf(112), BigDecimal.valueOf(20)),
                new BookData("Headphones On, World Off", "Silent Dev", "Escaping human interaction using noise cancellation", 2017, Genre.FOR_PEOPLE_WITHOUT_PERSONAL_LIFE),
                new BookData("Burnout as a Feature", "Agile Survivor", "Why sprints never end and weekends don't exist", 2022, Genre.FOR_PEOPLE_WITHOUT_PERSONAL_LIFE, BigDecimal.valueOf(122), BigDecimal.valueOf(25)),
                new BookData("Alone in the Open Space", "Desk Nomad", "Surrounded by people, talking only to Slack", 2019, Genre.FOR_PEOPLE_WITHOUT_PERSONAL_LIFE),
                new BookData("No Meetings, No Feelings", "Calendar Hater", "Emotional minimalism for remote developers", 2023, Genre.FOR_PEOPLE_WITHOUT_PERSONAL_LIFE, BigDecimal.valueOf(118), BigDecimal.valueOf(22)),
                new BookData("Side Projects and Silent Evenings", "Pet Projector", "Replacing social life with unfinished repositories", 2020, Genre.FOR_PEOPLE_WITHOUT_PERSONAL_LIFE),
                new BookData("404. Friends Not Found", "Alex Turner", "Advanced isolation techniques using code and " +
                        "excuses", 2018, Genre.FOR_PEOPLE_WITHOUT_PERSONAL_LIFE, BigDecimal.valueOf(102), BigDecimal.valueOf(15)),
                new BookData("The Commit That Should Not Exist", "Stephen Black", "A horror story about a mysterious commit in main branch", 2017, Genre.HORROR, BigDecimal.valueOf(87), BigDecimal.valueOf(10)),
                new BookData("Production at 3 AM", "Night Pager", "Every alert hides something unspeakable", 2021, Genre.HORROR, BigDecimal.valueOf(108), BigDecimal.valueOf(18)),
                new BookData("The Server Room Whispered Back", "Daemon Root", "Strange things happen when logs start writing themselves", 2019, Genre.HORROR),
                new BookData("They Deployed on Friday", "Release Manager", "A cautionary tale of broken rules and haunted weekends", 2018, Genre.HORROR, BigDecimal.valueOf(94), BigDecimal.valueOf(12)),
                new BookData("The Log File Never Ends", "Infinite Stream", "Scrolling logs until something scrolls back", 2020, Genre.HORROR),
                new BookData("Unknown Error", "Stack Trace", "No message, no code, only fear", 2016, Genre.HORROR, BigDecimal.valueOf(82), BigDecimal.valueOf(10)),
                new BookData("The Test That Sometimes Passes", "Flaky Master", "Unreproducible failures and creeping madness", 2022, Genre.HORROR, BigDecimal.valueOf(116), BigDecimal.valueOf(20)),
                new BookData("Haunted Microservice", "Service Mesh", "One service fails and nobody knows why", 2023, Genre.HORROR, BigDecimal.valueOf(162), BigDecimal.valueOf(30)),
                new BookData("The Password Was Correct Yesterday", "Auth Keeper", "Authentication horror beyond human understanding", 2019, Genre.HORROR),
                new BookData("Legacy System: Do Not Touch", "Stephen Black", "Every change makes it worse", 2015, Genre.HORROR, BigDecimal.valueOf(78), BigDecimal.valueOf(10)),
                new BookData("The Last Deploy", "Mark Runtime", "A fictional story about a developer facing one final release", 2020, Genre.FICTION, BigDecimal.valueOf(103), BigDecimal.valueOf(15)),
                new BookData("Code Between the Lines", "Anna Script", "Human drama hidden inside source code", 2018, Genre.FICTION),
                new BookData("How Software Really Breaks", "David Systems", "Real stories of failures in complex IT systems", 2019, Genre.NON_FICTION, BigDecimal.valueOf(147), BigDecimal.valueOf(28)),
                new BookData("The Reality of Agile", "Scrum Masterson", "What agile looks like outside presentations", 2021, Genre.NON_FICTION, BigDecimal.valueOf(153), BigDecimal.valueOf(32)),
                new BookData("The Disappearing Commit", "Logan Hash", "A detective investigates a missing commit in production", 2017, Genre.MYSTERY, BigDecimal.valueOf(97), BigDecimal.valueOf(12)),
                new BookData("Murder in the Server Room", "Binary Holmes", "A classic whodunit inside a data center", 2016, Genre.MYSTERY),
                new BookData("Zero-Day Countdown", "Ethan Firewall", "A race against time to stop a global exploit", 2022, Genre.THRILLER, BigDecimal.valueOf(167), BigDecimal.valueOf(35)),
                new BookData("Rollback Protocol", "Nina Carter", "A failed deploy turns into a survival thriller", 2020, Genre.THRILLER, BigDecimal.valueOf(143), BigDecimal.valueOf(25)),
                new BookData("Love on Standby", "Emily Stone", "Romance between two engineers on on-call duty", 2021, Genre.ROMANCE, BigDecimal.valueOf(68), BigDecimal.valueOf(10)),
                new BookData("Merged Hearts", "Pull Request", "Love blossoms during endless code reviews", 2019, Genre.ROMANCE, BigDecimal.valueOf(74), BigDecimal.valueOf(18)),
                new BookData("The Last Algorithm", "Isaac Neural", "An AI writes code better than humans", 2023, Genre.SCIENCE_FICTION, BigDecimal.valueOf(178), BigDecimal.valueOf(38)),
                new BookData("Beyond the Cloud", "Nova Stack", "Humanity lives inside distributed systems", 2020, Genre.SCIENCE_FICTION, BigDecimal.valueOf(164), BigDecimal.valueOf(30)),
                new BookData("The Wizard of DevOps", "Lara Moon", "Magic, pipelines, and ancient automation spells", 2018, Genre.FANTASY, BigDecimal.valueOf(89), BigDecimal.valueOf(15)),
                new BookData("The Code Rune", "Eldor Syntax", "A fantasy world powered by forbidden code", 2016, Genre.FANTASY),
                new BookData("The System Never Sleeps", "Stephen Black", "Servers whisper when nobody is watching", 2019, Genre.HORROR, BigDecimal.valueOf(106), BigDecimal.valueOf(20)),
                new BookData("Friday Night Release", "Night Pager", "Pure horror disguised as a deployment", 2017, Genre.HORROR),
                new BookData("Before the Internet", "Alan Archive", "Life and work in the pre-digital era", 2015, Genre.HISTORICAL, BigDecimal.valueOf(84), BigDecimal.valueOf(10)),
                new BookData("The Birth of Computing", "Grace History", "How early computers changed the world", 2014, Genre.HISTORICAL),
                new BookData("The Life of a Programmer", "Code Writer", "A biography of an engineer who changed everything", 2021, Genre.BIOGRAPHY, BigDecimal.valueOf(149), BigDecimal.valueOf(28)),
                new BookData("From Punch Cards to Cloud", "Legacy Dev", "One career across multiple IT eras", 2018, Genre.BIOGRAPHY, BigDecimal.valueOf(136), BigDecimal.valueOf(22)),
                new BookData("I Shipped It Myself", "Solo Founder", "Autobiography of a one-person startup", 2020, Genre.AUTOBIOGRAPHY, BigDecimal.valueOf(127), BigDecimal.valueOf(20)),
                new BookData("My Life in Bugs", "QA Veteran", "Personal story of a tester", 2017, Genre.AUTOBIOGRAPHY),
                new BookData("On Call Forever", "Sleep Deprived", "Memories from years of night alerts", 2019, Genre.MEMOIR, BigDecimal.valueOf(114), BigDecimal.valueOf(18)),
                new BookData("The Startup Years", "Burnout Survivor", "Memoirs of chaos, hope, and pivots", 2022, Genre.MEMOIR, BigDecimal.valueOf(156), BigDecimal.valueOf(30)),
                new BookData("Do Machines Think?", "Logic Mind", "Philosophical questions of artificial intelligence", 2016, Genre.PHILOSOPHY, BigDecimal.valueOf(141), BigDecimal.valueOf(25)),
                new BookData("Clean Code and Ethics", "Robert Martin", "Moral responsibility of developers", 2018, Genre.PHILOSOPHY),
                new BookData("The Programmer's Brain", "Daniel Harper", "How developers think and solve problems", 2021, Genre.PSYCHOLOGY, BigDecimal.valueOf(151), BigDecimal.valueOf(28)),
                new BookData("Burnout Patterns", "Mind Debugger", "Psychology of chronic overwork in IT", 2020, Genre.PSYCHOLOGY, BigDecimal.valueOf(139), BigDecimal.valueOf(25)),
                new BookData("How Not to Burn Out", "Life Hacker", "Self-help for tired engineers", 2022, Genre.SELF_HELP, BigDecimal.valueOf(123), BigDecimal.valueOf(22)),
                new BookData("Focus Without Coffee", "Deep Worker", "Productivity tips for developers", 2019, Genre.SELF_HELP),
                new BookData("Tech Startup Basics", "Michael Roberts", "Building a business around software", 2018, Genre.BUSINESS, BigDecimal.valueOf(129), BigDecimal.valueOf(20)),
                new BookData("Scaling Teams, Not Egos", "CTO Notes", "Business lessons from growing engineering teams", 2021, Genre.BUSINESS, BigDecimal.valueOf(157), BigDecimal.valueOf(32)),
                new BookData("Modern Backend Systems", "API Architect", "Designing scalable server-side applications", 2022, Genre.TECHNOLOGY, BigDecimal.valueOf(173), BigDecimal.valueOf(35)),
                new BookData("Cloud Native Reality", "DevOps Cloud", "What cloud computing really costs", 2020, Genre.TECHNOLOGY, BigDecimal.valueOf(159), BigDecimal.valueOf(30)),
                new BookData("Computer Science Explained", "Data Thinker", "Scientific foundations of computing", 2017, Genre.SCIENCE, BigDecimal.valueOf(144), BigDecimal.valueOf(28)),
                new BookData("The Math Behind Code", "Algo Scientist", "Why algorithms work", 2016, Genre.SCIENCE),
                new BookData("Learning Java the Hard Way", "Stack Teacher", "Educational journey through Java", 2019, Genre.EDUCATION, BigDecimal.valueOf(133), BigDecimal.valueOf(22)),
                new BookData("Testing 101", "Nina Carter", "Foundations of software testing", 2021, Genre.EDUCATION, BigDecimal.valueOf(161), BigDecimal.valueOf(30)),
                new BookData("Cooking for Developers", "Byte Chef", "Fast meals between builds", 2018, Genre.COOKING, BigDecimal.valueOf(76), BigDecimal.valueOf(10)),
                new BookData("One-Pan Deploy Dinners", "Lazy Ops", "Minimal effort cooking for IT people", 2022, Genre.COOKING),
                new BookData("Remote Work Around the World", "Nomad Dev", "Traveling while coding", 2020, Genre.TRAVEL, BigDecimal.valueOf(109), BigDecimal.valueOf(15)),
                new BookData("Cafes with Wi-Fi", "Digital Nomad", "Best places to work remotely", 2019, Genre.TRAVEL),
                new BookData("Ode to the Bug", "Poet QA", "Poems about failing tests", 2017, Genre.POETRY, BigDecimal.valueOf(63), BigDecimal.valueOf(10)),
                new BookData("Lines of Code and Life", "Syntax Poet", "Poetry inspired by programming", 2021, Genre.POETRY),
                new BookData("The Standup Meeting", "Office Playwright", "Drama unfolds in daily syncs", 2018, Genre.DRAMA, BigDecimal.valueOf(91), BigDecimal.valueOf(12)),
                new BookData("Deadline", "Project Manager", "A tragic story of impossible timelines", 2020, Genre.DRAMA),
                new BookData("Agile Gone Wrong", "Scrum Joker", "Comedy stories from IT teams", 2019, Genre.COMEDY, BigDecimal.valueOf(86), BigDecimal.valueOf(10)),
                new BookData("Funny Things in Production", "LOL Ops", "Laughing through incidents", 2022, Genre.COMEDY, BigDecimal.valueOf(99), BigDecimal.valueOf(15)),
                new BookData("Journey to Legacy Code", "Refactor Hero", "An epic adventure through old systems", 2016, Genre.ADVENTURE),
                new BookData("The Migration Quest", "Cloud Ranger", "Adventure of moving to the cloud", 2021, Genre.ADVENTURE, BigDecimal.valueOf(154), BigDecimal.valueOf(28)),
                new BookData("The Wild Wild Web", "Cowboy Dev", "Frontend battles in the digital west", 2017, Genre.WESTERN, BigDecimal.valueOf(93), BigDecimal.valueOf(12)),
                new BookData("High Noon at Production", "Release Sheriff", "Classic western with servers and guns", 2015, Genre.WESTERN),
                new BookData("My First Startup", "Teen Founder", "Young adult story about coding and dreams", 2020, Genre.YOUNG_ADULT, BigDecimal.valueOf(107), BigDecimal.valueOf(18)),
                new BookData("Hackathon Nights", "Junior Dev", "Friendship and code under pressure", 2019, Genre.YOUNG_ADULT),
                new BookData("Little Programmer", "Code Fairy", "Introducing kids to coding", 2021, Genre.CHILDREN, BigDecimal.valueOf(58), BigDecimal.valueOf(10)),
                new BookData("The Friendly Robot", "AI Tales", "A robot learns to help people", 2018, Genre.CHILDREN),
                new BookData("Commit After Dark", "Johnny Runtime", "Forbidden merges and hotfixes", 2017, Genre.PORNO, BigDecimal.valueOf(62), BigDecimal.valueOf(12)),
                new BookData("Secrets of the Private Repo", "Anonymous Author", "Things hidden from public access", 2019, Genre.PORNO),
                new BookData("Stack Trace Poetry", "Null Pointer", "Finding beauty in exceptions", 2020, Genre.FOR_NERDS, BigDecimal.valueOf(96), BigDecimal.valueOf(15)),
                new BookData("Advanced Sarcasm in Code Reviews", "Senior Dev", "A survival guide", 2022, Genre.FOR_NERDS, BigDecimal.valueOf(146), BigDecimal.valueOf(28)),
                new BookData("Living With Jira", "Task Manager", "When tickets replace people", 2018, Genre.FOR_PEOPLE_WITHOUT_PERSONAL_LIFE),
                new BookData("Alone With My Laptop", "Remote Worker", "Life between commits", 2021, Genre.FOR_PEOPLE_WITHOUT_PERSONAL_LIFE, BigDecimal.valueOf(111), BigDecimal.valueOf(20))
        };


        int createdCount = 0;
        int existingCount = 0;
        List<Book> savedBooks = new ArrayList<>();

        for (BookData bookData : booksData) {
            // Находим или создаем автора
            Author author = authorRepository.findByFullName(bookData.authorName)
                    .orElseGet(() -> {
                        Author newAuthor = Author.builder()
                                .fullName(bookData.authorName)
                                .build();
                        return authorRepository.save(newAuthor);
                    });

            // Проверяем, существует ли уже книга с таким названием и автором
            Book book;
            if (!bookRepository.existsByTitleAndAuthorId(bookData.title, author.getId())) {
                Book.BookBuilder bookBuilder = Book.builder()
                        .title(bookData.title)
                        .author(author)
                        .description(bookData.description)
                        .publishedYear(bookData.publishedYear)
                        .genre(bookData.genre)
                        .deletionLocked(false);

                // Устанавливаем цену и скидку, если они указаны
                if (bookData.price != null) {
                    bookBuilder.price(bookData.price);
                }
                if (bookData.discountPercent != null) {
                    bookBuilder.discountPercent(bookData.discountPercent);
                }

                book = bookBuilder.build();
                book = bookRepository.save(book);
                createdCount++;
            } else {
                book = bookRepository.findByTitleAndAuthorId(bookData.title, author.getId())
                        .orElse(null);
                if (book != null) {
                    // Обновляем цену и скидку для существующих книг, если они не заблокированы от удаления
                    boolean updated = false;
                    if (!book.getDeletionLocked()) {
                        if (bookData.price != null) {
                            book.setPrice(bookData.price);
                            updated = true;
                        }
                        if (bookData.discountPercent != null) {
                            book.setDiscountPercent(bookData.discountPercent);
                            updated = true;
                        }
                        if (updated) {
                            book = bookRepository.save(book);
                        }
                    }
                }
                existingCount++;
            }

            if (book != null) {
                savedBooks.add(book);
            }
        }

        System.out.println("Books initialization completed: " + createdCount + " created, " + existingCount + " already exist.");

        // Сохраняем список книг для последующего добавления отзывов
        this.savedBooks = savedBooks;
    }

    private void processBookImages() {
        try {
            // Получаем путь к папке pictures в resources
            Resource picturesResource = resourceLoader.getResource("classpath:pictures");
            Path picturesPath = null;

            try {
                if (picturesResource.exists()) {
                    // Пытаемся получить путь как файл (работает в IDE и при запуске из файловой системы)
                    java.io.File file = picturesResource.getFile();
                    if (file.exists() && file.isDirectory()) {
                        picturesPath = file.toPath();
                    }
                }
            } catch (Exception e) {
                // Если не удалось получить как файл (например, в JAR), пробуем альтернативные пути
            }

            // Альтернативный путь для работы внутри Docker-контейнера:
            // в Dockerfile мы копируем исходные картинки в /opt/spring-digital-bookstore/pictures-source
            if (picturesPath == null || !Files.exists(picturesPath)) {
                Path dockerPicturesPath = Paths.get("/opt/spring-digital-bookstore/pictures-source");
                if (Files.exists(dockerPicturesPath) && Files.isDirectory(dockerPicturesPath)) {
                    picturesPath = dockerPicturesPath;
                }
            }

            // Альтернативный способ получения пути для разработки (IDE / локальный запуск)
            if (picturesPath == null || !Files.exists(picturesPath)) {
                picturesPath = Paths.get("src/main/resources/pictures");
                if (!Files.exists(picturesPath)) {
                    System.out.println("Pictures directory not found. Skipping image processing.");
                    return;
                }
            }

            // Создаем папку для хранения изображений если её нет
            Path storageDir = Paths.get(storagePath);
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
                System.out.println("Created storage directory: " + storagePath);
            }

            // Получаем все книги из базы данных
            List<Book> allBooks = bookRepository.findAll();

            AtomicInteger processedCount = new AtomicInteger(0);
            AtomicInteger matchedCount = new AtomicInteger(0);

            // Проходим по всем файлам в папке pictures
            if (Files.exists(picturesPath) && Files.isDirectory(picturesPath)) {
                try (java.util.stream.Stream<Path> files = Files.list(picturesPath)) {
                    files.filter(Files::isRegularFile)
                            .forEach(imageFile -> {
                                try {
                                    String imageFileName = imageFile.getFileName().toString();
                                    // Убираем расширение для сравнения
                                    String imageNameWithoutExt = imageFileName;
                                    int lastDotIndex = imageFileName.lastIndexOf('.');
                                    if (lastDotIndex > 0) {
                                        imageNameWithoutExt = imageFileName.substring(0, lastDotIndex);
                                    }

                                    // Нормализуем имя файла: заменяем _ на пробелы, приводим к нижнему регистру
                                    String normalizedImageName = imageNameWithoutExt
                                            .replaceAll("_", " ")
                                            .replaceAll("\\s+", " ") // Нормализуем множественные пробелы
                                            .toLowerCase()
                                            .trim();

                                    // Ищем соответствующую книгу
                                    for (Book book : allBooks) {
                                        // Пропускаем книги, у которых уже есть изображение
                                        if (book.getImagePath() != null && !book.getImagePath().isEmpty()) {
                                            continue;
                                        }

                                        // Нормализуем название книги: приводим к нижнему регистру, нормализуем пробелы
                                        String bookTitleNormalized = book.getTitle()
                                                .replaceAll("\\s+", " ") // Нормализуем множественные пробелы
                                                .toLowerCase()
                                                .trim();

                                        // Сравниваем нормализованные имена
                                        if (normalizedImageName.equals(bookTitleNormalized)) {
                                            try {
                                                // Получаем расширение файла
                                                String extension = "";
                                                if (lastDotIndex > 0) {
                                                    extension = imageFileName.substring(lastDotIndex);
                                                }

                                                // Формируем имя файла для сохранения (на основе названия книги)
                                                // Заменяем пробелы на _, но сохраняем все символы включая русские
                                                String sanitizedTitle = book.getTitle()
                                                        .replaceAll("\\s+", "_")
                                                        .replaceAll("[<>:\"|?*]", ""); // Удаляем только недопустимые символы для Windows/Linux
                                                String targetFileName = sanitizedTitle + extension;

                                                // Путь для сохранения
                                                Path targetPath = storageDir.resolve(targetFileName);

                                                // Если файл уже существует, не копируем заново и не меняем имя
                                                if (Files.exists(targetPath)) {
                                                    System.out.println("Image file already exists, skipping copy: " + targetFileName);
                                                } else {
                                                    // Копируем файл (создаем новый)
                                                    Files.copy(imageFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
                                                }

                                                // Обновляем путь к изображению в базе данных
                                                book.setImagePath(targetPath.toString());
                                                bookRepository.save(book);

                                                System.out.println("Matched and copied image for book: " + book.getTitle() + " -> " + targetFileName);
                                                matchedCount.incrementAndGet();
                                            } catch (IOException e) {
                                                System.err.println("Failed to copy image for book " + book.getTitle() + ": " + e.getMessage());
                                            }
                                            break; // Нашли соответствие, переходим к следующему файлу
                                        }
                                    }
                                    processedCount.incrementAndGet();
                                } catch (Exception e) {
                                    System.err.println("Error processing image file " + imageFile.getFileName() + ": " + e.getMessage());
                                }
                            });
                } catch (IOException e) {
                    System.err.println("Error listing files in pictures directory: " + e.getMessage());
                }
            }

            System.out.println("Image processing completed: " + processedCount.get() + " files processed, " + matchedCount.get() + " images matched and copied.");
        } catch (Exception e) {
            System.err.println("Error during image processing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processBookPdfs() {
        try {
            System.out.println("Starting PDF processing...");
            System.out.println("PDF storage path: " + pdfStoragePath);

            // Получаем путь к папке pdf в resources
            Resource pdfResource = resourceLoader.getResource("classpath:pdf");
            Path pdfPath = null;

            try {
                if (pdfResource.exists()) {
                    // Пытаемся получить путь как файл (работает в IDE и при запуске из файловой системы)
                    java.io.File file = pdfResource.getFile();
                    if (file.exists() && file.isDirectory()) {
                        pdfPath = file.toPath();
                        System.out.println("Found PDF source directory (classpath): " + pdfPath);
                    }
                }
            } catch (Exception e) {
                // Если не удалось получить как файл (например, в JAR), пробуем альтернативные пути
                System.out.println("Could not access classpath:pdf as file (expected in JAR): " + e.getMessage());
            }

            // Альтернативный путь для работы внутри Docker-контейнера:
            // в Dockerfile мы копируем исходные PDF файлы в /opt/spring-digital-bookstore/pdf-source
            if (pdfPath == null || !Files.exists(pdfPath)) {
                Path dockerPdfPath = Paths.get("/opt/spring-digital-bookstore/pdf-source");
                System.out.println("Checking Docker PDF source path: " + dockerPdfPath);
                if (Files.exists(dockerPdfPath) && Files.isDirectory(dockerPdfPath)) {
                    pdfPath = dockerPdfPath;
                    System.out.println("Found PDF source directory (Docker): " + pdfPath);
                } else {
                    System.out.println("Docker PDF source directory does not exist: " + dockerPdfPath);
                }
            }

            // Альтернативный способ получения пути для разработки (IDE / локальный запуск)
            if (pdfPath == null || !Files.exists(pdfPath)) {
                pdfPath = Paths.get("src/main/resources/pdf");
                System.out.println("Checking development PDF source path: " + pdfPath);
                if (!Files.exists(pdfPath)) {
                    System.out.println("PDF directory not found in any location. Skipping PDF processing.");
                    System.out.println("Checked paths:");
                    System.out.println("  1. classpath:pdf");
                    System.out.println("  2. /opt/spring-digital-bookstore/pdf-source");
                    System.out.println("  3. src/main/resources/pdf");
                    return;
                } else {
                    System.out.println("Found PDF source directory (development): " + pdfPath);
                }
            }

            // Создаем папку для хранения PDF файлов если её нет
            Path storageDir = Paths.get(pdfStoragePath);
            System.out.println("PDF storage directory: " + storageDir);
            if (!Files.exists(storageDir)) {
                try {
                    Files.createDirectories(storageDir);
                    System.out.println("Created PDF storage directory: " + pdfStoragePath);
                } catch (Exception e) {
                    System.err.println("Failed to create PDF storage directory: " + pdfStoragePath);
                    System.err.println("Error: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            } else {
                System.out.println("PDF storage directory already exists: " + pdfStoragePath);
            }

            // Получаем все книги из базы данных
            List<Book> allBooks = bookRepository.findAll();

            AtomicInteger processedCount = new AtomicInteger(0);
            AtomicInteger matchedCount = new AtomicInteger(0);

            // Проходим по всем файлам в папке pdf
            System.out.println("Processing PDF files from: " + pdfPath);
            if (Files.exists(pdfPath) && Files.isDirectory(pdfPath)) {
                System.out.println("PDF source directory exists and is a directory");
                try (java.util.stream.Stream<Path> files = Files.list(pdfPath)) {
                    files.filter(Files::isRegularFile)
                            .filter(path -> {
                                String fileName = path.getFileName().toString().toLowerCase();
                                return fileName.endsWith(".pdf");
                            })
                            .forEach(pdfFile -> {
                                try {
                                    String pdfFileName = pdfFile.getFileName().toString();
                                    // Убираем расширение для сравнения
                                    String pdfNameWithoutExt = pdfFileName;
                                    int lastDotIndex = pdfFileName.lastIndexOf('.');
                                    if (lastDotIndex > 0) {
                                        pdfNameWithoutExt = pdfFileName.substring(0, lastDotIndex);
                                    }

                                    // Нормализуем имя файла: заменяем _ на пробелы, приводим к нижнему регистру
                                    String normalizedPdfName = pdfNameWithoutExt
                                            .replaceAll("_", " ")
                                            .replaceAll("\\s+", " ") // Нормализуем множественные пробелы
                                            .toLowerCase()
                                            .trim();

                                    // Ищем соответствующую книгу
                                    for (Book book : allBooks) {
                                        // Пропускаем книги, у которых уже есть PDF файл
                                        if (book.getPdfPath() != null && !book.getPdfPath().isEmpty()) {
                                            continue;
                                        }

                                        // Нормализуем название книги: приводим к нижнему регистру, нормализуем пробелы
                                        String bookTitleNormalized = book.getTitle()
                                                .replaceAll("\\s+", " ") // Нормализуем множественные пробелы
                                                .toLowerCase()
                                                .trim();

                                        // Сравниваем нормализованные имена
                                        if (normalizedPdfName.equals(bookTitleNormalized)) {
                                            try {
                                                // Получаем расширение файла
                                                String extension = "";
                                                if (lastDotIndex > 0) {
                                                    extension = pdfFileName.substring(lastDotIndex);
                                                }

                                                // Формируем имя файла для сохранения (на основе названия книги)
                                                // Заменяем пробелы на _, но сохраняем все символы включая русские
                                                String sanitizedTitle = book.getTitle()
                                                        .replaceAll("\\s+", "_")
                                                        .replaceAll("[<>:\"|?*]", ""); // Удаляем только недопустимые символы для Windows/Linux
                                                String targetFileName = sanitizedTitle + extension;

                                                // Путь для сохранения
                                                Path targetPath = storageDir.resolve(targetFileName);

                                                // Если файл уже существует, не копируем заново и не меняем имя
                                                if (Files.exists(targetPath)) {
                                                    System.out.println("PDF file already exists, skipping copy: " + targetFileName);
                                                } else {
                                                    // Копируем файл (создаем новый)
                                                    Files.copy(pdfFile, targetPath, StandardCopyOption.REPLACE_EXISTING);
                                                }

                                                // Обновляем путь к PDF файлу в базе данных
                                                book.setPdfPath(targetPath.toString());
                                                bookRepository.save(book);

                                                System.out.println("Matched and copied PDF for book: " + book.getTitle() + " -> " + targetFileName);
                                                matchedCount.incrementAndGet();
                                            } catch (IOException e) {
                                                System.err.println("Failed to copy PDF for book " + book.getTitle() + ": " + e.getMessage());
                                            }
                                            break; // Нашли соответствие, переходим к следующему файлу
                                        }
                                    }
                                    processedCount.incrementAndGet();
                                } catch (Exception e) {
                                    System.err.println("Error processing PDF file " + pdfFile.getFileName() + ": " + e.getMessage());
                                }
                            });
                } catch (IOException e) {
                    System.err.println("Error listing files in PDF directory: " + e.getMessage());
                }
            }

            System.out.println("PDF processing completed: " + processedCount.get() + " files processed, " + matchedCount.get() + " PDFs matched and copied.");
        } catch (Exception e) {
            System.err.println("Error during PDF processing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addFunnyReviews() {
        // Получаем или создаем тестовых пользователей для отзывов
        User reviewer1 = getOrCreateUser("funny_reviewer_1", "funny1@example.com");
        User reviewer2 = getOrCreateUser("funny_reviewer_2", "funny2@example.com");
        User reviewer3 = getOrCreateUser("funny_reviewer_3", "funny3@example.com");

        List<User> reviewers = List.of(reviewer1, reviewer2, reviewer3);

        // Список смешных отзывов
        String[] funnyReviews = {
                "Прочитал за один присест! Правда, пришлось три раза перезагружать компьютер, но оно того стоило!",
                "Купил книгу случайно, думал это про кулинарию. Теперь я Senior Developer! 10/10, рекомендую!",
                "Автор явно не тестировал код перед публикацией. У меня ничего не работает, но читать было весело!",
                "Книга отличная, но почему-то после прочтения мой код стал еще хуже. Возможно, я что-то не так понял?",
                "Прочитал на работе вместо выполнения задач. Босс недоволен, но я теперь знаю про dependency injection!",
                "Купил для подарка другу-программисту. Он плакал от смеха, а потом от отчаяния. Отличная книга!",
                "Книга помогла мне понять, что я вообще ничего не понимаю в программировании. Спасибо за просветление!",
                "Прочитал за выходные. Теперь у меня нет выходных, но зато есть понимание Spring Boot!",
                "Книга настолько хорошая, что я забыл поесть. И поспать. И выйти из дома. Помогите!",
                "Автор обещал, что после прочтения я стану гуру. Я стал гуру в чтении книг о программировании!",
                "Купил книгу, прочитал, ничего не понял, перечитал, снова ничего не понял. Купил еще одну книгу!",
                "Книга изменила мою жизнь! Теперь я не сплю по ночам, но не потому что читаю, а потому что дебажу код!",
                "Прочитал книгу и понял, что все эти годы я программировал неправильно. Теперь я программирую еще неправильнее!",
                "Книга отличная, но почему-то мой кот начал писать на Java после того, как я ее прочитал. Это нормально?",
                "Купил книгу для повышения квалификации. Теперь я квалифицированно не сплю по ночам!",
                "Прочитал книгу и решил переписать весь проект. Теперь у меня нет проекта, но есть опыт!",
                "Книга помогла мне понять, что я не один такой. Есть еще люди, которые не понимают, что они делают!",
                "Купил книгу случайно, открыл случайно, прочитал случайно. Теперь я случайный Senior Developer!",
                "Книга настолько информативная, что после прочтения мой мозг перезагрузился. Пришлось перечитать!",
                "Прочитал книгу и понял, что все мои проблемы были из-за того, что я не читал эту книгу раньше!",
                "Купил книгу по совету коллеги. Теперь я понимаю, почему он уволился!",
                "Прочитал книгу и начал видеть код во сне. Просыпаюсь и пишу на Java. Помогите!",
                "Книга отличная, но почему-то после прочтения мой компьютер начал сам себя обновлять. Это нормально?",
                "Купил книгу для изучения. Теперь я знаю, что не знаю ничего. Спасибо за честность!",
                "Прочитал книгу за один день. На следующий день забыл все. Перечитал. Забыл снова. Цикл бесконечен!",
                "Книга помогла мне понять, что мой код - это не баги, это фичи! Спасибо за вдохновение!",
                "Купил книгу случайно, прочитал случайно, понял случайно. Теперь я случайный архитектор!",
                "Прочитал книгу и решил стать программистом. Теперь я программист, который не умеет программировать!",
                "Книга настолько хорошая, что я купил еще 5 экземпляров. На всякий случай. И для друзей. И для кота.",
                "Прочитал книгу и понял, что все эти годы я использовал неправильный фреймворк. Теперь использую еще неправильнее!",
                "Купил книгу для повышения зарплаты. Зарплата не повысилась, но я теперь знаю про паттерны проектирования!",
                "Прочитал книгу и начал рефакторить весь код. Теперь у меня нет рабочего кода, но есть понимание!",
                "Книга отличная, но почему-то после прочтения мой IDE начал предлагать мне уволиться. Это нормально?",
                "Купил книгу по акции. Теперь понимаю, почему она была по акции!",
                "Прочитал книгу и понял, что мой код - это произведение искусства. Плохого искусства, но искусства!",
                "Книга помогла мне понять, что я не тупой, просто книга слишком умная для меня!",
                "Прочитал книгу и начал писать комментарии на русском. Теперь весь код на русском. Помогите!",
                "Купил книгу для изучения. Теперь я эксперт в чтении книг о программировании. Код все еще не работает!",
                "Прочитал книгу до конца и понял, что мне срочно нужен отпуск. Или новый мозг.",
                "Книга хорошая, но почему-то после неё все мои баги стали осознанными.",
                "Читал с блокнотом. Записал туда свои слёзы и stack trace.",
                "Книга настолько глубокая, что я утонул где-то на третьей главе.",
                "После прочтения начал уважать чужой код. Свой — по-прежнему ненавижу.",
                "Прочитал и решил: пора учиться дальше. Закрыл книгу. Открыл YouTube.",
                "Книга научила меня главному — всегда делай бэкап перед чтением.",
                "После этой книги мой код стал чище. Правда, проект больше не собирается.",
                "Автор обещал простые примеры. Они простые… если ты автор.",
                "Книга читается легко, а вот жизнь после неё — нет.",
                "Прочитал половину, понял всё. Прочитал вторую половину — понял, что ничего не понял.",
                "Книга помогла осознать, что кофе — это dependency, а сон — optional.",
                "После прочтения захотелось переписать код. И резюме. И жизнь.",
                "Книга хорошая, но я всё равно загуглил каждый пример.",
                "Прочитал книгу и начал разговаривать с компьютером. Он не отвечает, но слушает.",
                "Теперь я знаю, как надо писать код. Осталось понять, как так писать.",
                "Книга вдохновляет! Особенно вдохновляет закрыть IDE и пойти гулять.",
                "Прочитал книгу и понял, почему у нас в проекте всё так, как есть.",
                "Книга настолько честная, что мне стало немного больно.",
                "После прочтения начал писать тесты. Потом вспомнил, что дедлайн вчера.",
                "Автор явно страдал, пока писал эту книгу. Я страдал, пока читал. Мы квиты.",
                "Книга отличная, но мой мозг запросил перезагрузку.",
                "Прочитал книгу и стал умнее. Ненадолго, но приятно.",
                "После этой книги понял, что legacy — это состояние души.",
                "Книга читается быстро, если пропускать места, где ничего не понимаешь.",
                "Прочитал книгу и понял, что код — это временно, баги — навсегда.",
                "Книга помогла принять мой код таким, какой он есть. Ужасным, но родным.",
                "После прочтения захотелось удалить весь проект. Начал с README.",
                "Книга настолько мотивирует, что я открыл IDE. И тут же закрыл.",
                "Прочитал книгу и начал видеть архитектуру там, где её нет.",
                "Книга хорошая, но почему-то у меня повысилось количество TODO.",
                "Прочитал книгу и понял, что senior — это состояние усталости.",
                "Книга помогла мне осознать, что проблема не в коде. Проблема во мне.",
                "После прочтения начал писать код медленнее, но с чувством.",
                "Книга отличная, если вы любите боль, рефакторинг и самоанализ.",
                "Прочитал книгу и стал говорить фразу: «Это ожидаемое поведение».",
                "Книга вдохновила меня начать новый проект. Старый я так и не закончил.",
                "После книги понял, что главное — не сломать то, что и так не работает.",
                "Книга хорошая, но я всё равно делаю по-своему.",
                "Прочитал книгу. Теперь знаю, как не надо делать. Это уже прогресс!"
        };

        int reviewIndex = 0;
        int reviewsAdded = 0;
        Set<Long> booksWithRatings = new HashSet<>(); // Отслеживаем книги, для которых создали рейтинги

        // Добавляем отзывы к каждой второй книге (начиная с индекса 1: 1, 3, 5, 7...)
        for (int i = 1; i < savedBooks.size(); i += 2) {
            Book book = savedBooks.get(i);

            // Определяем количество отзывов (1 или 2)
            int numberOfReviews = random.nextInt(2) + 1; // 1 или 2

            for (int j = 0; j < numberOfReviews; j++) {
                // Выбираем случайного ревьюера
                User reviewer = reviewers.get(random.nextInt(reviewers.size()));

                // Проверяем, нет ли уже отзыва от этого пользователя
                if (reviewRepository.existsByBookIdAndUserId(book.getId(), reviewer.getId())) {
                    continue;
                }

                // Выбираем случайный отзыв
                String reviewText = funnyReviews[reviewIndex % funnyReviews.length];
                reviewIndex++;

                // Создаем отзыв
                Review review = Review.builder()
                        .book(book)
                        .user(reviewer)
                        .text(reviewText)
                        .build();
                reviewRepository.save(review);

                // Создаем оценку (от 3 до 10, чтобы было смешно)
                short ratingValue = (short) (random.nextInt(3) + 8);

                // Проверяем, нет ли уже оценки от этого пользователя
                if (!ratingRepository.existsByBookIdAndUserId(book.getId(), reviewer.getId())) {
                    Rating rating = Rating.builder()
                            .book(book)
                            .user(reviewer)
                            .value(ratingValue)
                            .build();
                    ratingRepository.save(rating);
                    booksWithRatings.add(book.getId()); // Запоминаем книгу с рейтингом
                }

                reviewsAdded++;
            }
        }

        // Пересчитываем рейтинги для всех книг, у которых были созданы рейтинги
        updateBookRatings(booksWithRatings);

        System.out.println("Funny reviews initialization completed: " + reviewsAdded + " reviews added.");
    }

    private void updateBookRatings(Set<Long> bookIds) {
        int updatedCount = 0;
        for (Long bookId : bookIds) {
            try {
                Book book = bookRepository.findById(bookId).orElse(null);
                if (book == null) {
                    continue;
                }

                Double avgRating = ratingRepository.calculateAverageRating(bookId);
                long count = ratingRepository.countByBookId(bookId);

                if (avgRating != null) {
                    book.setRatingAvg(BigDecimal.valueOf(avgRating)
                            .setScale(2, RoundingMode.HALF_UP));
                } else {
                    book.setRatingAvg(BigDecimal.ZERO);
                }
                book.setRatingCount((int) count);

                bookRepository.save(book);
                updatedCount++;
            } catch (Exception e) {
                System.err.println("Error updating rating for book id " + bookId + ": " + e.getMessage());
            }
        }
        System.out.println("Book ratings updated: " + updatedCount + " books.");
    }

    private User getOrCreateUser(String nickname, String email) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User user = User.builder()
                            .nickname(nickname)
                            .email(email)
                            .passwordHash(passwordEncoder.encode("password123"))
                            .role(Role.USER)
                            .isVerified(true)  // Тестовые пользователи создаются системой, email верифицирован по умолчанию
                            .build();
                    return userRepository.save(user);
                });
    }

    // Вспомогательный класс для хранения данных о книге
    private static class BookData {
        String title;
        String authorName;
        String description;
        Integer publishedYear;
        Genre genre;
        BigDecimal price;
        BigDecimal discountPercent;

        BookData(String title, String authorName, String description, Integer publishedYear, Genre genre) {
            this.title = title;
            this.authorName = authorName;
            this.description = description;
            this.publishedYear = publishedYear;
            this.genre = genre;
            this.price = null;
            this.discountPercent = null;
        }

        BookData(String title, String authorName, String description, Integer publishedYear, Genre genre, BigDecimal price, BigDecimal discountPercent) {
            this.title = title;
            this.authorName = authorName;
            this.description = description;
            this.publishedYear = publishedYear;
            this.genre = genre;
            this.price = price;
            this.discountPercent = discountPercent;
        }
    }
}


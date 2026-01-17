package online.ityura.springdigitallibrary.model;

public enum Genre {
    FICTION("Художественная литература"),
    NON_FICTION("Нехудожественная литература"),
    MYSTERY("Детектив"),
    THRILLER("Триллер"),
    ROMANCE("Романтика"),
    SCIENCE_FICTION("Научная фантастика"),
    FANTASY("Фэнтези"),
    HORROR("Ужасы"),
    HISTORICAL("Историческая литература"),
    BIOGRAPHY("Биография"),
    AUTOBIOGRAPHY("Автобиография"),
    MEMOIR("Мемуары"),
    PHILOSOPHY("Философия"),
    PSYCHOLOGY("Психология"),
    SELF_HELP("Саморазвитие"),
    BUSINESS("Бизнес"),
    TECHNOLOGY("Технологии"),
    SCIENCE("Наука"),
    EDUCATION("Образование"),
    COOKING("Кулинария"),
    TRAVEL("Путешествия"),
    POETRY("Поэзия"),
    DRAMA("Драма"),
    COMEDY("Комедия"),
    ADVENTURE("Приключения"),
    WESTERN("Вестерн"),
    YOUNG_ADULT("Молодежная литература"),
    CHILDREN("Детская литература"),
    PORNO("Порно"),
    FOR_NERDS("Для задротов"),
    FOR_PEOPLE_WITHOUT_PERSONAL_LIFE("Для людей без личной жизни");

    private final String displayName;

    Genre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}


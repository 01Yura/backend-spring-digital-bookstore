package online.ityura.springdigitallibrary.api;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class BaseApiTest {
    protected SoftAssertions softly;

    @BeforeEach
    public void initSoftAssertions() {
        this.softly = new SoftAssertions();
    }

    @AfterEach
    public void verifySoftAssertions() {
        this.softly.assertAll();
    }
}

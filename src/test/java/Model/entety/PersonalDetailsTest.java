package Model.entety;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class PersonalDetailsTest {

    @Test
    void ageYearsAt_typicalBirthday_returnsCompletedYears() {
        PersonalDetails p = new PersonalDetails("a", "b", LocalDate.of(1990, 6, 15), null);
        assertEquals(OptionalInt.of(36), p.ageYearsAt(LocalDate.of(2026, 6, 15)));
        assertEquals(OptionalInt.of(35), p.ageYearsAt(LocalDate.of(2026, 6, 14)));
        assertEquals(OptionalInt.of(37), p.ageYearsAt(LocalDate.of(2027, 6, 16)));
    }

    @Test
    void ageYearsAt_leapYearFeb29_handlesJavaPeriodRules() {
        PersonalDetails p = new PersonalDetails("a", "b", LocalDate.of(2004, 2, 29), null);
        assertEquals(OptionalInt.of(21), p.ageYearsAt(LocalDate.of(2026, 2, 28)));
    }

    @Test
    void ageYearsAt_nullDateOfBirth_isEmpty() {
        PersonalDetails p = new PersonalDetails();
        assertTrue(p.ageYearsAt(LocalDate.of(2026, 1, 1)).isEmpty());
    }

    @Test
    void ageYearsAt_futureDateOfBirth_isEmpty() {
        PersonalDetails p = new PersonalDetails("a", "b", LocalDate.of(2030, 1, 1), null);
        assertTrue(p.ageYearsAt(LocalDate.of(2026, 1, 1)).isEmpty());
    }

    @Test
    void ageYearsAt_nullAsOf_isEmpty() {
        PersonalDetails p = new PersonalDetails("a", "b", LocalDate.of(2000, 1, 1), null);
        assertTrue(p.ageYearsAt(null).isEmpty());
    }
}

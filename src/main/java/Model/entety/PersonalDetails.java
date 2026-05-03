package Model.entety;

import Model.enums.Gender;

import java.time.LocalDate;
import java.time.Period;
import java.util.OptionalInt;

public class PersonalDetails {
    private String firstName;
    private String lastName;
    /** Calendar date of birth; null when unknown (see {@link #ageYearsAt(LocalDate)}). */
    private LocalDate dateOfBirth;
    private Gender gender;

    public PersonalDetails() {
    }

    public PersonalDetails(String firstName, String lastName, LocalDate dateOfBirth, Gender gender) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    /**
     * Completed years between date of birth and {@code asOfDate} (exclusive of birthdays that fall after {@code asOfDate}).
     * Empty when DOB is unknown or falls strictly after {@code asOfDate}.
     */
    public OptionalInt ageYearsAt(LocalDate asOfDate) {
        if (dateOfBirth == null || asOfDate == null) {
            return OptionalInt.empty();
        }
        if (dateOfBirth.isAfter(asOfDate)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Period.between(dateOfBirth, asOfDate).getYears());
    }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
}

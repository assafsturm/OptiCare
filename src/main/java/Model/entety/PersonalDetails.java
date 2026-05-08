package Model.entety;

import java.time.LocalDate;
import java.time.Period;
import java.util.OptionalInt;

import Model.enums.Gender;

public class PersonalDetails { 
    private String firstName;
    private String lastName;
    // Calendar date of birth
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

    // returns the age of the person at the given date
    // OptionalInt is used to return the age or empty if the date of birth is unknown or falls strictly after the given date
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

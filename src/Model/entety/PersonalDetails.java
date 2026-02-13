package Model.entety;

import Model.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalDetails {
    private String firstName;
    private String lastName;
    private int age;
    private Gender gender;
}

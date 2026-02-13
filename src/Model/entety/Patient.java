package Model.entety;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    private String id; // ת"ז נשארת בחוץ כי היא המזהה של התיק

    // פרטיים אישיים + פרטים אפטומולוגים
    private PersonalDetails personalDetails;
    private ClinicalData clinicalData;
}
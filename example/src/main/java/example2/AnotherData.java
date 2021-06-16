package example2;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class AnotherData {

    private int wins;
    private double rating;
    private Map<String, String> complexStuff;

}


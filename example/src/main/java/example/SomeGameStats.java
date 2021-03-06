package example;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SomeGameStats {

	private int wins;
	private int games;
	private double rating;
	private List<InventoryStat> inventory;

}

package pk.mr.pasir_rozek_mateusz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BalanceDTO {

    private double totalIncome;
    private double totalExpense;
    private double balance;

}
import { useState, useCallback, type ReactNode } from "react";
import graphqlClient from "../../api/graphClient";
import { BalanceContext, type Balance } from "./BalanceContext";

interface UserBalanceResponse {
  userBalance: Balance;
}

export const BalanceProvider = ({ children }: { children: ReactNode }) => {
  const [balance, setBalance] = useState<Balance | null>(null);
  
  // Nasz nowy stan dodany w poprawnym miejscu (wewnątrz komponentu)
  const [refreshVersion, setRefreshVersion] = useState(0);

  const refreshBalance = useCallback(async (days: number | null) => {
    const query = `
      query($days: Float) {
        userBalance(days: $days) {
          totalIncome
          totalExpense
          balance
        }
      }
    `;
    try {
      const response = await graphqlClient<UserBalanceResponse>(query, { days });
      if (response.data && response.data.userBalance) {
        setBalance(response.data.userBalance);
        
        // Podbijamy wersję przy każdym udanym pobraniu
        setRefreshVersion((v) => v + 1); 
      }
    } catch (error) {
      console.error("Błąd pobierania bilansu:", error);
    }
  }, []);

  return (
    // Przekazujemy refreshVersion w dół do innych komponentów
    <BalanceContext.Provider value={{ balance, refreshBalance, refreshVersion }}>
      {children}
    </BalanceContext.Provider>
  );
};
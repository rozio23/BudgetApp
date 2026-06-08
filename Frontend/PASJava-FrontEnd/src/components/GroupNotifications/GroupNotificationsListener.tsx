import { useEffect } from "react";
import { toast } from "react-toastify";
import { useAuth } from "../../context/AuthContext";

interface GroupNotification {
  type: "GROUP_EXPENSE_ADDED";
  groupId: number | string;
  groupName: string;
  title: string;
  amount: number;
  userShare: number;
  createdByEmail: string;
  message: string;
}

const getWebSocketUrl = (token: string) => {
  // 1. Rozbijamy token na części składowe JWT (Header, Payload, Signature)
  const parts = token.split('.');

  // 2. Token JWT musi składać się dokładnie z 3 części
  if (parts.length !== 3) {
    throw new Error("Invalid token structure for WebSocket connection");
  }

  // 3. Prosty, bezpieczny Regex bez ryzyka backtrackingu (Base64URL)
  const safeRegex = /^[A-Za-z0-9-_=]+$/;

  // 4. Walidujemy czy każda sekcja zawiera wyłącznie dozwolone znaki
  const isEveryPartSafe = parts.every(part => safeRegex.test(part));

  if (!isEveryPartSafe) {
    throw new Error("Invalid token format for WebSocket connection");
  }

  const protocol = globalThis.location.protocol === "https:" ? "wss" : "ws";
  return `${protocol}://localhost:8080/ws/group-notifications?token=${encodeURIComponent(token)}`;
};

const GroupNotificationsListener = () => {
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    if (!isAuthenticated) return;

    const token = localStorage.getItem("accessToken");
    if (!token) return;

    // Deklarujemy zmienną socket na poziomie bloku useEffect, by mieć do niej dostęp w funkcji czyszczącej
    let socket: WebSocket | null = null;

    try {
      // Bezpieczne generowanie URL i otwarcie strumienia WebSocket
      const wsUrl = getWebSocketUrl(token);
      socket = new WebSocket(wsUrl);

      socket.onmessage = (event) => {
        try {
          const notification = JSON.parse(event.data) as GroupNotification;
          if (notification.type === "GROUP_EXPENSE_ADDED") {
            toast.info(notification.message);
            globalThis.dispatchEvent(new Event("refresh_group_data"));
          }
        } catch (error) {
          console.error("Nie udało się obsłużyć komunikatu grupowego:", error);
        }
      };

      socket.onerror = (error) => {
        console.error("Błąd połączenia WebSocket z komunikatami grupowymi:", error);
      };

    } catch (securityError) {
      // Przechwytujemy błąd walidacji danych wejściowych, zabezpieczając aplikację przed domniemanym atakiem
      console.error("WebSocket security validation failed:", securityError);
    }

    // Funkcja czyszcząca po odmontowaniu komponentu
    return () => {
      if (socket) {
        socket.close();
      }
    };
  }, [isAuthenticated]);

  return null;
};

export default GroupNotificationsListener;
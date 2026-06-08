import type { ReactNode } from "react";
import styles from "./ConfirmModal.module.scss";

interface ConfirmModalProps {
  visible: boolean;
  title?: string;
  message: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
}

const ConfirmModal = ({
  visible,
  title = "Potwierdź akcję",
  message,
  confirmLabel = "Potwierdź",
  cancelLabel = "Anuluj",
  onConfirm,
  onCancel,
}: ConfirmModalProps) => {
  if (!visible) return null;

  return (
    <div
      className={styles.modalOverlay}
      onClick={onCancel}
      role="button"
      tabIndex={-1}
      aria-label="Zamknij tło okna"
    >
      {/* Zamieniamy <div> na natywny tag <dialog> */}
      <dialog
        open /* Flaga open sprawia, że komponent jest widoczny */
        className={styles.modal}
        aria-labelledby="confirm-modal-title"
        /* Usunięcie onClick zapobiega błędowi "Non-interactive elements" */
        onKeyDown={(event) => event.stopPropagation()}
      >
        {/* Otaczamy zawartość wewnętrzną elementem wstrzymującym propagację kliknięć,
           ale bez dodawania interaktywnych listenerów bezpośrednio do tagu dialog
        */}
        <div onClick={(event) => event.stopPropagation()} role="presentation">
          <h3 id="confirm-modal-title">{title}</h3>
          <p>{message}</p>
          <div className={styles.actions}>
            <button type="button" className={styles.cancel} onClick={onCancel}>
              {cancelLabel}
            </button>
            <button type="button" className={styles.confirm} onClick={onConfirm}>
              {confirmLabel}
            </button>
          </div>
        </div>
      </dialog>
    </div>
  );
};

export default ConfirmModal;

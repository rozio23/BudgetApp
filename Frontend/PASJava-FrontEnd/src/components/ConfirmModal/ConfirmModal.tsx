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
    /* Zewnętrzny kontener (tło) staje się pełnoprawnym przyciskiem */
    <button
      type="button"
      className={styles.modalOverlay}
      onClick={onCancel}
      aria-label="Zamknij tło okna"
    >
      {/* Sekcja wewnętrzna okna - natywny <dialog> bez eventów */}
      <dialog
        open
        className={styles.modal}
        aria-labelledby="confirm-modal-title"
      >
        {/* Ten div zatrzymuje propagację kliknięć, żeby kliknięcie wewnątrz okna go nie zamknęło */}
        <div
          onClick={(event) => event.stopPropagation()}
          onKeyDown={(event) => event.stopPropagation()}
          role="presentation"
        >
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
    </button>
  );
};

export default ConfirmModal;

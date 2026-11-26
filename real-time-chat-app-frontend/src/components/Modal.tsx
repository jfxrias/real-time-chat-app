import { X } from "lucide-react";
import type { ReactNode } from "react";

export function Modal({ title, onClose, children, footer, size }: { title: string; onClose: () => void; children: ReactNode; footer?: ReactNode; size?: "lg" }) {
  return (
    <div className="uz-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className={`uz-modal ${size === "lg" ? "uz-modal-lg" : ""}`} role="dialog" aria-modal="true" aria-label={title}>
        <div className="uz-modal-header">
          <h5 className="m-0">{title}</h5>
          <button className="btn btn-sm uz-icon-btn" onClick={onClose} aria-label="Fechar"><X size={18} /></button>
        </div>
        <div className="uz-modal-body">{children}</div>
        {footer && <div className="uz-modal-footer">{footer}</div>}
      </div>
    </div>
  );
}

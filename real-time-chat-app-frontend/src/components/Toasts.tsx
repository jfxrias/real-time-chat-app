import { useEffect, useState } from "react";
import { CheckCircle2, Info, X, XCircle } from "lucide-react";
import { dismiss, subscribeToasts, type ToastItem } from "@/lib/toast";

export function Toasts() {
  const [items, setItems] = useState<ToastItem[]>([]);
  useEffect(() => subscribeToasts(setItems), []);
  return (
    <div className="uz-toasts" aria-live="polite">
      {items.map((t) => (
        <div key={t.id} className={`uz-toast uz-toast-${t.kind}`}>
          {t.kind === "success" ? <CheckCircle2 size={18} /> : t.kind === "error" ? <XCircle size={18} /> : <Info size={18} />}
          <span>{t.text}</span>
          <button className="uz-toast-close" onClick={() => dismiss(t.id)} aria-label="Fechar"><X size={14} /></button>
        </div>
      ))}
    </div>
  );
}

export type ToastKind = "success" | "error" | "info";
export interface ToastItem { id: number; kind: ToastKind; text: string }
type L = (t: ToastItem[]) => void;
let items: ToastItem[] = []; let seq = 0; const ls = new Set<L>();
const emit = () => ls.forEach((l) => l(items));
export function toast(text: string, kind: ToastKind = "info") {
  const id = ++seq; items = [...items, { id, kind, text }]; emit();
  setTimeout(() => dismiss(id), 4500);
}
export function dismiss(id: number) { items = items.filter((t) => t.id !== id); emit(); }
export function subscribeToasts(l: L) { ls.add(l); l(items); return () => { ls.delete(l); }; }

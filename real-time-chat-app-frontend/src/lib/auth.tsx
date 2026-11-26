import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import { api, clearSession, TOKEN_KEY, USER_KEY } from "@/services/api";
import type { User } from "@/services/types";

interface Ctx { ready: boolean; token: string | null; user: User | null; login: (token: string, user: User) => void; logout: () => void; setUser: (u: User) => void }
const AuthCtx = createContext<Ctx | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);
  const [token, setToken] = useState<string | null>(null);
  const [user, setUserState] = useState<User | null>(null);
  useEffect(() => {
    if (typeof window === "undefined") return;
    const t = localStorage.getItem(TOKEN_KEY);
    const u = localStorage.getItem(USER_KEY);
    setToken(t);
    try { setUserState(u ? JSON.parse(u) : null); } catch { setUserState(null); }
    setReady(true);
    if (t) api.me().then((me) => { setUserState(me); localStorage.setItem(USER_KEY, JSON.stringify(me)); }).catch(() => {});
  }, []);
  const setUser = useCallback((u: User) => { setUserState(u); if (typeof window !== "undefined") localStorage.setItem(USER_KEY, JSON.stringify(u)); }, []);
  const login = useCallback((t: string, u: User) => { if (typeof window !== "undefined") localStorage.setItem(TOKEN_KEY, t); setToken(t); setUser(u); }, [setUser]);
  const logout = useCallback(() => { clearSession(); setToken(null); setUserState(null); }, []);
  return <AuthCtx.Provider value={{ ready, token, user, login, logout, setUser }}>{children}</AuthCtx.Provider>;
}
export function useAuth() { const c = useContext(AuthCtx); if (!c) throw new Error("AuthProvider ausente"); return c; }

export const onlyDigits = (s: string) => s.replace(/\D/g, "").slice(0, 11);
export function maskCpf(s: string) {
  const d = onlyDigits(s);
  return d.replace(/(\d{3})(\d)/, "$1.$2").replace(/(\d{3})(\d)/, "$1.$2").replace(/(\d{3})(\d{1,2})$/, "$1-$2");
}
export const initials = (n?: string) => (n ?? "?").trim().split(/\s+/).slice(0, 2).map((p) => p[0]?.toUpperCase()).join("") || "?";
export const fmtTime = (iso?: string) => { if (!iso) return ""; const d = new Date(iso); return isNaN(d.getTime()) ? "" : d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" }); };
export const fmtDate = (iso?: string) => { if (!iso) return "—"; const d = new Date(iso); return isNaN(d.getTime()) ? iso : d.toLocaleString("pt-BR"); };

import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useEffect, useState, type FormEvent } from "react";
import { maskCpf, onlyDigits, useAuth } from "@/lib/auth";
import { toast } from "@/lib/toast";
import { api } from "@/services/api";
import sofismarIcon from "../img/favLogoSemFundo.png";

export const Route = createFileRoute("/login")({
  head: () => ({ meta: [{ title: "Entrar — sofismar.app" }, { name: "description", content: "Entre no sofismar.app com seu CPF e senha." }, { property: "og:title", content: "Entrar — sofismar.app" }, { property: "og:description", content: "Entre no sofismar.app com seu CPF e senha." }, { property: "og:type", content: "website" }, { name: "twitter:card", content: "summary" }] }),
  component: LoginPage,
});

function LoginPage() {
  const { login, token, ready } = useAuth();
  const nav = useNavigate();
  const [cpf, setCpf] = useState(""); const [password, setPassword] = useState(""); const [busy, setBusy] = useState(false);
  useEffect(() => { if (ready && token) void nav({ to: "/" }); }, [ready, token, nav]);
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (onlyDigits(cpf).length !== 11) return toast("Informe um CPF com 11 dígitos.", "error");
    setBusy(true);
    try { const r = await api.login({ cpf: onlyDigits(cpf), password }); login(r.token, r.user); toast(`Bem-vindo(a), ${r.user.name}!`, "success"); void nav({ to: "/" }); }
    catch { /* toast já exibido */ } finally { setBusy(false); }
  };
  return (
    <div className="uz-auth">
      <form className="uz-auth-card" onSubmit={submit}>
        <div className="uz-auth-brand"><img className="sofismar-logo-icon" src={sofismarIcon} alt="" /><span className="sofismar-brand fs-3">sofismar.app</span></div>
        <p className="text-secondary mb-4">Entre para continuar suas conversas.</p>
        <div className="mb-3"><label className="form-label" htmlFor="cpf">CPF</label>
          <input id="cpf" className="form-control" inputMode="numeric" placeholder="000.000.000-00" value={maskCpf(cpf)} onChange={(e) => setCpf(onlyDigits(e.target.value))} required /></div>
        <div className="mb-4"><label className="form-label" htmlFor="pw">Senha</label>
          <input id="pw" type="password" className="form-control" value={password} onChange={(e) => setPassword(e.target.value)} required /></div>
        <button className="btn btn-primary w-100 py-2" disabled={busy}>{busy ? "Entrando..." : "Entrar"}</button>
        <p className="text-center mt-3 mb-0 small text-secondary">Não tem conta? <Link to="/register">Cadastre-se</Link></p>
      </form>
    </div>
  );
}

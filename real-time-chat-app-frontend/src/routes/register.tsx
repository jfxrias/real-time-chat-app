import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { MessageCircle } from "lucide-react";
import { useState, type FormEvent } from "react";
import { maskCpf, onlyDigits } from "@/lib/auth";
import { toast } from "@/lib/toast";
import { api } from "@/services/api";

export const Route = createFileRoute("/register")({
  head: () => ({ meta: [{ title: "Criar conta — UaiZap" }, { name: "description", content: "Crie sua conta no UaiZap." }, { property: "og:title", content: "Criar conta — UaiZap" }, { property: "og:description", content: "Crie sua conta no UaiZap." }, { property: "og:type", content: "website" }, { name: "twitter:card", content: "summary" }] }),
  component: RegisterPage,
});

function RegisterPage() {
  const nav = useNavigate();
  const [name, setName] = useState(""); const [cpf, setCpf] = useState(""); const [password, setPassword] = useState(""); const [isAdmin, setIsAdmin] = useState(false); const [busy, setBusy] = useState(false);
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (onlyDigits(cpf).length !== 11) return toast("Informe um CPF com 11 dígitos.", "error");
    if (password.length < 6) return toast("A senha deve ter no mínimo 6 caracteres.", "error");
    setBusy(true);
    try { await api.register({ name: name.trim(), cpf: onlyDigits(cpf), password, isAdmin }); toast("Conta criada! Faça login.", "success"); void nav({ to: "/login" }); }
    catch { /* toast */ } finally { setBusy(false); }
  };
  return (
    <div className="uz-auth">
      <form className="uz-auth-card" onSubmit={submit}>
        <div className="uz-auth-brand"><span className="mark"><MessageCircle size={24} /></span><span className="uz-logo">Uai<span>Zap</span></span></div>
        <p className="text-secondary mb-4">Crie sua conta em segundos.</p>
        <div className="mb-3"><label className="form-label" htmlFor="nm">Nome</label><input id="nm" className="form-control" value={name} onChange={(e) => setName(e.target.value)} required /></div>
        <div className="mb-3"><label className="form-label" htmlFor="cpf">CPF</label><input id="cpf" className="form-control" inputMode="numeric" placeholder="000.000.000-00" value={maskCpf(cpf)} onChange={(e) => setCpf(onlyDigits(e.target.value))} required /></div>
        <div className="mb-3"><label className="form-label" htmlFor="pw">Senha</label><input id="pw" type="password" className="form-control" minLength={6} value={password} onChange={(e) => setPassword(e.target.value)} required /><div className="form-text">Mínimo de 6 caracteres.</div></div>
        <div className="form-check mb-4"><input id="adm" type="checkbox" className="form-check-input" checked={isAdmin} onChange={(e) => setIsAdmin(e.target.checked)} /><label htmlFor="adm" className="form-check-label">Conta de Administrador</label></div>
        <button className="btn btn-primary w-100 py-2" disabled={busy}>{busy ? "Criando..." : "Criar conta"}</button>
        <p className="text-center mt-3 mb-0 small text-secondary">Já tem conta? <Link to="/login">Entrar</Link></p>
      </form>
    </div>
  );
}

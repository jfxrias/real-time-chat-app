import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { ArrowLeft, BarChart3, Flag, Users, UsersRound } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { fmtDate, useAuth } from "@/lib/auth";
import { toast } from "@/lib/toast";
import { api } from "@/services/api";
import type { Group, Report, User } from "@/services/types";

export const Route = createFileRoute("/admin")({
  head: () => ({ meta: [{ title: "Painel Admin — UaiZap" }, { name: "description", content: "Administração de usuários, grupos e denúncias do UaiZap." }, { property: "og:title", content: "Painel Admin — UaiZap" }, { property: "og:description", content: "Administração do UaiZap." }, { property: "og:type", content: "website" }, { name: "twitter:card", content: "summary" }] }),
  component: AdminPage,
});

type Tab = "users" | "groups" | "reports" | "analytics";

function AdminPage() {
  const { ready, token, user } = useAuth();
  const nav = useNavigate();
  const [tab, setTab] = useState<Tab>("users");
  const [users, setUsers] = useState<User[]>([]); const [groups, setGroups] = useState<Group[]>([]); const [reports, setReports] = useState<Report[]>([]);
  const [loading, setLoading] = useState(false);
  useEffect(() => { if (ready && !token) void nav({ to: "/login" }); else if (ready && user && !user.isAdmin) void nav({ to: "/" }); }, [ready, token, user, nav]);
  const load = useCallback(async (t: Tab) => {
    setLoading(true);
    try {
      if (t === "users") setUsers(await api.adminUsers());
      if (t === "groups") setGroups(await api.adminGroups());
      if (t === "reports") setReports([...(await api.reports())].sort((a, b) => +new Date(b.reportedAt) - +new Date(a.reportedAt)));
    } catch { /* toast */ } finally { setLoading(false); }
  }, []);
  useEffect(() => { if (user?.isAdmin && tab !== "analytics") void load(tab); }, [tab, user?.isAdmin, load]);
  const nameOf = (id: string) => users.find((u) => u.userUuid === id)?.name;
  useEffect(() => { if (tab === "reports" && user?.isAdmin && users.length === 0) api.adminUsers().then(setUsers).catch(() => {}); if (tab === "reports" && user?.isAdmin && groups.length === 0) api.adminGroups().then(setGroups).catch(() => {}); }, [tab]);
  const groupName = (id: string) => groups.find((g) => g.groupUuid === id)?.groupName;

  if (!ready || !user?.isAdmin) return <div className="uz-center">Carregando...</div>;
  const toggleUser = async (u: User) => { try { await api.setUserActive(u.userUuid, !u.isActive); toast(u.isActive ? "Usuário desativado." : "Usuário reativado.", "success"); void load("users"); } catch { /* */ } };
  const toggleGroup = async (g: Group) => { const active = g.status === "ACTIVE"; try { await api.setGroupActive(g.groupUuid, !active); toast(active ? "Grupo desativado." : "Grupo reativado.", "success"); void load("groups"); } catch { /* */ } };

  const tabs: [Tab, string, typeof Users][] = [["users", "Usuários", Users], ["groups", "Grupos", UsersRound], ["reports", "Denúncias", Flag], ["analytics", "Analytics", BarChart3]];
  return (
    <div className="uz-admin">
      <div className="uz-admin-top"><Link to="/" className="uz-icon-btn" aria-label="Voltar"><ArrowLeft size={20} /></Link><span className="uz-logo fs-5">Uai<span>Zap</span></span><span className="text-secondary">/ Painel Administrativo</span></div>
      <div className="uz-admin-body">
        <div className="uz-tabs">{tabs.map(([k, l, I]) => <button key={k} className={`uz-tab ${tab === k ? "active" : ""}`} onClick={() => setTab(k)}><I size={16} />{l}</button>)}</div>
        {loading && <div className="text-secondary small mb-2">Carregando...</div>}
        {tab === "users" && <div className="uz-card"><div className="table-responsive"><table className="table table-hover"><thead><tr><th>Nome</th><th>CPF</th><th>Perfil</th><th>Status</th><th className="text-end">Ação</th></tr></thead><tbody>
          {users.map((u) => <tr key={u.userUuid}><td>{u.name}<div className="uz-mono">{u.userUuid}</div></td><td>{u.cpf}</td><td>{u.isAdmin ? <span className="badge text-bg-info">Admin</span> : <span className="badge text-bg-secondary">Usuário</span>}</td><td>{u.isActive ? <span className="badge text-bg-success">Ativo</span> : <span className="badge text-bg-danger">Inativo</span>}</td>
            <td className="text-end"><button className={`btn btn-sm ${u.isActive ? "btn-outline-danger" : "btn-outline-success"}`} onClick={() => toggleUser(u)} disabled={u.userUuid === user.userUuid}>{u.isActive ? "Desativar" : "Reativar"}</button></td></tr>)}
          {!loading && users.length === 0 && <tr><td colSpan={5} className="uz-empty">Nenhum usuário.</td></tr>}</tbody></table></div></div>}
        {tab === "groups" && <div className="uz-card"><div className="table-responsive"><table className="table table-hover"><thead><tr><th>Grupo</th><th>Criador</th><th>Criado em</th><th>Status</th><th className="text-end">Ação</th></tr></thead><tbody>
          {groups.map((g) => { const a = g.status === "ACTIVE"; return <tr key={g.groupUuid}><td>{g.groupName}<div className="uz-mono">{g.groupUuid}</div></td><td className="uz-mono">{g.creatorUuid}</td><td>{fmtDate(g.createdAt)}</td><td><span className={`badge ${a ? "text-bg-success" : "text-bg-danger"}`}>{g.status}</span></td>
            <td className="text-end"><button className={`btn btn-sm ${a ? "btn-outline-danger" : "btn-outline-success"}`} onClick={() => toggleGroup(g)}>{a ? "Desativar" : "Reativar"}</button></td></tr>; })}
          {!loading && groups.length === 0 && <tr><td colSpan={5} className="uz-empty">Nenhum grupo.</td></tr>}</tbody></table></div></div>}
        {tab === "reports" && <div className="uz-card"><div className="table-responsive"><table className="table table-hover"><thead><tr><th>Data</th><th>Motivo</th><th>Denunciante</th><th>Denunciado</th><th>Grupo</th></tr></thead><tbody>
          {reports.map((r) => <tr key={r.reportUuid}><td className="text-nowrap">{fmtDate(r.reportedAt)}</td><td style={{ maxWidth: 360 }}>{r.reason}</td><td>{nameOf(r.reporterUuid) ?? <span className="uz-mono">{r.reporterUuid}</span>}</td><td>{nameOf(r.reportedUuid) ?? <span className="uz-mono">{r.reportedUuid}</span>}</td><td>{groupName(r.groupUuid) ?? <span className="uz-mono">{r.groupUuid}</span>}</td></tr>)}
          {!loading && reports.length === 0 && <tr><td colSpan={5} className="uz-empty">Nenhuma denúncia registrada.</td></tr>}</tbody></table></div></div>}
        {tab === "analytics" && <div className="uz-card uz-analytics-empty"><div className="ico"><BarChart3 size={34} /></div><h5 className="text-light">Em breve</h5><p className="mb-0">Integração dos endpoints analíticos.</p></div>}
      </div>
    </div>
  );
}

import { Crown, LogOut, Pencil, Shield, Trash2, UserMinus, UserPlus, Flag } from "lucide-react";
import { useEffect, useId, useState } from "react";
import { Modal } from "@/components/Modal";
import { initials, useAuth } from "@/lib/auth";
import { toast } from "@/lib/toast";
import { api } from "@/services/api";
import type { Group, GroupMember, User } from "@/services/types";

export function ProfileModal({ onClose, onLogout }: { onClose: () => void; onLogout: () => void }) {
  const { user, setUser } = useAuth();
  const [name, setName] = useState(user?.name ?? ""); const [pw, setPw] = useState(""); const [busy, setBusy] = useState(false);
  const saveName = async () => { if (!name.trim()) return; setBusy(true); try { setUser(await api.updateName(name.trim())); toast("Nome atualizado.", "success"); } catch { /* */ } finally { setBusy(false); } };
  const savePw = async () => { if (pw.length < 6) return toast("A senha deve ter no mínimo 6 caracteres.", "error"); setBusy(true); try { const r = await api.updatePassword(pw); toast(r?.message || "Senha alterada.", "success"); setPw(""); } catch { /* */ } finally { setBusy(false); } };
  return (
    <Modal title="Meu perfil" onClose={onClose} footer={<button className="btn btn-outline-danger me-auto d-flex align-items-center gap-2" onClick={onLogout}><LogOut size={16} />Sair</button>}>
      <div className="d-flex align-items-center gap-3 mb-3"><span className="uz-avatar" style={{ width: 56, height: 56 }}>{initials(user?.name)}</span><div><strong>{user?.name}</strong><div className="small text-secondary">CPF {user?.cpf}{user?.isAdmin ? " · Administrador" : ""}</div></div></div>
      <div className="uz-section-title">Nome</div>
      <div className="input-group"><input className="form-control" value={name} onChange={(e) => setName(e.target.value)} /><button className="btn btn-primary" disabled={busy} onClick={saveName}>Salvar</button></div>
      <div className="uz-section-title">Nova senha</div>
      <div className="input-group"><input type="password" className="form-control" placeholder="Mínimo 6 caracteres" value={pw} onChange={(e) => setPw(e.target.value)} /><button className="btn btn-primary" disabled={busy} onClick={savePw}>Alterar</button></div>
    </Modal>
  );
}

export function NewGroupModal({ onClose, onCreated }: { onClose: () => void; onCreated: (g: Group) => void }) {
  const { user } = useAuth(); const [name, setName] = useState(""); const [busy, setBusy] = useState(false);
  const create = async () => { if (!name.trim() || !user) return; setBusy(true); try { const g = await api.createGroup(name.trim(), user.userUuid); toast("Grupo criado!", "success"); onCreated(g); } catch { /* */ } finally { setBusy(false); } };
  return <Modal title="Novo grupo" onClose={onClose} footer={<><button className="btn btn-secondary" onClick={onClose}>Cancelar</button><button className="btn btn-primary" disabled={busy || !name.trim()} onClick={create}>Criar</button></>}>
    <label className="form-label small text-secondary">Nome do grupo</label>
    <input autoFocus className="form-control" value={name} onChange={(e) => setName(e.target.value)} onKeyDown={(e) => e.key === "Enter" && create()} />
  </Modal>;
}

export function BroadcastModal({ groups, onClose }: { groups: Group[]; onClose: () => void }) {
  const [sel, setSel] = useState<string[]>([]); const [content, setContent] = useState(""); const [busy, setBusy] = useState(false);
  const active = groups.filter((g) => g.status !== "INACTIVE");
  const send = async () => { setBusy(true); try { const r = await api.sendGroups(sel, content.trim()); toast(r?.message || `Enviado para ${sel.length} grupo(s).`, "success"); onClose(); } catch { /* */ } finally { setBusy(false); } };
  return <Modal title="Enviar para vários grupos" onClose={onClose} footer={<><button className="btn btn-secondary" onClick={onClose}>Cancelar</button><button className="btn btn-primary" disabled={busy || !sel.length || !content.trim()} onClick={send}>Enviar ({sel.length})</button></>}>
    <div className="uz-section-title mt-0">Grupos</div>
    {active.length === 0 && <div className="text-secondary small">Você não participa de nenhum grupo ativo.</div>}
    <div style={{ maxHeight: 220, overflowY: "auto" }}>{active.map((g) => <div className="form-check" key={g.groupUuid}><input id={`b-${g.groupUuid}`} type="checkbox" className="form-check-input" checked={sel.includes(g.groupUuid)} onChange={(e) => setSel((s) => e.target.checked ? [...s, g.groupUuid] : s.filter((x) => x !== g.groupUuid))} /><label htmlFor={`b-${g.groupUuid}`} className="form-check-label">{g.groupName}</label></div>)}</div>
    <div className="uz-section-title">Mensagem</div>
    <textarea className="form-control" rows={3} value={content} onChange={(e) => setContent(e.target.value)} />
  </Modal>;
}

const REPORT_REASONS = [
  "Spam ou flood",
  "Assédio ou bullying",
  "Discurso de ódio",
  "Conteúdo impróprio ou ofensivo",
  "Golpe ou fraude",
  "Falsidade ideológica",
  "Outro",
] as const;

export function ReportModal({ member, groupUuid, onClose }: { member: { userUuid: string; name: string }; groupUuid: string; onClose: () => void }) {
  const id = useId(); const [reason, setReason] = useState(""); const [details, setDetails] = useState(""); const [busy, setBusy] = useState(false);
  const finalReason = reason === "Outro" ? details.trim() : reason;
  const send = async () => { if (!finalReason || busy) return; setBusy(true); try { const r = await api.report({ reportedUuid: member.userUuid, groupUuid, reason: finalReason }); toast(r?.message || "Denúncia enviada.", "success"); onClose(); } catch { /* */ } finally { setBusy(false); } };
  return <Modal title={`Denunciar ${member.name}`} onClose={onClose} footer={<><button className="btn btn-secondary" onClick={onClose}>Cancelar</button><button className="btn btn-danger" disabled={busy || !finalReason} onClick={send}>Enviar denúncia</button></>}>
    <div className="uz-section-title mt-0">Motivo</div>
    <div className="d-flex flex-column gap-1">{REPORT_REASONS.map((option, i) => <div className="form-check" key={option}><input className="form-check-input" type="radio" name={`report-${id}`} id={`report-${id}-${i}`} checked={reason === option} onChange={() => setReason(option)} /><label className="form-check-label" htmlFor={`report-${id}-${i}`}>{option}</label></div>)}</div>
    {reason === "Outro" && <div className="mt-3"><label className="form-label small text-secondary" htmlFor={`report-details-${id}`}>Descreva o motivo</label><textarea id={`report-details-${id}`} autoFocus className="form-control" rows={4} maxLength={255} placeholder="Descreva o motivo da denúncia" value={details} onChange={(e) => setDetails(e.target.value.slice(0, 255))} /><div className={`text-end small mt-1 ${details.length >= 240 ? "text-warning" : "text-secondary"}`}>{details.length}/255</div></div>}
  </Modal>;
}

export function GroupDetailsModal({ group, users, onClose, onChanged, onDeleted }: { group: Group; users: User[]; onClose: () => void; onChanged: (g: Group) => void; onDeleted: (id: string) => void }) {
  const { user } = useAuth();
  const [members, setMembers] = useState<GroupMember[]>([]); const [name, setName] = useState(group.groupName);
  const [addUuid, setAddUuid] = useState(""); const [role, setRole] = useState<"MEMBER" | "ADMIN">("MEMBER");
  const [reporting, setReporting] = useState<{ userUuid: string; name: string } | null>(null); const [confirmDel, setConfirmDel] = useState(false);
  const load = () => api.members(group.groupUuid).then(setMembers).catch(() => {});
  useEffect(() => { void load(); }, [group.groupUuid]);
  const isCreator = user?.userUuid === group.creatorUuid;
  const myRole = members.find((m) => m.userUuid === user?.userUuid)?.role;
  const canManage = isCreator || myRole === "ADMIN";
  const nameOf = (m: GroupMember) => m.name || users.find((u) => u.userUuid === m.userUuid)?.name || m.userUuid;
  const candidates = users.filter((u) => !members.some((m) => m.userUuid === u.userUuid));
  const rename = async () => { if (!name.trim()) return; try { const g = await api.renameGroup(group.groupUuid, name.trim()); toast("Grupo renomeado.", "success"); onChanged({ ...group, ...(g ?? {}), groupName: g?.groupName ?? name.trim() }); } catch { /* */ } };
  const add = async () => { if (!addUuid) return; try { const r = await api.addMember(group.groupUuid, addUuid, role); toast(r?.message || "Membro adicionado.", "success"); setAddUuid(""); void load(); } catch { /* */ } };
  const remove = async (id: string) => { try { const r = await api.removeMember(group.groupUuid, id); toast(r?.message || "Membro removido.", "success"); void load(); } catch { /* */ } };
  const del = async () => { if (!user) return; try { const r = await api.deleteGroup(group.groupUuid, user.userUuid); toast(r?.message || "Grupo excluído.", "success"); onDeleted(group.groupUuid); } catch { /* */ } };
  return (
    <>
      <Modal title="Detalhes do grupo" size="lg" onClose={onClose}>
        <div className="d-flex align-items-center gap-3 mb-2"><span className="uz-avatar group" style={{ width: 56, height: 56 }}>{initials(group.groupName)}</span><div><strong className="fs-5">{group.groupName}</strong><div><span className={`badge ${group.status === "INACTIVE" ? "text-bg-danger" : "text-bg-success"}`}>{group.status}</span></div></div></div>
        {isCreator && <><div className="uz-section-title">Renomear</div><div className="input-group"><input className="form-control" value={name} onChange={(e) => setName(e.target.value)} /><button className="btn btn-primary d-flex align-items-center gap-1" onClick={rename}><Pencil size={14} />Salvar</button></div></>}
        {canManage && <><div className="uz-section-title">Adicionar membro</div><div className="input-group">
          <select className="form-select" value={addUuid} onChange={(e) => setAddUuid(e.target.value)}><option value="">Selecione um usuário...</option>{candidates.map((u) => <option key={u.userUuid} value={u.userUuid}>{u.name}</option>)}</select>
          <select className="form-select" style={{ maxWidth: 130 }} value={role} onChange={(e) => setRole(e.target.value as "MEMBER" | "ADMIN")}><option value="MEMBER">MEMBER</option><option value="ADMIN">ADMIN</option></select>
          <button className="btn btn-primary" disabled={!addUuid} onClick={add}><UserPlus size={16} /></button></div></>}
        <div className="uz-section-title">Membros ({members.length})</div>
        <div className="list-group">{members.map((m) => (
          <div key={m.userUuid} className="list-group-item d-flex align-items-center gap-2">
            <span className="uz-avatar sm">{initials(nameOf(m))}</span>
            <span className="flex-grow-1">{nameOf(m)}{m.userUuid === user?.userUuid && <span className="text-secondary"> (você)</span>}</span>
            {m.userUuid === group.creatorUuid ? <span className="badge text-bg-warning d-flex gap-1 align-items-center"><Crown size={12} />Criador</span> : m.role === "ADMIN" ? <span className="badge text-bg-info d-flex gap-1 align-items-center"><Shield size={12} />ADMIN</span> : <span className="badge text-bg-secondary">MEMBER</span>}
            {m.userUuid !== user?.userUuid && <button className="uz-icon-btn" title="Denunciar membro" onClick={() => setReporting({ userUuid: m.userUuid, name: nameOf(m) })}><Flag size={16} /></button>}
            {canManage && m.userUuid !== group.creatorUuid && m.userUuid !== user?.userUuid && <button className="uz-icon-btn" title="Remover" onClick={() => remove(m.userUuid)}><UserMinus size={16} /></button>}
          </div>))}</div>
        {isCreator && <div className="mt-4">{confirmDel ? <div className="alert alert-danger d-flex align-items-center gap-2 mb-0"><span className="flex-grow-1">Excluir este grupo definitivamente?</span><button className="btn btn-sm btn-secondary" onClick={() => setConfirmDel(false)}>Cancelar</button><button className="btn btn-sm btn-danger" onClick={del}>Excluir</button></div>
          : <button className="btn btn-outline-danger d-flex align-items-center gap-2" onClick={() => setConfirmDel(true)}><Trash2 size={16} />Excluir grupo</button>}</div>}
      </Modal>
      {reporting && <ReportModal member={reporting} groupUuid={group.groupUuid} onClose={() => setReporting(null)} />}
    </>
  );
}

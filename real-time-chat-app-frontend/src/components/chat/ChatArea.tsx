import { ArrowLeft, Check, Info, MoreVertical, Pencil, SendHorizontal, Trash2, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { fmtTime, initials, useAuth } from "@/lib/auth";
import { api } from "@/services/api";
import type { ChatMessage, Conversation, User } from "@/services/types";

interface Props { conv: Conversation; messages: ChatMessage[]; loading: boolean; users: User[]; onSent: (m: ChatMessage) => void; onEdited: (id: string, content: string, editedAt: string) => void; onRemoved: (id: string) => void; onBack: () => void; onDetails: () => void }

export function ChatArea({ conv, messages, loading, users, onSent, onEdited, onRemoved, onBack, onDetails }: Props) {
  const { user } = useAuth();
  const [text, setText] = useState(""); const [sending, setSending] = useState(false);
  const [menu, setMenu] = useState<string | null>(null);
  const [editing, setEditing] = useState<{ id: string; content: string } | null>(null);
  const endRef = useRef<HTMLDivElement>(null);
  useEffect(() => { endRef.current?.scrollIntoView({ behavior: "smooth" }); }, [messages.length, conv.id]);
  useEffect(() => { setText(""); setEditing(null); setMenu(null); }, [conv.id]);
  const nameOf = (id: string) => users.find((u) => u.userUuid === id)?.name ?? "Usuário";

  const send = async () => {
    const c = text.trim(); if (!c || sending) return;
    setSending(true);
    try {
      if (conv.type === "private") onSent(await api.sendPrivate(conv.id, c));
      else { const r = await api.sendGroups([conv.id], c); r?.deliveries?.forEach((d) => onSent({ ...d, targetGroupUuid: d.targetGroupUuid ?? conv.id })); }
      setText("");
    } catch { /* toast */ } finally { setSending(false); }
  };
  const saveEdit = async () => { if (!editing?.content.trim()) return; try { const r = await api.editMessage(editing.id, editing.content.trim()); onEdited(editing.id, r?.content ?? editing.content.trim(), r?.editedAt ?? new Date().toISOString()); setEditing(null); } catch { /* */ } };
  const delForMe = async (id: string) => { setMenu(null); try { await api.deleteForMe(id); onRemoved(id); } catch { /* */ } };
  const isSelf = conv.type === "private" && conv.id === user?.userUuid;

  return (
    <div className="uz-main" onClick={() => setMenu(null)}>
      <div className="uz-chat-header">
        <button className="uz-icon-btn uz-back" onClick={onBack} aria-label="Voltar"><ArrowLeft size={20} /></button>
        <span className={`uz-avatar ${conv.type === "group" ? "group" : ""}`}>{initials(conv.name)}</span>
        <div className="title"><strong className="d-block text-truncate">{isSelf ? "Mensagens Salvas" : conv.name}</strong><small>{conv.type === "group" ? "Grupo · toque em detalhes para ver membros" : isSelf ? "Anotações para você mesmo" : "Conversa privada"}</small></div>
        {conv.type === "group" && <button className="uz-icon-btn" onClick={onDetails} title="Detalhes do grupo"><Info size={20} /></button>}
      </div>
      <div className="uz-messages">
        {loading && <div className="uz-empty">Carregando mensagens...</div>}
        {!loading && messages.length === 0 && <div className="uz-empty">Nenhuma mensagem ainda. Diga olá! 👋</div>}
        {messages.map((m) => {
          const mine = m.senderUuid === user?.userUuid;
          return (
            <div key={m.messageUuid} className={`uz-msg-row ${mine ? "out" : "in"}`}>
              <div className="uz-bubble">
                {conv.type === "group" && !mine && <span className="sender">{m.senderName ?? nameOf(m.senderUuid)}</span>}
                {editing?.id === m.messageUuid ? (
                  <div className="d-flex gap-1 align-items-center" style={{ minWidth: 240 }}>
                    <input autoFocus className="form-control form-control-sm" value={editing.content} onChange={(e) => setEditing({ ...editing, content: e.target.value })} onKeyDown={(e) => { if (e.key === "Enter") void saveEdit(); if (e.key === "Escape") setEditing(null); }} />
                    <button className="uz-icon-btn" onClick={saveEdit} aria-label="Salvar"><Check size={16} /></button>
                    <button className="uz-icon-btn" onClick={() => setEditing(null)} aria-label="Cancelar"><X size={16} /></button>
                  </div>
                ) : <>{m.content}<span className="time">{m.editedAt ? "(editada) " : ""}{fmtTime(m.sentAt)}</span></>}
                {mine && editing?.id !== m.messageUuid && <>
                  <button className="menu-btn" aria-label="Ações" onClick={(e) => { e.stopPropagation(); setMenu(menu === m.messageUuid ? null : m.messageUuid); }}><MoreVertical size={14} /></button>
                  {menu === m.messageUuid && <div className="uz-dropdown" onClick={(e) => e.stopPropagation()}>
                    <button onClick={() => { setMenu(null); setEditing({ id: m.messageUuid, content: m.content }); }}><Pencil size={14} />Editar</button>
                    <button className="danger" onClick={() => delForMe(m.messageUuid)}><Trash2 size={14} />Excluir para mim</button>
                  </div>}
                </>}
              </div>
            </div>
          );
        })}
        <div ref={endRef} />
      </div>
      <div className="uz-composer">
        <textarea className="form-control" rows={1} placeholder="Digite uma mensagem" value={text} onChange={(e) => setText(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); void send(); } }} />
        <button className="btn btn-primary uz-send" onClick={send} disabled={sending || !text.trim()} aria-label="Enviar"><SendHorizontal size={18} /></button>
      </div>
    </div>
  );
}

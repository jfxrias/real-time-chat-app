import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { Megaphone, Plus, Settings, ShieldCheck, UserMinus, UserPlus, UserRound } from "lucide-react";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { ChatArea } from "@/components/chat/ChatArea";
import { BroadcastModal, GroupDetailsModal, NewGroupModal, ProfileModal } from "@/components/chat/Modals";
import { initials, useAuth } from "@/lib/auth";
import { toast } from "@/lib/toast";
import { api } from "@/services/api";
import type { ChatMessage, Conversation, Friendship, Group, User } from "@/services/types";
import { friendUuidOf, upsertMessage } from "@/lib/chat-utils";
import { useChatSocket, type WsStatus } from "@/services/websocket";
import sofismarIcon from "../img/favLogoSemFundo.png";

export const Route = createFileRoute("/")({
  head: () => ({ meta: [{ title: "sofismar.app — Chat em tempo real" }, { name: "description", content: "Converse em tempo real com amigos e grupos no sofismar.app." }, { property: "og:title", content: "sofismar.app — Chat em tempo real" }, { property: "og:description", content: "Converse em tempo real com amigos e grupos no sofismar.app." }, { property: "og:type", content: "website" }, { name: "twitter:card", content: "summary_large_image" }] }),
  component: Workspace,
});

type Tab = "contacts" | "groups" | "friends";
const key = (c: { type: string; id: string }) => `${c.type}:${c.id}`;
const WS_LABEL: Record<WsStatus, string> = { connected: "Conectado", reconnecting: "Reconectando", offline: "Offline" };

function Workspace() {
  const { ready, token, user, logout } = useAuth();
  const nav = useNavigate();
  const [tab, setTab] = useState<Tab>("contacts");
  const [search, setSearch] = useState("");
  const [users, setUsers] = useState<User[]>([]); const [friends, setFriends] = useState<Friendship[]>([]); const [groups, setGroups] = useState<Group[]>([]);
  const [conv, setConv] = useState<Conversation | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]); const [loadingMsgs, setLoadingMsgs] = useState(false);
  const [unread, setUnread] = useState<Record<string, number>>({});
  const [modal, setModal] = useState<"profile" | "group" | "broadcast" | "details" | null>(null);
  const convRef = useRef(conv); convRef.current = conv;

  useEffect(() => { if (ready && !token) void nav({ to: "/login" }); }, [ready, token, nav]);
  const loadAll = useCallback(() => {
    api.users().then(setUsers).catch(() => {});
    api.friendships().then(setFriends).catch(() => {});
    api.groups().then(setGroups).catch(() => {});
  }, []);
  useEffect(() => { if (ready && token) loadAll(); }, [ready, token, loadAll]);

  useEffect(() => {
    if (!conv) return;
    let alive = true; setMessages([]); setLoadingMsgs(true);
    (conv.type === "private" ? api.privateHistory(conv.id) : api.groupHistory(conv.id))
      .then((l) => { if (alive) setMessages((cur) => { let out = [...(l ?? [])]; cur.forEach((m) => { out = upsertMessage(out, m); }); return out; }); })
      .catch(() => {}).finally(() => alive && setLoadingMsgs(false));
    setUnread((u) => ({ ...u, [key(conv)]: 0 }));
    return () => { alive = false; };
  }, [conv?.type, conv?.id]);

  const onPrivate = useCallback((m: ChatMessage) => {
    const me = user?.userUuid; const other = m.senderUuid === me ? m.receiverUuid ?? me : m.senderUuid;
    const c = convRef.current;
    if (c?.type === "private" && (c.id === m.senderUuid || c.id === m.receiverUuid) && (c.id !== me || (m.senderUuid === me && m.receiverUuid === me))) setMessages((l) => upsertMessage(l, m));
    else if (m.senderUuid !== me && other && !m.editedAt) setUnread((u) => ({ ...u, [`private:${other}`]: (u[`private:${other}`] ?? 0) + 1 }));
  }, [user?.userUuid]);
  const onGroup = useCallback((m: ChatMessage) => {
    const gid = m.targetGroupUuid ?? ""; const c = convRef.current;
    if (c?.type === "group" && c.id === gid) setMessages((l) => upsertMessage(l, m));
    else if (m.senderUuid !== user?.userUuid && !m.editedAt) setUnread((u) => ({ ...u, [`group:${gid}`]: (u[`group:${gid}`] ?? 0) + 1 }));
  }, [user?.userUuid]);
  const groupIds = useMemo(() => groups.filter((g) => g.status !== "INACTIVE").map((g) => g.groupUuid), [groups]);
  const ws = useChatSocket({ token, userUuid: user?.userUuid ?? null, groupUuids: groupIds, onPrivate, onGroup });

  if (!ready || !token || !user) return <div className="uz-center">Carregando...</div>;

  const friendIds = new Set(friends.map((f) => friendUuidOf(f, user.userUuid)));
  const q = search.toLowerCase();
  const nameOf = (id: string) => users.find((u) => u.userUuid === id)?.name ?? friends.find((f) => friendUuidOf(f, user.userUuid) === id)?.name ?? "Usuário";
  const contacts = [...users].sort((a, b) => (a.userUuid === user.userUuid ? -1 : b.userUuid === user.userUuid ? 1 : a.name.localeCompare(b.name))).filter((u) => u.name.toLowerCase().includes(q));
  const friendList = [...friendIds].filter(Boolean).map((id) => ({ id, name: nameOf(id) })).filter((f) => f.name.toLowerCase().includes(q));
  const groupList = groups.filter((g) => g.groupName.toLowerCase().includes(q));

  const openPrivate = (id: string, name: string) => setConv({ type: "private", id, name: id === user.userUuid ? "Mensagens Salvas" : name });
  const addFriend = async (id: string) => { try { const r = await api.addFriend(id); toast(r?.message || "Amigo adicionado.", "success"); setFriends(await api.friendships()); } catch { /* */ } };
  const removeFriend = async (id: string) => { try { await api.removeFriend(id); toast("Amizade removida.", "success"); setFriends(await api.friendships()); } catch { /* */ } };
  const doLogout = () => { logout(); void nav({ to: "/login" }); };
  const badge = (k: string) => (unread[k] ? <span className="uz-unread">{unread[k]}</span> : null);
  const tabUnread = (prefix: string) => Object.entries(unread).filter(([k]) => k.startsWith(prefix)).reduce((a, [, v]) => a + v, 0);
  const activeGroup = conv?.type === "group" ? groups.find((g) => g.groupUuid === conv.id) : undefined;

  return (
    <div className={`uz-app ${conv ? "has-chat" : ""}`}>
      <aside className="uz-sidebar">
        <div className="sofismar-sidebar-brand">
          <img className="sofismar-logo-icon" src={sofismarIcon} alt="sofismar.app" />
          <span className="sofismar-brand">sofismar.app</span>
        </div>
        <div className="uz-side-header">
          <span className="uz-avatar">{initials(user.name)}</span>
          <div className="who"><strong>{user.name}</strong><span className={`uz-ws ${ws}`}><i />{WS_LABEL[ws]}</span></div>
          <button className="uz-icon-btn" title="Enviar para vários grupos" onClick={() => setModal("broadcast")}><Megaphone size={19} /></button>
          {user.isAdmin && <Link to="/admin" className="uz-icon-btn" title="Painel Admin"><ShieldCheck size={19} /></Link>}
          <button className="uz-icon-btn" title="Perfil" onClick={() => setModal("profile")}><Settings size={19} /></button>
        </div>
        <div className="uz-tabs">
          <button className={`uz-tab ${tab === "contacts" ? "active" : ""}`} onClick={() => setTab("contacts")}>Conversas {tabUnread("private:") > 0 && <span className="uz-unread ms-1">{tabUnread("private:")}</span>}</button>
          <button className={`uz-tab ${tab === "groups" ? "active" : ""}`} onClick={() => setTab("groups")}>Grupos {tabUnread("group:") > 0 && <span className="uz-unread ms-1">{tabUnread("group:")}</span>}</button>
          <button className={`uz-tab ${tab === "friends" ? "active" : ""}`} onClick={() => setTab("friends")}>Amigos</button>
        </div>
        <div className="uz-search d-flex gap-2">
          <input className="form-control" placeholder="Pesquisar..." value={search} onChange={(e) => setSearch(e.target.value)} />
          {tab === "groups" && <button className="btn btn-primary btn-sm d-flex align-items-center gap-1 text-nowrap" onClick={() => setModal("group")}><Plus size={16} />Novo Grupo</button>}
        </div>
        <div className="uz-list">
          {tab === "contacts" && (contacts.length ? contacts.map((u) => {
            const self = u.userUuid === user.userUuid; const k = `private:${u.userUuid}`;
            return <div key={u.userUuid} className={`uz-item ${conv && key(conv) === k ? "active" : ""}`} onClick={() => openPrivate(u.userUuid, u.name)}>
              <span className="uz-avatar">{self ? <UserRound size={18} /> : initials(u.name)}</span>
              <div className="meta"><strong>{self ? "Mensagens Salvas" : u.name}</strong><small>{self ? "Você" : friendIds.has(u.userUuid) ? "Amigo" : u.isActive === false ? "Inativo" : "Contato"}</small></div>
              {badge(k)}
              {!self && !friendIds.has(u.userUuid) && <button className="uz-icon-btn" title="Adicionar amigo" onClick={(e) => { e.stopPropagation(); void addFriend(u.userUuid); }}><UserPlus size={17} /></button>}
            </div>;
          }) : <div className="uz-empty">Nenhum usuário encontrado.</div>)}
          {tab === "friends" && (friendList.length ? friendList.map((f) => <div key={f.id} className={`uz-item ${conv && key(conv) === `private:${f.id}` ? "active" : ""}`} onClick={() => openPrivate(f.id, f.name)}>
            <span className="uz-avatar">{initials(f.name)}</span><div className="meta"><strong>{f.name}</strong><small>Amigo</small></div>{badge(`private:${f.id}`)}
            <button className="uz-icon-btn" title="Remover amigo" onClick={(e) => { e.stopPropagation(); void removeFriend(f.id); }}><UserMinus size={17} /></button>
          </div>) : <div className="uz-empty">Você ainda não tem amigos. Adicione pela aba Conversas.</div>)}
          {tab === "groups" && (groupList.length ? groupList.map((g) => <div key={g.groupUuid} className={`uz-item ${conv && key(conv) === `group:${g.groupUuid}` ? "active" : ""}`} onClick={() => setConv({ type: "group", id: g.groupUuid, name: g.groupName })}>
            <span className="uz-avatar group">{initials(g.groupName)}</span><div className="meta"><strong>{g.groupName}</strong><small><span className={`badge ${g.status === "INACTIVE" ? "text-bg-danger" : "text-bg-success"}`}>{g.status}</span></small></div>{badge(`group:${g.groupUuid}`)}
          </div>) : <div className="uz-empty">Nenhum grupo. Crie o primeiro!</div>)}
        </div>
      </aside>

      {conv ? <ChatArea conv={conv} messages={messages} loading={loadingMsgs} users={users}
        onSent={(m) => setMessages((l) => upsertMessage(l, m))}
        onEdited={(id, content, editedAt) => setMessages((l) => l.map((m) => (m.messageUuid === id ? { ...m, content, editedAt } : m)))}
        onRemoved={(id) => setMessages((l) => l.filter((m) => m.messageUuid !== id))}
        onBack={() => setConv(null)} onDetails={() => setModal("details")} />
        : <div className="uz-main"><div className="uz-welcome"><img className="sofismar-welcome-logo mb-3" src={sofismarIcon} alt="sofismar.app" /><h2 className="sofismar-brand">sofismar.app</h2><p>Escolha uma conversa ou grupo para começar.</p></div></div>}

      {modal === "profile" && <ProfileModal onClose={() => setModal(null)} onLogout={doLogout} />}
      {modal === "group" && <NewGroupModal onClose={() => setModal(null)} onCreated={(g) => { setGroups((l) => [...l, g]); setModal(null); setTab("groups"); setConv({ type: "group", id: g.groupUuid, name: g.groupName }); }} />}
      {modal === "broadcast" && <BroadcastModal groups={groups} onClose={() => setModal(null)} />}
      {modal === "details" && activeGroup && <GroupDetailsModal group={activeGroup} users={users} onClose={() => setModal(null)}
        onChanged={(g) => { setGroups((l) => l.map((x) => (x.groupUuid === g.groupUuid ? g : x))); setConv({ type: "group", id: g.groupUuid, name: g.groupName }); }}
        onDeleted={(id) => { setGroups((l) => l.filter((x) => x.groupUuid !== id)); setConv(null); setModal(null); }} />}
    </div>
  );
}

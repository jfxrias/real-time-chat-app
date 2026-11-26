import { Client, type StompSubscription } from "@stomp/stompjs";
import { useEffect, useRef, useState } from "react";
import { WS_URL } from "./api";
import type { ChatMessage } from "./types";

export type WsStatus = "connected" | "reconnecting" | "offline";

interface Opts { token: string | null; userUuid: string | null; groupUuids: string[]; onPrivate: (m: ChatMessage) => void; onGroup: (m: ChatMessage) => void }

export function useChatSocket({ token, userUuid, groupUuids, onPrivate, onGroup }: Opts): WsStatus {
  const [status, setStatus] = useState<WsStatus>("offline");
  const clientRef = useRef<Client | null>(null);
  const groupSubs = useRef(new Map<string, StompSubscription>());
  const handlers = useRef({ onPrivate, onGroup });
  handlers.current = { onPrivate, onGroup };
  const groupsRef = useRef(groupUuids);
  groupsRef.current = groupUuids;

  const syncGroups = () => {
    const c = clientRef.current;
    if (!c || !c.connected) return;
    const want = new Set(groupsRef.current);
    for (const [id, sub] of groupSubs.current) if (!want.has(id)) { sub.unsubscribe(); groupSubs.current.delete(id); }
    for (const id of want) if (!groupSubs.current.has(id)) {
      groupSubs.current.set(id, c.subscribe(`/topic/groups/${id}`, (f) => {
        try { const m = JSON.parse(f.body) as ChatMessage; handlers.current.onGroup({ ...m, targetGroupUuid: m.targetGroupUuid ?? m.groupUuid ?? id }); } catch { /* ignore */ }
      }));
    }
  };

  useEffect(() => {
    if (typeof window === "undefined" || !token || !userUuid) return;
    const client = new Client({ brokerURL: WS_URL, connectHeaders: { Authorization: `Bearer ${token}` }, reconnectDelay: 5000 });
    client.onConnect = () => {
      setStatus("connected");
      groupSubs.current.clear();
      client.subscribe(`/topic/private/${userUuid}`, (f) => { try { handlers.current.onPrivate(JSON.parse(f.body)); } catch { /* ignore */ } });
      syncGroups();
    };
    client.onWebSocketClose = () => setStatus((s) => (s === "connected" ? "reconnecting" : s === "reconnecting" ? "reconnecting" : "offline"));
    client.onStompError = () => setStatus("reconnecting");
    clientRef.current = client;
    setStatus("reconnecting");
    client.activate();
    return () => { void client.deactivate(); clientRef.current = null; groupSubs.current.clear(); setStatus("offline"); };
  }, [token, userUuid]);

  useEffect(() => { syncGroups(); }, [groupUuids.join(","), status]);
  return status;
}

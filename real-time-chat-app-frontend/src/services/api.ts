import { toast } from "@/lib/toast";
import type { ChatMessage, Friendship, FriendshipCreated, Group, GroupMember, Report, User } from "./types";

export const API_BASE_URL: string = import.meta.env["VITE_API_BASE_URL"] || "http://localhost:8080";
export const WS_URL: string = import.meta.env["VITE_WS_URL"] || "ws://localhost:8080/ws";
export const TOKEN_KEY = "uaizap.token";
export const USER_KEY = "uaizap.user";

export class ApiError extends Error { constructor(message: string, public status: number) { super(message); } }

export function clearSession() {
  if (typeof window === "undefined") return;
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

async function request<T>(method: string, path: string, body?: unknown, opts: { auth?: boolean; headers?: Record<string, string>; silent?: boolean } = {}): Promise<T> {
  const auth = opts.auth ?? true;
  const headers: Record<string, string> = { Accept: "application/json", ...(opts.headers ?? {}) };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (auth && typeof window !== "undefined") { const t = localStorage.getItem(TOKEN_KEY); if (t) headers["Authorization"] = `Bearer ${t}`; }
  let res: Response;
  try {
    res = await fetch(API_BASE_URL + path, { method, headers, body: body !== undefined ? JSON.stringify(body) : null });
  } catch {
    const e = new ApiError("Não foi possível conectar ao servidor.", 0);
    if (!opts.silent) toast(e.message, "error");
    throw e;
  }
  const text = await res.text();
  let data: unknown = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!res.ok) {
    if (res.status === 401 && auth && typeof window !== "undefined") {
      clearSession();
      if (window.location.pathname !== "/login") window.location.href = "/login";
    }
    const msg: string = String((data && typeof data === "object" && "message" in data && String((data as { message: unknown }).message)) || `Erro ${res.status}`);
    if (!opts.silent) toast(msg, "error");
    throw new ApiError(msg, res.status);
  }
  return data as T;
}

type Msg = { message: string };
export const api = {
  register: (b: { name: string; cpf: string; password: string; isAdmin: boolean }) => request<User>("POST", "/auth/register", b, { auth: false }),
  login: (b: { cpf: string; password: string }) => request<{ token: string; user: User }>("POST", "/auth/login", b, { auth: false }),
  me: () => request<User>("GET", "/auth/me"),
  users: () => request<User[]>("GET", "/auth/users"),
  updateName: (name: string) => request<User>("PATCH", "/auth/name", { name }),
  updatePassword: (password: string) => request<Msg>("PATCH", "/auth/password", { password }),
  friendships: () => request<Friendship[]>("GET", "/auth/friendships"),
  addFriend: (userUuid: string) => request<FriendshipCreated>("POST", "/auth/friendships", { userUuid }),
  removeFriend: (userUuid: string) => request<Msg>("DELETE", `/auth/friendships/${userUuid}`),
  groups: () => request<Group[]>("GET", "/groups"),
  createGroup: (groupName: string, creatorUuid: string) => request<Group>("POST", "/groups", { groupName, creatorUuid }),
  renameGroup: (id: string, groupName: string) => request<Group>("PATCH", `/groups/${id}`, { groupName }),
  deleteGroup: (id: string, userUuid: string) => request<Msg>("DELETE", `/groups/${id}`, undefined, { headers: { "user-uuid": userUuid } }),
  members: (id: string) => request<GroupMember[]>("GET", `/groups/${id}/members`),
  addMember: (id: string, userUuid: string, role: "MEMBER" | "ADMIN") => request<Msg>("POST", `/groups/${id}/members`, { userUuid, role }),
  removeMember: (id: string, userUuid: string) => request<Msg>("DELETE", `/groups/${id}/members/${userUuid}`),
  privateHistory: (other: string) => request<ChatMessage[]>("GET", `/messages/private/${other}`),
  sendPrivate: (receiverUuid: string, content: string) => request<ChatMessage>("POST", "/messages/private", { receiverUuid, content }),
  groupHistory: (id: string) => request<ChatMessage[]>("GET", `/messages/groups/${id}`),
  sendGroups: (groupUuids: string[], content: string) => request<Msg & { deliveries: ChatMessage[] }>("POST", "/messages/groups", { groupUuids, content }),
  editMessage: (id: string, content: string) => request<{ messageUuid: string; content: string; editedAt: string }>("PATCH", `/messages/${id}`, { content }),
  deleteForMe: (id: string) => request<Msg>("PATCH", `/messages/${id}/delete-for-me`),
  report: (b: { reportedUuid: string; groupUuid: string; reason: string }) => request<Report & Msg>("POST", "/reports", b),
  reports: () => request<Report[]>("GET", "/reports"),
  adminUsers: () => request<User[]>("GET", "/admin/users"),
  setUserActive: (id: string, active: boolean) => request<Msg & { userUuid: string; isActive: boolean }>("PATCH", `/admin/users/${id}/${active ? "reactivate" : "deactivate"}`),
  adminGroups: () => request<Group[]>("GET", "/admin/groups"),
  setGroupActive: (id: string, active: boolean) => request<Msg & { groupUuid: string; status: string }>("PATCH", `/admin/groups/${id}/${active ? "reactivate" : "deactivate"}`),
};

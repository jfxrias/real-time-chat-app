import type { ChatMessage, Friendship } from "@/services/types";

export function friendUuidOf(f: Friendship, _me: string) {
  return f.userUuid;
}
export function upsertMessage(list: ChatMessage[], m: ChatMessage) {
  const index = list.findIndex((x) => x.messageUuid === m.messageUuid);
  if (index === -1) return [...list, m];
  const next = [...list];
  next[index] = { ...list[index]!, ...m };
  return next;
}


import { describe, expect, it } from "vitest";
import { upsertMessage } from "@/lib/chat-utils";
const m = { messageUuid: "a", senderUuid: "u", content: "oi", sentAt: "2026-01-01T00:00:00Z" };
const editedAt = "2026-01-01T00:01:00Z";
describe("upsertMessage", () => {
  it("não duplica mensagens com o mesmo messageUuid (REST + WebSocket)", () => {
    expect(upsertMessage(upsertMessage([], m), { ...m })).toHaveLength(1);
  });
  it("atualiza content e editedAt mantendo a posição original", () => {
    const other = { ...m, messageUuid: "b", content: "segunda" };
    const list = upsertMessage(upsertMessage([], m), other);
    const result = upsertMessage(list, { ...m, content: "editada", editedAt });
    expect(result).toHaveLength(2);
    expect(result[0]).toMatchObject({ messageUuid: "a", content: "editada", editedAt });
    expect(result[1]?.messageUuid).toBe("b");
  });
});

import { describe, expect, it } from "vitest";
import { groupMemberRoleLabel, groupStatusLabel } from "@/lib/labels";

describe("labels de grupos", () => {
  it("traduz status conhecidos", () => {
    expect(groupStatusLabel("ACTIVE")).toBe("Ativo");
    expect(groupStatusLabel("INACTIVE")).toBe("Inativo");
  });

  it("traduz papéis conhecidos", () => {
    expect(groupMemberRoleLabel("MEMBER")).toBe("Membro");
    expect(groupMemberRoleLabel("ADMIN")).toBe("Admin");
    expect(groupMemberRoleLabel("CREATOR")).toBe("Criador");
  });

  it("não expõe enums desconhecidos em inglês", () => {
    expect(groupStatusLabel("SUSPENDED")).toBe("Desconhecido");
    expect(groupMemberRoleLabel("MODERATOR")).toBe("Desconhecido");
  });
});

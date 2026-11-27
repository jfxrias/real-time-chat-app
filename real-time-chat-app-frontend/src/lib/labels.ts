export function groupStatusLabel(status: string): string {
  switch (status) {
    case "ACTIVE":
      return "Ativo";
    case "INACTIVE":
      return "Inativo";
    default:
      return "Desconhecido";
  }
}

export function groupMemberRoleLabel(role: string): string {
  switch (role) {
    case "MEMBER":
      return "Membro";
    case "ADMIN":
      return "Admin";
    case "CREATOR":
      return "Criador";
    default:
      return "Desconhecido";
  }
}

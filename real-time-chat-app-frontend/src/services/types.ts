export interface User { userUuid: string; name: string; cpf: string; isAdmin: boolean; isActive: boolean }
export interface Friendship { userUuid: string; name: string; cpf: string; isAdmin: boolean; isActive: boolean; createdAt: string }
export interface FriendshipCreated { message: string; firstUserUuid: string; secondUserUuid: string; createdAt: string }
export interface Group { groupUuid: string; groupName: string; creatorUuid: string; status: string; createdAt?: string }
export interface GroupMember { userUuid: string; name: string; role: "MEMBER" | "ADMIN" | "CREATOR" | string; joinedAt: string }
export interface ChatMessage { messageUuid: string; senderUuid: string; receiverUuid?: string; targetGroupUuid?: string; groupUuid?: string; content: string; sentAt: string; editedAt?: string | null; senderName?: string }
export interface Report { reportUuid: string; reporterUuid: string; reportedUuid: string; groupUuid: string; reason: string; reportedAt: string }
export interface ReceivedPercentage { userUuid: string; name: string; receivedCount: number; percentage: number }
export interface UserMostFriends { userUuid: string; name: string; totalFriends: number }
export interface GroupMemberCount { groupUuid: string; groupName: string; totalUsers: number }
export interface UserTopReported { userUuid: string; name: string; totalReports: number }
export interface CommonReportReason { reason: string; totalReports: number }
export interface UserMostGroups { userUuid: string; name: string; totalGroups: number }
export interface UserMostInactive { userUuid: string; name: string; lastMessageAt: string; createdAt: string }
export interface UserMessageStats { userUuid: string; name: string; sentCount: number; receivedCount: number; totalCount: number }
export type Conversation = { type: "private"; id: string; name: string } | { type: "group"; id: string; name: string };

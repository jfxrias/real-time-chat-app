import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { ApiError, api } from "@/services/api";
import type {
  CommonReportReason,
  GroupMemberCount,
  ReceivedPercentage,
  User,
  UserMessageStats,
  UserMostFriends,
  UserMostGroups,
  UserMostInactive,
  UserTopReported,
} from "@/services/types";

function localDateValue(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function getDefaultDates() {
  const end = new Date();
  const start = new Date(end);
  start.setDate(start.getDate() - 29);
  return { start: localDateValue(start), end: localDateValue(end) };
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).format(new Date(value));
}

function AnalyticsDashboard({ users }: { users: User[] }) {
  const [received, setReceived] = useState<ReceivedPercentage[]>([]);
  const [mostFriends, setMostFriends] = useState<UserMostFriends[]>([]);
  const [groupCounts, setGroupCounts] = useState<GroupMemberCount[]>([]);
  const [topReported, setTopReported] = useState<UserTopReported[]>([]);
  const [commonReasons, setCommonReasons] = useState<CommonReportReason[]>([]);
  const [mostGroups, setMostGroups] = useState<UserMostGroups[]>([]);
  const [mostInactive, setMostInactive] = useState<UserMostInactive | null>(null);
  const [inactiveEmpty, setInactiveEmpty] = useState(false);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState(false);
  const dates = getDefaultDates();
  const [selectedUserUuid, setSelectedUserUuid] = useState("");
  const [startDate, setStartDate] = useState(dates.start);
  const [endDate, setEndDate] = useState(dates.end);
  const [stats, setStats] = useState<UserMessageStats | null>(null);
  const [statsLoading, setStatsLoading] = useState(false);
  const [statsError, setStatsError] = useState("");

  useEffect(() => {
    if (!selectedUserUuid && users[0]) setSelectedUserUuid(users[0].userUuid);
  }, [selectedUserUuid, users]);

  const loadAnalytics = useCallback(async () => {
    setLoading(true);
    setLoadError(false);
    setInactiveEmpty(false);
    const results = await Promise.allSettled([
      api.analyticsReceivedPercentage(),
      api.analyticsMostFriends(),
      api.analyticsGroupMemberCount(),
      api.analyticsTopReported(),
      api.analyticsCommonReasons(),
      api.analyticsMostGroups(),
      api.analyticsMostInactive(),
    ]);

    const [
      receivedResult,
      friendsResult,
      groupsResult,
      reportedResult,
      reasonsResult,
      mostGroupsResult,
      inactiveResult,
    ] = results;
    if (receivedResult.status === "fulfilled") setReceived(receivedResult.value);
    if (friendsResult.status === "fulfilled") setMostFriends(friendsResult.value);
    if (groupsResult.status === "fulfilled") setGroupCounts(groupsResult.value);
    if (reportedResult.status === "fulfilled") setTopReported(reportedResult.value);
    if (reasonsResult.status === "fulfilled") setCommonReasons(reasonsResult.value);
    if (mostGroupsResult.status === "fulfilled") setMostGroups(mostGroupsResult.value);
    if (inactiveResult.status === "fulfilled") setMostInactive(inactiveResult.value);

    const noUsers =
      inactiveResult.status === "rejected" &&
      inactiveResult.reason instanceof ApiError &&
      inactiveResult.reason.status === 400 &&
      inactiveResult.reason.message === "Nenhum usuario encontrado";
    if (noUsers) {
      setMostInactive(null);
      setInactiveEmpty(true);
    }
    setLoadError(
      results.slice(0, 6).some((result) => result.status === "rejected") ||
        (inactiveResult.status === "rejected" && !noUsers),
    );
    setLoading(false);
  }, []);

  useEffect(() => {
    void loadAnalytics();
  }, [loadAnalytics]);

  const submitStats = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (startDate > endDate) {
      setStatsError("A data inicial deve ser anterior ou igual à data final.");
      setStats(null);
      return;
    }
    if (!selectedUserUuid) {
      setStatsError("Selecione um usuário.");
      setStats(null);
      return;
    }
    setStatsLoading(true);
    setStatsError("");
    setStats(null);
    try {
      setStats(
        await api.analyticsMessageStats(
          selectedUserUuid,
          `${startDate}T00:00:00`,
          `${endDate}T23:59:59`,
        ),
      );
    } catch (error) {
      setStatsError(
        error instanceof Error ? error.message : "Não foi possível consultar as mensagens.",
      );
    } finally {
      setStatsLoading(false);
    }
  };

  const maxReasonCount = Math.max(0, ...commonReasons.map((reason) => reason.totalReports));
  const inactiveDays = mostInactive
    ? Math.max(
        0,
        Math.floor((Date.now() - new Date(mostInactive.lastMessageAt).getTime()) / 86_400_000),
      )
    : 0;

  return (
    <section className="uz-analytics">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <div>
          <h2 className="h5 mb-1">Analytics</h2>
          <p className="small text-secondary mb-0">Indicadores de usuários, grupos e denúncias.</p>
        </div>
        <button
          className="btn btn-primary btn-sm"
          onClick={() => void loadAnalytics()}
          disabled={loading}
        >
          {loading ? "Atualizando..." : "Atualizar"}
        </button>
      </div>
      {loadError && (
        <p className="uz-analytics-feedback" role="status">
          Alguns dados não puderam ser carregados. Tente atualizar.
        </p>
      )}

      <div className="row g-3 mb-3">
        <div className="col-12 col-lg-6">
          <section className="uz-card uz-analytics-card h-100">
            <h3 className="uz-analytics-title">Mais tempo sem enviar mensagens</h3>
            {mostInactive ? (
              <>
                <p className="uz-analytics-highlight mb-1">{mostInactive.name}</p>
                <p className="text-secondary small mb-1">
                  Última mensagem: {formatDate(mostInactive.lastMessageAt)}
                </p>
                <p className="text-secondary small mb-0">{inactiveDays} dias sem atividade</p>
                {mostInactive.lastMessageAt === mostInactive.createdAt && (
                  <p className="uz-analytics-note mb-0">Nunca enviou mensagens.</p>
                )}
              </>
            ) : inactiveEmpty ? (
              <p className="text-secondary small mb-0">Nenhum usuário encontrado.</p>
            ) : loading ? (
              <p className="text-secondary small mb-0">Carregando...</p>
            ) : (
              <p className="text-secondary small mb-0">Dados indisponíveis.</p>
            )}
          </section>
        </div>

        <div className="col-12 col-lg-6">
          <section className="uz-card uz-analytics-card h-100">
            <h3 className="uz-analytics-title">Mensagens por usuário no período</h3>
            <form onSubmit={submitStats}>
              <label className="form-label small" htmlFor="analytics-user">
                Usuário
              </label>
              <select
                id="analytics-user"
                className="form-select form-select-sm mb-2"
                value={selectedUserUuid}
                onChange={(event) => setSelectedUserUuid(event.target.value)}
                required
              >
                <option value="">Selecione um usuário</option>
                {users.map((user) => (
                  <option key={user.userUuid} value={user.userUuid}>
                    {user.name}
                  </option>
                ))}
              </select>
              <div className="row g-2">
                <div className="col-6">
                  <label className="form-label small" htmlFor="analytics-start">
                    Início
                  </label>
                  <input
                    id="analytics-start"
                    className="form-control form-control-sm"
                    type="date"
                    value={startDate}
                    onChange={(event) => setStartDate(event.target.value)}
                    required
                  />
                </div>
                <div className="col-6">
                  <label className="form-label small" htmlFor="analytics-end">
                    Fim
                  </label>
                  <input
                    id="analytics-end"
                    className="form-control form-control-sm"
                    type="date"
                    value={endDate}
                    onChange={(event) => setEndDate(event.target.value)}
                    required
                  />
                </div>
              </div>
              <button
                className="btn btn-primary btn-sm mt-3"
                type="submit"
                disabled={statsLoading || users.length === 0}
              >
                {statsLoading ? "Consultando..." : "Consultar"}
              </button>
            </form>
            {statsError && (
              <p className="uz-analytics-feedback mt-2 mb-0" role="status">
                {statsError}
              </p>
            )}
            {stats && (
              <div className="row g-2 mt-2">
                <div className="col-4">
                  <div className="uz-analytics-stat">
                    <span>Enviadas</span>
                    <strong>{stats.sentCount}</strong>
                  </div>
                </div>
                <div className="col-4">
                  <div className="uz-analytics-stat">
                    <span>Recebidas</span>
                    <strong>{stats.receivedCount}</strong>
                  </div>
                </div>
                <div className="col-4">
                  <div className="uz-analytics-stat">
                    <span>Total</span>
                    <strong>{stats.totalCount}</strong>
                  </div>
                </div>
              </div>
            )}
          </section>
        </div>
      </div>

      <div className="row g-3">
        <div className="col-12 col-lg-6">
          <section className="uz-card uz-analytics-card">
            <h3 className="uz-analytics-title">% de mensagens recebidas por usuário</h3>
            <div className="uz-analytics-list">
              {received.map((item) => (
                <div className="uz-analytics-list-item" key={item.userUuid}>
                  <div className="d-flex justify-content-between gap-3">
                    <span>{item.name}</span>
                    <span className="text-nowrap">
                      {item.receivedCount} ·{" "}
                      {new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 2 }).format(
                        item.percentage,
                      )}
                      %
                    </span>
                  </div>
                  <div className="uz-analytics-track" aria-hidden="true">
                    <span style={{ width: `${Math.min(100, Math.max(0, item.percentage))}%` }} />
                  </div>
                </div>
              ))}
              {!loading && received.length === 0 && (
                <p className="text-secondary small mb-0">Nenhum dado disponível.</p>
              )}
            </div>
          </section>
        </div>

        <div className="col-12 col-lg-6">
          <section className="uz-card uz-analytics-card">
            <h3 className="uz-analytics-title">Motivos mais comuns de denúncia</h3>
            <div className="uz-analytics-list">
              {commonReasons.map((item) => (
                <div className="uz-analytics-list-item" key={item.reason}>
                  <div className="d-flex justify-content-between gap-3">
                    <span>{item.reason}</span>
                    <span>{item.totalReports}</span>
                  </div>
                  <div className="uz-analytics-track" aria-hidden="true">
                    <span
                      style={{
                        width: `${maxReasonCount ? (item.totalReports / maxReasonCount) * 100 : 0}%`,
                      }}
                    />
                  </div>
                </div>
              ))}
              {!loading && commonReasons.length === 0 && (
                <p className="text-secondary small mb-0">Nenhum dado disponível.</p>
              )}
            </div>
          </section>
        </div>

        <div className="col-12 col-lg-6">
          <section className="uz-card uz-analytics-card">
            <h3 className="uz-analytics-title">Usuários com mais de 3 denúncias</h3>
            <div className="uz-analytics-list">
              {topReported.map((item) => (
                <div className="uz-analytics-row" key={item.userUuid}>
                  <span>{item.name}</span>
                  <span className="badge uz-analytics-danger">{item.totalReports}</span>
                </div>
              ))}
              {!loading && topReported.length === 0 && (
                <p className="text-secondary small mb-0">Nenhum usuário com mais de 3 denúncias.</p>
              )}
            </div>
          </section>
        </div>

        <div className="col-12 col-lg-6">
          <AnalyticsTable
            title="Usuários com mais amigos"
            rows={mostFriends.map((item) => [item.name, item.totalFriends])}
            label="Amigos"
            loading={loading}
          />
        </div>
        <div className="col-12 col-lg-6">
          <AnalyticsTable
            title="Usuários por grupo"
            rows={groupCounts.map((item) => [item.groupName, item.totalUsers])}
            label="Usuários"
            loading={loading}
          />
        </div>
        <div className="col-12 col-lg-6">
          <AnalyticsTable
            title="Usuários em mais grupos"
            rows={mostGroups.map((item) => [item.name, item.totalGroups])}
            label="Grupos"
            loading={loading}
          />
        </div>
      </div>
    </section>
  );
}

function AnalyticsTable({
  title,
  rows,
  label,
  loading,
}: {
  title: string;
  rows: [string, number][];
  label: string;
  loading: boolean;
}) {
  return (
    <section className="uz-card uz-analytics-card">
      <h3 className="uz-analytics-title">{title}</h3>
      <div className="uz-analytics-list">
        {rows.map(([name, count], index) => (
          <div className="uz-analytics-row" key={`${name}-${index}`}>
            <span>{name}</span>
            <span className="uz-analytics-count">
              {count} <span className="text-secondary">{label}</span>
            </span>
          </div>
        ))}
        {!loading && rows.length === 0 && (
          <p className="text-secondary small mb-0">Nenhum dado disponível.</p>
        )}
      </div>
    </section>
  );
}

export default AnalyticsDashboard;

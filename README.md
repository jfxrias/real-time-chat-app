<p align="center"><img src="./assets/readme.png" alt="Logo sofismar.app" width="220" /></p>

<h1 align="center">sofismar.app — Sistema de Mensagens Instantâneas</h1>

Plataforma full-stack de mensagens instantâneas privadas e em grupo com identificação única por CPF.  
Inclui autenticação JWT, entrega em tempo real via WebSocket/STOMP, moderação administrativa e consultas analíticas implementadas em SQL.

## Visão geral

O projeto é dividido em backend Spring Boot e frontend React. Os dados são persistidos em MySQL por JPA/Hibernate e consultas JDBC; a comunicação ocorre por REST para operações CRUD e por WebSocket/STOMP para entrega em tempo real de mensagens e edições. Os endpoints protegidos são autenticados com tokens JWT e o identificador de usuário usado pelo sistema é o UUID associado ao cadastro identificado por CPF.

O backend segue uma organização em camadas: controllers recebem as requisições HTTP, services aplicam as regras de negócio, repositories acessam os dados e entities representam as tabelas. O WebSocket usa o broker simples do Spring, com o endpoint `/ws` e destinos `/topic`.

O sofismar.app parte de uma premissa de autenticidade e rastreabilidade nas conversas: cada conta é vinculada a um CPF único para garantir a regra de uma conta por pessoa e reduzir a criação de perfis falsos. Internamente, o sistema desacopla esse dado utilizando UUIDs como identificadores nos relacionamentos e nas rotas da API.

## Tecnologias utilizadas

### Backend

- Java 17
- Spring Boot 4.1.2
- Gradle, Spring Data JPA e JDBC
- Spring Web MVC, Spring Security e Spring WebSocket/STOMP
- JJWT 0.12.6
- MySQL Connector/J
- Lombok
- MySQL
- JUnit 5 e H2 para os testes

### Frontend

O frontend está em `real-time-chat-app-frontend/` neste workspace.

- React 19.2.0
- TypeScript 5.8.3
- Vite 8.1.7
- TanStack Router 1.170.42 e TanStack React Query 5.101.1
- Bootstrap 5.3.9
- STOMP para JavaScript 7.5.0
- Vitest 4.1.12

O arquivo `.env.example` define `VITE_API_BASE_URL=http://localhost:8080` e `VITE_WS_URL=ws://localhost:8080/ws`.

## Modelagem do Banco de Dados

Diagrama disponível em `sql/Diagrama de Entidade e Relacionamento/DER.png`:

![Diagrama Entidade-Relacionamento](real-time-chat-app-backend/sql/Diagrama%20de%20Entidade%20e%20Relacionamento/DER.png)

O script `sql/bd.sql` cria o banco `uaizap_db` e as seguintes estruturas:

| Tabela | Conteúdo |
|---|---|
| `user` | Usuários identificados por UUID, CPF, nome, hash da senha, estado ativo e perfil administrativo. |
| `friendship` | Relação entre dois usuários, com UUID composto pelas duas partes da amizade. |
| `group` | Grupos com nome, status `ACTIVE`/`INACTIVE`, criador e data de criação. |
| `group_member` | Associação entre usuários e grupos, com papel `MEMBER` ou `ADMIN`. |
| `report` | Denúncias com usuário denunciante, usuário denunciado, grupo e motivo. |
| `message` | Dados comuns das mensagens: remetente, conteúdo, datas e exclusão para o remetente. |
| `private_message` | Especialização de `message` para mensagens privadas, contendo destinatário e exclusão pelo receptor. |
| `group_message` | Especialização de `message` para mensagens enviadas a um grupo. |

O script de consultas analíticas está em `sql/consultas_analiticas.sql`.

## Funcionalidades implementadas

- Cadastro e login por CPF, com senha armazenada como hash e token JWT.
- Consulta e atualização do perfil autenticado, alteração de nome e senha.
- Cadastro de amizades, listagem e remoção de relações.
- Criação, alteração, exclusão e listagem de grupos.
- Inclusão e remoção de membros, com regras para criador e administradores do grupo.
- Envio e histórico de mensagens privadas.
- Envio de uma mensagem para vários grupos e histórico de mensagens de um grupo.
- Edição de mensagens privadas e de grupo; os eventos são publicados em tempo real pelo STOMP.
- Exclusão individual de mensagens para o usuário que solicitou a operação.
- Registro de denúncias e consulta administrativa das denúncias.
- Desativação e reativação administrativas de usuários e grupos.
- Listagem administrativa de usuários e grupos.
- 8 consultas analíticas protegidas para administradores.

## Como executar localmente

### Pré-requisitos

- JDK 17
- MySQL
- Git
- Node.js e npm, apenas para executar o frontend

### Banco de dados

A aplicação backend está configurada em `src/main/resources/application.properties` para acessar `jdbc:mysql://localhost:3306/uaizap_db`. Use o MySQL como usuário com permissão para criar o banco e substituir as credenciais locais por placeholders fora do controle de versão:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/uaizap_db
spring.datasource.username=<usuario_mysql>
spring.datasource.password=<senha_mysql>
```

Na raiz do backend, crie o banco e as tabelas:

```powershell
cd real-time-chat-app-backend
mysql -u <usuario_mysql> -p < sql/bd.sql
```

### Backend

No Windows, execute a partir da pasta `real-time-chat-app-backend`:

```powershell
.\gradlew.bat bootRun
```

No ambiente Unix, também disponível neste repositório:

```bash
./gradlew bootRun
```

A aplicação Spring Boot usa a porta padrão `8080`.

Para executar os testes automatizados com o perfil H2 configurado em `src/test/resources/application.properties`:

```powershell
.\gradlew.bat test
```

```bash
./gradlew test
```

### Frontend

O frontend está no mesmo workspace e usa os arquivos `.env`/`.env.example` já configurados para `http://localhost:8080` e `ws://localhost:8080/ws`.

```powershell
cd real-time-chat-app-frontend
npm install
npm run dev
```

## Principais Endpoints

| Método | Rota | Autenticação | Propósito |
|---|---|---|---|
| `POST` | `/auth/register` | Público | Cadastra usuário por CPF e opcionalmente recebe `admin-uuid` no registro. |
| `POST` | `/auth/login` | Público | Autentica por CPF e senha e devolve JWT. |
| `PATCH` | `/auth/name` | JWT | Atualiza o nome do usuário autenticado. |
| `PATCH` | `/auth/password` | JWT | Atualiza a senha do usuário autenticado. |
| `POST` | `/auth/friendships` | JWT | Cria amizade com o usuário informado. |
| `GET` | `/auth/me` | JWT | Consulta o perfil do usuário autenticado. |
| `GET` | `/auth/users` | JWT | Lista usuários ativos. |
| `GET` | `/auth/friendships` | JWT | Lista as amizades do usuário autenticado. |
| `DELETE` | `/auth/friendships/{userUuid}` | JWT | Remove uma amizade. |
| `POST` | `/groups` ou `/group` | Público (UUID do criador) | Cria grupo. Também aceita `/groups/create` e `/group/create`. |
| `POST` | `/groups/{groupUuid}/members` | JWT | Adiciona membro e seu papel ao grupo. |
| `DELETE` | `/groups/{groupUuid}/members/{userUuid}` | JWT | Remove membro do grupo. |
| `DELETE` | `/groups/{groupUuid}`, `/groups/delete/{groupUuid}` ou `/groups` | Público (UUID do criador) | Exclui grupo. |
| `PATCH` | `/groups/{groupUuid}` | JWT | Atualiza o nome do grupo. |
| `GET` | `/groups` | JWT | Lista grupos do usuário autenticado. |
| `GET` | `/groups/{groupUuid}/members` | JWT | Lista membros do grupo. |
| `PATCH` | `/messages/{messageUuid}` | JWT | Edita uma mensagem do remetente. |
| `PATCH` | `/messages/{messageUuid}/delete-for-me` | JWT | Marca a mensagem como excluída para o usuário. |
| `POST` | `/messages/private` | JWT | Envia mensagem privada. |
| `POST` | `/messages/groups` | JWT | Envia uma mensagem para uma lista de grupos. |
| `GET` | `/messages/private/{otherUserUuid}` | JWT | Consulta o histórico privado com outro usuário. |
| `GET` | `/messages/groups/{groupUuid}` | JWT | Consulta o histórico de mensagens do grupo. |
| `POST` | `/reports` | JWT | Registra uma denúncia. |
| `GET` | `/reports` | Admin | Lista todas as denúncias. |
| `PATCH` | `/admin/users/{userUuid}/deactivate` | Admin | Desativa usuário. |
| `PATCH` | `/admin/users/{userUuid}/reactivate` | Admin | Reativa usuário. |
| `PATCH` | `/admin/groups/{groupUuid}/deactivate` | Admin | Desativa grupo. |
| `PATCH` | `/admin/groups/{groupUuid}/reactivate` | Admin | Reativa grupo. |
| `GET` | `/admin/users` | Admin | Lista todos os usuários. |
| `GET` | `/admin/groups` | Admin | Lista todos os grupos. |
| `GET` | `/analytics/messages/received-percentage` | Admin | Porcentagem de mensagens recebidas por usuário. |
| `GET` | `/analytics/users/most-friends` | Admin | Usuários com mais amizades. |
| `GET` | `/analytics/groups/member-count` | Admin | Quantidade de membros por grupo. |
| `GET` | `/analytics/reports/top-reported` | Admin | Usuários com mais de 3 denúncias. |
| `GET` | `/analytics/reports/common-reasons` | Admin | Motivos de denúncia mais comuns. |
| `GET` | `/analytics/users/most-groups` | Admin | Usuários presentes em mais grupos. |
| `GET` | `/analytics/users/most-inactive` | Admin | Usuário há mais tempo sem enviar mensagens. |
| `GET` | `/analytics/users/{userUuid}/message-stats?start={ISO-8601}&end={ISO-8601}` | Admin | Estatísticas de envio e recebimento em um período. |

## Principais tópicos WebSocket

A conexão STOMP deve ser feita em `ws://localhost:8080/ws` e incluir o header `Authorization: Bearer <token>`. O broker do backend publica em:

| Tipo | Destino | Conteúdo |
|---|---|---|
| Assinatura | `/topic/private/{userUuid}` | Mensagem privada enviada ou edição publicada para o destinatário e remetente. |
| Assinatura | `/topic/groups/{groupUuid}` | Mensagem enviada ao grupo ou edição publicada para os membros assinantes. |

Não há destinos `/app` associados a controllers de mensagens; o envio atual é iniciado pelos endpoints REST e publicado pelo backend.

## Contexto e autoria

Projeto desenvolvido por **João Gabriel Farias Machado**, com origem na disciplina de Banco de Dados (Prof. Jurair Rosa) do curso de Engenharia de Computação da UFCI-RJ (antigo CEFET/RJ) — Campus Petrópolis.

## Declaração de Uso de Inteligência Artificial Generativa (IAG)

### 1. Finalidade do uso

A IAG foi utilizada como apoio na estruturação de boilerplate e testes de integração.

### 2. Etapas do trabalho em que a IAG foi utilizada

A IAG participou da implementação incremental dos endpoints, da revisão de testes, dos ajustes de integração com o frontend e desta documentação.

### 3. Grau de intervenção humana

A arquitetura, a modelagem do banco e as regras principais foram definidas pelo autor. Código e diffs foram revisados manualmente, incluindo correções de regras de negócio, permissões e validações. Os testes também receberam validação manual no sistema.

### 4. Forma de validação das informações geradas

As entregas foram validadas pela execução da suíte JUnit/Gradle, pela execução de requisições pelo Postman, pela inspeção direta das tabelas e queries no MySQL via DBeaver e por testes de ponta a ponta na interface.
